package com.zetzaus.quickentry.ui

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.*
import com.zetzaus.quickentry.database.DistancedSpot
import com.zetzaus.quickentry.database.EntrySpot
import com.zetzaus.quickentry.database.toLocation

class MainFragmentViewModel(application: Application) : EntryViewModel(application) {
    private val liveQuery = MutableLiveData<String>("")
    private val liveLocation = MutableLiveData<Location?>(null)

    private val liveEntrySpots
        get() = Transformations.switchMap(liveQuery) {
            if (it == "") {
                Log.d(TAG, "No query string, retrieve all items")
                repository.entrySpotsFlow
            } else {
                Log.d(TAG, "Query string is \"$it\", filtering items")
                repository.getSpotsContaining(it)
            }.asLiveData()
        }

    val liveDistancedSpots
        get() = Transformations.switchMap(liveLocation) { liveEntrySpots.toDistancedSpot(it) }

    /**
     * This extension function wraps the [EntrySpot] in a [DistancedSpot] with the appropriate distance.
     * The distance should be submitted by calling [updateLocation].
     *
     */
    private fun LiveData<List<EntrySpot>>.toDistancedSpot(location: Location?) =
        this.map { entryList ->
            entryList.map {
                DistancedSpot(
                    it,
                    location?.distanceTo(it.location.toLocation())
                )
            }
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