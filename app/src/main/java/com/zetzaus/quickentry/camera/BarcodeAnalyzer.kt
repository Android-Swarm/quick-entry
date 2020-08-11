package com.zetzaus.quickentry.camera

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

//@Deprecated(message = "CameraX has been replaced with third party library")
//class BarcodeAnalyzer(
//    private val options: BarcodeScannerOptions,
//    private val onSuccess: (List<Barcode>) -> Unit,
//    private val onFailure: (Exception) -> Unit
//) : ImageAnalysis.Analyzer {
//
//    @UseExperimental(markerClass = ExperimentalGetImage::class)
//    override fun analyze(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image
//
//        if (mediaImage != null) {
//            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//            scanBarcode(image, options) {
//                if (it.isSuccessful) {
//                    onSuccess(it.result!!)
//                } else {
//                    onFailure(it.exception!!)
//                }
//                imageProxy.close()
//            }
//        }
//    }
//}

fun scanBarcode(
    input: InputImage,
    options: BarcodeScannerOptions,
    onComplete: (Task<List<Barcode>>) -> Unit
) = BarcodeScanning.getClient(options)
    .process(input)
    .addOnCompleteListener(onComplete)

suspend fun scanBarcodeSynchronous(
    input: InputImage,
    options: BarcodeScannerOptions
): List<Barcode> = scanBarcode(input, options) {}.await()