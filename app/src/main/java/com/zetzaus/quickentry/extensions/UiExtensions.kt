package com.zetzaus.quickentry.extensions

import android.app.Service
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

private fun getSimpleName(thing: Any) = thing::class.simpleName

val AppCompatActivity.TAG
    get() = getSimpleName(this)

val Fragment.TAG
    get() = getSimpleName(this)

/**
 * Returns `true` if the device is connected to the internet.
 *
 * @return `true` if the device is connected to the internet.
 */
fun Fragment.isNetworkConnected(): Boolean {
    (activity?.getSystemService(Service.CONNECTIVITY_SERVICE) as ConnectivityManager?)?.let { manager ->
        val activeNetwork = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    return false
}