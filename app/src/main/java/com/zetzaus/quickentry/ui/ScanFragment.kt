package com.zetzaus.quickentry.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.camera.BarcodeAnalyzer
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

    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private val acceptableBarcodeFormats =
        listOf(Barcode.FORMAT_CODE_39, Barcode.FORMAT_QR_CODE)

    private var resetTextJob: Job? = null

    private lateinit var lastLocation: Location

    private val viewModel by activityViewModels<MainActivityViewModel>()

    private lateinit var root: View
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_scan, container, false)

        cameraExecutor = Executors.newSingleThreadExecutor()

        viewModel.lastLocation.value?.let { lastLocation = it } // Try to initialize fast
        viewModel.lastLocation.observe(viewLifecycleOwner) { newLocation ->
            lastLocation = newLocation

            Log.d(TAG, "Received location, updating current location.")
        }

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

    private fun allPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this.requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this.requireContext())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build()

            // What to do when analysis encountered exception
            val onFailure: (Exception) -> Unit = {
                Log.e(
                    TAG,
                    "Encountered exception when analyzing QR code: ${it.localizedMessage}",
                    it
                )
            }

            // What to do when a barcode is recognized
            val onSuccess: (List<Barcode>) -> Unit = { barcodes ->
                val barcodesOfInterest = barcodes.filter { it.format in acceptableBarcodeFormats }
                    .also {
                        it.firstOrNull()?.let { detected ->
                            Log.d(TAG, "Detected valid barcode of format ${detected.format}")
                            updateTextAndProgressBar(detected.displayValue ?: "")
                        }
                    }

                val validBarcode = barcodesOfInterest.firstOrNull {
                    return@firstOrNull when (it.format) {
                        Barcode.FORMAT_CODE_39 -> it.displayValue?.isNRICBarcode() ?: false
                        Barcode.FORMAT_QR_CODE -> it.url?.url?.isSafeEntryCodeURL() ?: false
                        else -> false
                    }
                }

                validBarcode?.let { barcode ->
                    when (barcode.format) {
                        Barcode.FORMAT_QR_CODE -> barcode.url?.url?.let { processQrCode(it) }
                        Barcode.FORMAT_CODE_39 -> barcode.displayValue?.let { processCode39(it) }
                        else -> Unit
                    }
                }
            }

            // Which type of barcodes to recognize
            val barcodeOption = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(acceptableBarcodeFormats[0], acceptableBarcodeFormats[1])
                .build()

            imageAnalysis = ImageAnalysis.Builder().build()
                .apply {
                    setAnalyzer(
                        cameraExecutor,
                        BarcodeAnalyzer(barcodeOption, onSuccess, onFailure)
                    )
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview, imageAnalysis
                )

                val surfaceProvider = viewFinder.createSurfaceProvider()
                preview?.setSurfaceProvider(surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Caught exception when starting camera", e)
            }
        }, ContextCompat.getMainExecutor(this.activity))

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
            ::lastLocation.isInitialized,
            if (!::lastLocation.isInitialized) null else lastLocation
        )

        viewFinder?.findNavController()
            ?.navigateOnce(R.id.scanFragment, action)
    }

    private fun processCode39(text: String) {
        Log.d(TAG, "Found NRIC barcode! First 4: ${text.slice(0..3)}")

        Snackbar.make(root, R.string.snack_bar_found_nric, Snackbar.LENGTH_LONG)
            .setAction(R.string.snack_bar_action_view) {
                Log.d(TAG, "Pressed the View action on Snackbar")
                //TODO: save to shared preference
                //TODO: navigate to barcodeFragment
            }.show()
    }

    companion object {
        const val REQUEST_PERMISSION_CODE = 1024
        val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}