package com.zetzaus.quickentry.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.database.DistancedSpot
import com.zetzaus.quickentry.extensions.TAG
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    private val sharedModel by activityViewModels<MainActivityViewModel>()
    private lateinit var viewModel: MainFragmentViewModel

    private val entrySpotCallback = object : DiffUtil.ItemCallback<DistancedSpot>() {
        override fun areItemsTheSame(oldItem: DistancedSpot, newItem: DistancedSpot): Boolean =
            oldItem.entrySpot.urlId == newItem.entrySpot.urlId

        override fun areContentsTheSame(oldItem: DistancedSpot, newItem: DistancedSpot): Boolean =
            oldItem == newItem
    }

    private val spotAdapter = EntrySpotAdapter(entrySpotCallback)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MainFragmentViewModel::class.java]

        scanFloatingActionButton.setOnClickListener {
            it.findNavController().navigate(R.id.action_mainFragment_to_scanFragment)
        }

        editText.addTextChangedListener(object : TextWatcher {
            // Do nothing
            override fun afterTextChanged(s: Editable?) = Unit

            // Do nothing
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { viewModel.updateQuery(s.toString()) }
            }
        })

        viewModel.liveDistancedSpots.observe(viewLifecycleOwner) { spots ->
            spotAdapter.submitList(spots.sortedBy { it.distance ?: Float.MAX_VALUE })
        }

        sharedModel.lastLocation.observe(viewLifecycleOwner) { currentLocation ->
            Log.d(
                TAG,
                "Observed location change: " +
                        "${currentLocation.latitude} ${currentLocation.longitude}" +
                        ", refreshing list..."
            )
            viewModel.updateLocation(currentLocation)
        }

        recyclerView.apply {
            adapter = spotAdapter
            layoutManager = LinearLayoutManager(this@MainFragment.requireContext())
        }
    }

    inner class EntrySpotAdapter(callback: DiffUtil.ItemCallback<DistancedSpot>) :
        ListAdapter<DistancedSpot, EntrySpotAdapter.EntrySpotViewHolder>(callback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntrySpotViewHolder {
            return LayoutInflater.from(parent.context).inflate(
                R.layout.entry_item_name,
                parent,
                false
            ).run {
                EntrySpotViewHolder(this)
            }
        }

        override fun onBindViewHolder(holder: EntrySpotViewHolder, position: Int) {
            holder.bind(currentList[position])
        }

        inner class EntrySpotViewHolder(private val parent: View) :
            RecyclerView.ViewHolder(parent) {

            fun bind(spot: DistancedSpot) {
                parent.apply {
                    findViewById<ImageView>(R.id.checkedIn).visibility =
                        if (spot.entrySpot.checkedIn) View.VISIBLE else View.INVISIBLE

                    findViewById<MaterialTextView>(R.id.textOriginalName).text =
                        spot.entrySpot.originalName

                    findViewById<MaterialTextView>(R.id.textCustomName).text =
                        spot.entrySpot.customName
                    findViewById<MaterialTextView>(R.id.textDistance).text =
                        spot.distance?.let {
                            "%.2f m".format(it)
                        } ?: getString(R.string.no_location_yet)

                    this.setOnClickListener {
                        MainFragmentDirections.actionMainFragmentToWebActivity(
                            spot.entrySpot.url,
                            null
                        ).run {
                            view?.findNavController()?.navigate(this)
                        }
                    }
                }
            }
        }
    }
}