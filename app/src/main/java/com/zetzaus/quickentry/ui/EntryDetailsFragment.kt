package com.zetzaus.quickentry.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.database.EntrySpot
import kotlinx.android.synthetic.main.fragment_entry_details.*


class EntryDetailsFragment : Fragment() {

    private lateinit var viewModel: EntryDetailsViewModel

    private lateinit var currentSpot: EntrySpot

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
            editTextCustomName.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = Unit

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    s?.let { currentSpot.customName = it.toString() }
                }

            })

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
        }
    }

    override fun onStop() {
        super.onStop()

        // Persist changes
        if (::currentSpot.isInitialized) {
            currentSpot.let { viewModel.updateSpotData(it) }
        }
    }
}