package com.zetzaus.quickentry.camera

import androidx.annotation.experimental.UseExperimental
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val options: BarcodeScannerOptions,
    private val onSuccess: (List<Barcode>) -> Unit,
    private val onFailure: (Exception) -> Unit
) : ImageAnalysis.Analyzer {

    @UseExperimental(markerClass = ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        onSuccess(it.result!!)
                    } else {
                        onFailure(it.exception!!)
                    }
                    imageProxy.close()
                }
        }
    }
}