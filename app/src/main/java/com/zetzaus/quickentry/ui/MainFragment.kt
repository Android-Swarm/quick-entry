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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.zetzaus.quickentry.R
import com.zetzaus.quickentry.database.DistancedSpot
import com.zetzaus.quickentry.database.createSingleId
import com.zetzaus.quickentry.extensions.TAG
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    private val sharedModel by activityViewModels<MainActivityViewModel>()
    private lateinit var viewModel: MainFragmentViewModel

    private val entrySpotCallback = object : DiffUtil.ItemCallback<DistancedSpot>() {
        /** 2 [DistancedSpot] objects are the same if the `EntrySpot`'s primary key is the same.*/
        override fun areItemsTheSame(oldItem: DistancedSpot, newItem: DistancedSpot): Boolean {
            return oldItem.entrySpot.urlId == newItem.entrySpot.urlId &&
                    oldItem.entrySpot.originalName == newItem.entrySpot.originalName
        }

        override fun areContentsTheSame(oldItem: DistancedSpot, newItem: DistancedSpot): Boolean {
            return oldItem == newItem
        }

        /**
         * Returns [OnlyDistanceChanged] object if the only field that changes is the distance.
         *
         * @param oldItem The old [DistancedSpot].
         * @param newItem The updated [DistancedSpot].
         * @return [OnlyDistanceChanged] if the only field that changes is the distance.
         */
        override fun getChangePayload(oldItem: DistancedSpot, newItem: DistancedSpot): Any? {
            return if (oldItem.entrySpot == newItem.entrySpot && oldItem.distance != newItem.distance) {
                OnlyDistanceChanged
            } else {
                super.getChangePayload(oldItem, newItem)
            }
        }
    }

    /** This fragment's [RecyclerView.Adapter].*/
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

        recyclerView.apply {
            adapter = spotAdapter
            layoutManager = LinearLayoutManager(this@MainFragment.requireContext())
        }

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
            spotAdapter.submitList(spots) {
                if (viewModel.isFirstLocationUpdate) {
                    Log.d(TAG, "First location update, scrolling to the top")
                    recyclerView.scrollToPosition(0)
                }
            }
        }

        sharedModel.lastLocation.observe(viewLifecycleOwner) { currentLocation ->
            Log.d(TAG, "Observed location change, refreshing list...")
            viewModel.updateLocation(currentLocation)
        }
    }

    private inner class EntrySpotAdapter(callback: DiffUtil.ItemCallback<DistancedSpot>) :
        ListAdapter<DistancedSpot, EntrySpotAdapter.EntrySpotViewHolder>(callback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntrySpotViewHolder {
            return LayoutInflater.from(parent.context).inflate(
                R.layout.item_entry_spot,
                parent,
                false
            ).run {
                EntrySpotViewHolder(this)
            }
        }

        override fun onBindViewHolder(holder: EntrySpotViewHolder, position: Int) {
            holder.bind(currentList[position])
        }

        override fun onBindViewHolder(
            holder: EntrySpotViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                if (payloads.any { it is OnlyDistanceChanged }) {
                    holder.updateDistanceTextAndIndicator(currentList[position])
                }
            }
        }

        private inner class EntrySpotViewHolder(private val parent: View) :
            RecyclerView.ViewHolder(parent) {

            fun bind(spot: DistancedSpot) {
                parent.apply {
                    findViewById<ImageView>(R.id.checkedIn).visibility =
                        if (spot.entrySpot.checkedIn) View.VISIBLE else View.INVISIBLE

                    val sharedTextView =
                        findViewById<MaterialTextView>(R.id.textOriginalName).apply {
                            text = spot.entrySpot.originalName
                            transitionName = spot.entrySpot.createSingleId()
                        }

                    findViewById<MaterialTextView>(R.id.textCustomName).text =
                        spot.entrySpot.customName

                    updateDistanceTextAndIndicator(spot)

                    // When clicked, go to the website
                    setOnClickListener {
                        MainFragmentDirections.actionMainFragmentToWebActivity(
                            spot.entrySpot.url,
                            null
                        ).run {
                            view?.findNavController()?.navigate(this)
                        }
                    }

                    // When long-clicked, go to the EntryDetailsFragment
                    setOnLongClickListener {
                        val extras =
                            FragmentNavigatorExtras(sharedTextView to spot.entrySpot.createSingleId())

                        MainFragmentDirections
                            .actionMainFragmentToEntryDetailsFragment(
                                spot.entrySpot.urlId,
                                spot.entrySpot.originalName
                            )
                            .run {
                                view?.findNavController()?.navigate(this, extras)
                                true
                            }
                    }
                }
            }

            fun updateDistanceTextAndIndicator(spot: DistancedSpot) {
                parent.findViewById<MaterialTextView>(R.id.textDistance).text =
                    spot.distance?.let {
                        "%.2f m".format(it)
                    } ?: getString(R.string.no_location_yet)

                parent.findViewById<ImageView>(R.id.imageCardLocation).setImageResource(
                    if (spot.distance == null) R.drawable.ic_baseline_location_off_24
                    else R.drawable.ic_baseline_location_on_24
                )
            }
        }
    }

    /** A simple object that is used as a payload for the recycler view's adapter. This payload
     * indicates that only the distance field is different.
     */
    object OnlyDistanceChanged
}