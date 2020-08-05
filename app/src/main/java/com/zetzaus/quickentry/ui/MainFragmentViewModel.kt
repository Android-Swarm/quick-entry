package com.zetzaus.quickentry.ui

import android.app.Application
import androidx.lifecycle.asLiveData
import com.zetzaus.quickentry.database.DistancedSpot
import com.zetzaus.quickentry.database.EntrySpot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MainFragmentViewModel(application: Application) : EntryViewModel(application) {
    val liveEntrySpots
        get() = repository
            .entrySpotsFlow
            .toDistancedSpot()
            .asLiveData()

    /** This extension function wraps the [EntrySpot] in a [DistancedSpot] with null distance. */
    private fun Flow<List<EntrySpot>>.toDistancedSpot() =
        this.map { entryList -> entryList.map { DistancedSpot(it, null) } }
}