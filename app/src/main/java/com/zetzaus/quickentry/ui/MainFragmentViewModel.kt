package com.zetzaus.quickentry.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.*
import com.zetzaus.quickentry.database.DistancedSpot
import com.zetzaus.quickentry.database.EntrySpot
import com.zetzaus.quickentry.database.FirebaseHandler
import com.zetzaus.quickentry.database.toLocation
import kotlinx.coroutines.launch

class MainFragmentViewModel(application: Application) : EntryViewModel(application) {
    private val liveQuery = MutableLiveData<String>("")
    private val liveLocation = MutableLiveData<Location?>(null)
    private val firebase = FirebaseHandler.getInstance(application.baseContext)

    init {
        // Fetch data from Firebase
        viewModelScope.launch { firebase.readAll(viewModelScope) }
    }

    private val liveEntrySpots = Transformations.switchMap(liveQuery) {
        if (it == "") {
            repository.entrySpotsFlow
        } else {
            repository.getSpotsContaining(it)
        }.asLiveData()
    }

    val liveDistancedSpots =
        Transformations.switchMap(liveLocation) { liveEntrySpots.toSortedDistancedSpot(it) }

    /**
     * This extension function wraps the [EntrySpot] in a [DistancedSpot] with the appropriate
     * distance. The distance should be submitted by calling [updateLocation].
     *
     * The entries will be sorted ascending to the distance, and checked-in
     * location will be placed on the beginning.
     *
     */
    private fun LiveData<List<EntrySpot>>.toSortedDistancedSpot(location: Location?) =
        this.map { entryList ->
            entryList.map {
                DistancedSpot(
                    it,
                    location?.distanceTo(it.location.toLocation())?.roundTo(2)
                )
            }.sortedBy { it.distance ?: Float.MAX_VALUE }  // Sort by distance first
                .sortedByDescending { it.entrySpot.checkedIn }// Sort by checked in afterwards
        }

    private infix fun Float.roundTo(decimalPlace: Int): Float {
        var multiplier = 1.0f
        repeat(decimalPlace) { multiplier *= 10 }
        return Math.round(this * multiplier) / multiplier
    }

    /**
     * Updates the query string for the list items.
     *
     * @param query The new query string to be used as a filter to the custom name of [EntrySpot].
     */
    fun updateQuery(query: String) = liveQuery.postValue(query)

    /**
     * Updates the current location.
     *
     * @param location The new location to be set as the current location.
     */
    fun updateLocation(location: Location) = liveLocation.postValue(location)

    companion object {
        val TAG = MainFragmentViewModel::class.simpleName
    }
}