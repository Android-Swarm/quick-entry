package com.zetzaus.quickentry.database

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class NricRepository(private val context: Context) {
    private val sharedPreference
        get() = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun saveBarcode(raw: String) =
        CryptoManager.encrypt(raw).let { (cipherBarcode, initVector) ->
            sharedPreference.edit {
                putString(BARCODE_KEY, cipherBarcode)
                putString(IV_KEY, initVector)
            }
        }

    fun getBarcode(): String {
        val encryptedBarcode = sharedPreference.getString(BARCODE_KEY, "")!!
        val initVector = sharedPreference.getString(IV_KEY, "")!!

//        Log.d("NricRepository", encryptedBarcode)
//        Log.d("NricRepository", initVector)

        return CryptoManager.decrypt(
            encryptedBarcode,
            initVector
        )
    }

    companion object {
        const val BARCODE_KEY = "BARCODE"
        const val IV_KEY = "INIT_VECTOR"
    }
}