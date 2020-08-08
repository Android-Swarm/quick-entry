package com.zetzaus.quickentry.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.database.NricRepository
import com.zetzaus.quickentry.extensions.TAG
import kotlinx.android.synthetic.main.fragment_barcode.*

class BarcodeFragment : Fragment() {

    private var barcodeRaw: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.run {
            // Load saved barcode if any
            try {
                barcodeRaw = NricRepository(requireContext()).getBarcode()
                Log.d(
                    TAG, "Found saved barcode. First 4: ${barcodeRaw?.slice(0..3)}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error when fetching barcode: ", e)
            }

            inflate(R.layout.fragment_barcode, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val observer = barcodeImage.viewTreeObserver
        val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                barcodeImage
                    .setImageBitmap(getBarcodeBitmap(barcodeImage.width, barcodeImage.height))

                // Remove when done
                observer.removeOnGlobalLayoutListener(this)
            }
        }

        observer.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun getBarcodeBitmap(width: Int, height: Int) =
        barcodeRaw?.let {
            val bit = MultiFormatWriter().encode(it, BarcodeFormat.CODE_39, width, height)
            BarcodeEncoder().createBitmap(bit)
        }
}