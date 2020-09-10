package com.zetzaus.quickentry.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.Image
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.camera.scanBarcode
import com.zetzaus.quickentry.database.NricRepository
import com.zetzaus.quickentry.extensions.TAG
import com.zetzaus.quickentry.extensions.isNRICBarcode
import com.zetzaus.quickentry.extensions.isSafeEntryCodeURL
import com.zetzaus.quickentry.extensions.navigateOnce
import kotlinx.android.synthetic.main.fragment_scan.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    private val acceptableBarcodeFormats =
        listOf(Barcode.FORMAT_CODE_39, Barcode.FORMAT_QR_CODE)

    // Which type of barcodes to recognize
    private val barcodeOption = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(acceptableBarcodeFormats[0], acceptableBarcodeFormats[1])
        .build()

    private var resetTextJob: Job? = null

    private val viewModel by activityViewModels<MainActivityViewModel>()

    private lateinit var root: View
    private lateinit var cameraExecutor: ExecutorService

    // What to do when analysis encountered exception
    private val onFailure: (Exception) -> Unit = {
        Log.e(
            TAG,
            "Encountered exception when analyzing QR code: ${it.localizedMessage}",
            it
        )
    }

    // What to do when a barcode is recognized
    private val onSuccess: (List<Barcode>) -> Unit = { barcodes ->
        // Get the first only code 39 or QR code
        val barcodesOfInterest = barcodes.filter { it.format in acceptableBarcodeFormats }
            .also {
                it.firstOrNull()?.let { detected ->
                    Log.d(TAG, "Detected valid barcode of format ${detected.format}")
                    updateTextAndProgressBar(detected.displayValue ?: "")
                }
            }

        // Get the first only NRIC barcode and SafeEntry URL
        val validBarcode = barcodesOfInterest.firstOrNull {
            return@firstOrNull when (it.format) {
                Barcode.FORMAT_CODE_39 -> it.displayValue?.isNRICBarcode() ?: false
                Barcode.FORMAT_QR_CODE -> it.url?.url?.isSafeEntryCodeURL() ?: false
                else -> false
            }
        }

        validBarcode?.let { barcode ->
            when (barcode.format) {
                Barcode.FORMAT_QR_CODE -> {
                    // Remove all processors if it already sure to navigate.
                    // !! DO NOT REMOVE !!
                    // This is required to stop processing the next frames,
                    // otherwise segmentation fault may happen.
                    cameraView.clearFrameProcessors()
                    Log.d(TAG, "Going to navigate, removed processors")

                    barcode.url?.url?.let { processQrCode(it) }
                }
                Barcode.FORMAT_CODE_39 -> barcode.displayValue?.let { processCode39(it) }
                else -> Unit
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_scan, container, false)

        cameraExecutor = Executors.newSingleThreadExecutor()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!allPermissionsGranted()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSION_CODE)
        } else {
            startCamera()
        }

        textDetected.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    scanningProgressBar.isVisible = s.isBlank()
                }
            }
        })

        viewModel.lastLocation.observe(viewLifecycleOwner) { newLocation ->
            if (newLocation != null) {
                imageLocationIndicator.setImageResource(R.drawable.ic_baseline_location_on_24)
            } else {
                imageLocationIndicator.setImageResource(R.drawable.ic_baseline_location_off_24)
            }
            Log.d(TAG, "Received location, updating current location.")
        }
    }

    override fun onResume() {
        super.onResume()

        cameraView.addFrameProcessor {
            // This part is run in a background thread according to documentation
            if (it.dataClass == Image::class.java) {
                val image = InputImage.fromMediaImage(it.getData() as Image, it.rotationToUser)
                Log.d(TAG, "Converted frame into InputImage")

                try {
                    val result = Tasks.await(scanBarcode(image, barcodeOption))
                    Log.d(TAG, "Processing result received, passing to onSuccess()")
                    onSuccess(result)
                } catch (e: Exception) {
                    onFailure(e)
                }
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (!allPermissionsGranted()) {
                Snackbar.make(
                    root,
                    R.string.snack_bar_camera_permission_denied,
                    Snackbar.LENGTH_INDEFINITE
                ).also { snackBar ->
                    snackBar.setAction(android.R.string.ok) { snackBar.dismiss() }
                }.show()

                Log.d(TAG, "Permission not granted")
            } else {
                Log.d(TAG, "Permission granted by user, starting camera...")
                startCamera()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        cameraView.clearFrameProcessors()
    }

    private fun allPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this.requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        cameraView.setLifecycleOwner(viewLifecycleOwner)
    }

    private fun updateTextAndProgressBar(text: String) {
        resetTextJob?.cancel()
        resetTextJob = lifecycleScope.launch {
            textDetected?.text =
                if (text.isNRICBarcode()) getString(R.string.snack_bar_nric_mask) else text
            textScanStatus?.text = getString(R.string.label_scanned)

            textDescription?.text = getString(
                when {
                    text.isNRICBarcode() -> R.string.label_scanned_nric
                    text.isSafeEntryCodeURL() -> R.string.label_scanned_safe_entry
                    else -> R.string.label_scanned_invalid
                }
            )

            // Reset after 3 seconds
            delay(5000)

            textDetected?.text = ""
            textDescription?.text = ""
            textScanStatus?.text = getString(R.string.label_scanning)
        }
    }

    private fun processQrCode(url: String) {
        Log.d(TAG, "Found SafeEntry URL: $url")

        val action = ScanFragmentDirections.actionScanFragmentToWebActivity(
            url,
            viewModel.lastLocation.value != null,
            viewModel.lastLocation.value
        )

        fragmentNavController?.navigateOnce(R.id.scanFragment, action)

    }

    private fun processCode39(text: String) {
        Log.d(TAG, "Found NRIC barcode! First 4: ${text.slice(0..3)}")

        //Save to shared preference
        NricRepository(requireContext()).saveBarcode(text)

        Snackbar.make(root, R.string.snack_bar_found_nric, Snackbar.LENGTH_LONG)
            .setAction(R.string.snack_bar_action_view) {
                Log.d(TAG, "Pressed the View action on Snackbar")
                fragmentNavController?.navigateOnce(
                    R.id.scanFragment,
                    R.id.action_scanFragment_to_barcodeFragment
                )
            }.show()
    }

    private val fragmentNavController
        get() = activity?.findNavController(R.id.navHostFragment)

    companion object {
        const val REQUEST_PERMISSION_CODE = 1024
        val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}