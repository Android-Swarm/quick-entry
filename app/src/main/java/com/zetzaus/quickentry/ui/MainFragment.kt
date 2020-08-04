package com.zetzaus.quickentry.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.zetzaus.quickentry.database.EntrySpot
import com.zetzaus.quickentry.database.SimpleLocation
import com.zetzaus.quickentry.extensions.TAG
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    private val sharedModel by activityViewModels<MainActivityViewModel>()
    private lateinit var viewModel: MainFragmentViewModel

    private val entrySpotCallback = object : DiffUtil.ItemCallback<EntrySpot>() {
        override fun areItemsTheSame(oldItem: EntrySpot, newItem: EntrySpot): Boolean =
            oldItem.urlId == newItem.urlId

        override fun areContentsTheSame(oldItem: EntrySpot, newItem: EntrySpot): Boolean =
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

        viewModel.liveEntrySpots.observe(viewLifecycleOwner) {
            spotAdapter.submitList(it)
        }

        sharedModel.lastLocation.observe(viewLifecycleOwner) {
            // TODO: sort list based on the new location
            Log.d(
                TAG,
                "Observed location change: ${it.latitude} ${it.longitude}, refreshing list..."
            )
        }

        recyclerView.apply {
            adapter = spotAdapter
            layoutManager = LinearLayoutManager(this@MainFragment.requireContext())
        }

//        spotAdapter.submitList(
//            listOf(
//                EntrySpot(
//                    "1",
//                    "CAN 11",
//                    "CAN 11",
//                    "https://www.google.com",
//                    SimpleLocation(1.0, 1.0),
//                    false
//                ),
//                EntrySpot(
//                    "1",
//                    "CAN 12",
//                    "CAN 12",
//                    "https://www.google.com",
//                    SimpleLocation(1.0, 1.0),
//                    true
//                )
//            )
//        )
    }

    inner class EntrySpotAdapter(callback: DiffUtil.ItemCallback<EntrySpot>) :
        ListAdapter<EntrySpot, EntrySpotAdapter.EntrySpotViewHolder>(callback) {

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

            fun bind(spot: EntrySpot) {
                parent.apply {
                    if (spot.checkedIn)
                        findViewById<TextView>(R.id.checkedIn).setBackgroundColor(
                            ContextCompat.getColor(
                                this@MainFragment.requireContext(),
                                android.R.color.holo_green_light
                            )
                        )

                    findViewById<MaterialTextView>(R.id.textCustomName).text = spot.customName
                    findViewById<MaterialTextView>(R.id.textDistance).text = "Waiting for GPS..."
                    // TODO: do calculation logic
                }
            }
        }
    }
}