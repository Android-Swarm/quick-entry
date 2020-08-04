package com.zetzaus.quickentry.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.extensions.TAG
import com.zetzaus.quickentry.service.LocationService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var locationService: LocationService
    private lateinit var receiver: LocationReceiver

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Location service has been disconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Location service has been connected")
            locationService = (service as LocationService.LocationBinder).service
            if (allPermissionsGranted()) locationService.startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
        receiver = LocationReceiver()
    }

    override fun onResume() {
        super.onResume()

        bindService(
            Intent(this, LocationService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        registerReceiver(
            receiver,
            IntentFilter(LocationService.ACTION_LOCATION_BROADCAST),
            LocationService.LOCAL_PERMISSION,
            null
        )

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                PERMISSION_CODE
            )
        }
    }

    override fun onPause() {
        super.onPause()
        unbindService(serviceConnection)
        unregisterReceiver(receiver)
    }

    private fun allPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CODE) {
            if (allPermissionsGranted()) {
                locationService.startLocationUpdates()
            } else {
                Snackbar.make(
                    navHostFragment,
                    R.string.snack_bar_location_permission_denied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        const val PERMISSION_CODE = 1000
    }

    private inner class LocationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == LocationService.ACTION_LOCATION_BROADCAST) {
                    Log.d(TAG, "Received location broadcast: updating location")
                    viewModel.updateLocation(LocationService.getLocation(it))
                }
            }
        }
    }
}