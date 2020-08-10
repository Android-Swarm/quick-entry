package com.zetzaus.quickentry.ui

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.database.EntrySpot
import com.zetzaus.quickentry.database.createSingleId
import kotlinx.android.synthetic.main.fragment_entry_details.*


class EntryDetailsFragment : Fragment() {

    private lateinit var viewModel: EntryDetailsViewModel

    private lateinit var currentSpot: EntrySpot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =
            TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entry_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[EntryDetailsViewModel::class.java]

        EntryDetailsFragmentArgs.fromBundle(requireArguments()).run {
            viewModel.fetchSpotAsync(urlId, oiginalName)
        }

        viewModel.currentSpot.observe(viewLifecycleOwner) { new ->
            new?.let { currentSpot = it }
            setupUi()
        }
    }

    private fun setupUi() {
        currentSpot.run {
            currentSpot = this

            editTextCustomName.setText(customName)

            textOriginalName.transitionName = createSingleId()
            textOriginalName.text = originalName

            buttonCheck.setText(
                if (checkedIn)
                    R.string.button_check_out
                else
                    R.string.button_check_in
            )

            buttonCheck.setOnClickListener {
                EntryDetailsFragmentDirections
                    .actionEntryDetailsFragmentToWebActivity(url, null)
                    .run {
                        it.findNavController().navigate(this)
                    }
            }

            imageCheckInStatus.colorFilter = if (!checkedIn) {
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray).run {
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        this,
                        BlendModeCompat.SRC_IN
                    )
                }
            } else {
                null
            }

            setupLocationMap(this)
        }
    }

    private fun setupLocationMap(spot: EntrySpot) {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment

        lifecycleScope.launchWhenCreated {
            val googleMap = mapFragment.awaitMap()

            val spotPosition = LatLng(spot.location.latitude, spot.location.longitude)

            val mapBounds = LatLngBounds.Builder()
                .include(spotPosition)
                .build()

            val mapMargin = resources.getDimensionPixelSize(R.dimen.margin_inset_map)

            CameraUpdateFactory.newLatLngBounds(mapBounds, mapMargin).run {
                googleMap.moveCamera(this)
            }

            googleMap.addMarker {
                position(spotPosition)
                title(spot.originalName)
            }
        }

    }

    override fun onStop() {
        super.onStop()

        // Persist changes
        if (::currentSpot.isInitialized) {
            currentSpot.let {
                viewModel.updateSpotData(it.apply {
                    customName = editTextCustomName.text.toString()
                })
            }
        }
    }
}