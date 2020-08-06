package com.zetzaus.quickentry.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.Barcode
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.camera.QRAnalyzer
import com.zetzaus.quickentry.extensions.TAG
import com.zetzaus.quickentry.extensions.isSafeEntryCodeURL
import kotlinx.android.synthetic.main.fragment_scan.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
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

            val onFailure: (Exception) -> Unit = {
                Log.e(
                    TAG,
                    "Encountered exception when analyzing QR code: ${it.localizedMessage}",
                    it
                )
            }

            val onSuccess: (List<Barcode>) -> Unit = { barcodes ->
                val safeEntryBarcode = barcodes
                    .filter { it.valueType == Barcode.TYPE_URL }
                    .firstOrNull { it.url?.url?.isSafeEntryCodeURL() ?: false }

                safeEntryBarcode?.let { barcode ->
                    barcode.url?.let { urlBookmark ->
                        urlBookmark.url?.let { url ->
                            Log.d(TAG, "Found SafeEntry URL: $url")
                            val action = ScanFragmentDirections.actionScanFragmentToWebActivity(
                                url,
                                ::lastLocation.isInitialized,
                                if (!::lastLocation.isInitialized) null else lastLocation
                            )
                            viewFinder?.findNavController()?.navigate(action)
                        }
                    }
                }
            }

            imageAnalysis = ImageAnalysis.Builder().build()
                .apply {
                    setAnalyzer(cameraExecutor, QRAnalyzer(onSuccess, onFailure))
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

    companion object {
        const val REQUEST_PERMISSION_CODE = 1024
        val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}