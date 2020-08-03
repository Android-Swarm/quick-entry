package com.zetzaus.quickentry.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.zetzaus.quickentry.BuildConfig
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    private val binder = LocationBinder()

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        locationClient = FusedLocationProviderClient(this)

        locationRequest = LocationRequest().apply {
            fastestInterval = TimeUnit.MILLISECONDS.toMillis(500)
            interval = TimeUnit.SECONDS.toMillis(1)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                super.onLocationResult(result)

                result ?: run {
                    Log.d(TAG, "Received location callback, but location is null")
                    return
                }

                val currentLocation = result.lastLocation

                Intent(ACTION_LOCATION_BROADCAST).apply {
                    putExtra(EXTRA_LOCATION, currentLocation)
                }.run {
                    Log.d(
                        TAG,
                        "Received location: " +
                                "${currentLocation.longitude}, ${currentLocation.latitude}, " +
                                "sending broadcast..."
                    )
                    sendBroadcast(this, LOCAL_PERMISSION)
                }
            }
        }
    }

    fun startLocationUpdates() {
        Log.d(TAG, "Starting location updates...")
        try {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to request location, permission is denied!")
        }
    }

//    fun stopLocationUpdates() {
//        Log.d(TAG, "Stopping location updates...")
//        locationClient.removeLocationUpdates(locationCallback)
//    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocationBinder : Binder() {
        val service: LocationService
            get() = this@LocationService
    }

    companion object {
        val TAG = LocationService::class.simpleName
        private const val packageName = BuildConfig.APPLICATION_ID

        const val ACTION_LOCATION_BROADCAST = "$packageName.ACTION_LOCATION_BROADCAST"
        const val EXTRA_LOCATION = "${packageName}_EXTRA_LOCATION"

        const val LOCAL_PERMISSION = "com.zetzaus.quickentry.PRIVATE"

        fun getLocation(intent: Intent): Location =
            intent.getParcelableExtra(EXTRA_LOCATION)!!
    }
}
