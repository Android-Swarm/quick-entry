package com.zetzaus.quickentry.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.service.LocationService
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    private lateinit var viewModel: MainFragmentViewModel
    private lateinit var receiver: LocationReceiver

    private var locationService: LocationService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Location service has been disconnected")
            locationService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Location service has been connected")
            locationService = (service as LocationService.LocationBinder).service
            if (allPermissionsGranted()) locationService?.startLocationUpdates()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MainFragmentViewModel::class.java]
        receiver = LocationReceiver()

        scanFloatingActionButton.setOnClickListener {
            it.findNavController().navigate(R.id.action_mainFragment_to_scanFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.let {
            it.bindService(
                Intent(requireContext(), LocationService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            it.registerReceiver(
                receiver,
                IntentFilter(LocationService.ACTION_LOCATION_BROADCAST),
                LocationService.LOCAL_PERMISSION,
                null
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (!allPermissionsGranted()) {
            requestPermissions(PERMISSIONS, PERMISSION_CODE)
        }
    }

    override fun onStop() {
        super.onStop()

        activity?.let {
            it.unbindService(serviceConnection)
            it.unregisterReceiver(receiver)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CODE) {
            if (allPermissionsGranted()) {
                locationService?.startLocationUpdates()
            } else {
                Snackbar.make(
                    scanFloatingActionButton,
                    R.string.snack_bar_location_permission_denied,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun allPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this.requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val TAG = MainFragment::class.simpleName
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
                    viewModel.lastLocation = LocationService.getLocation(it)
                }
            }
        }
    }
}