package com.zetzaus.quickentry.ui

import android.app.Application
import android.location.Location
import android.util.Log
import android.webkit.WebChromeClient
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zetzaus.quickentry.database.EntrySpot
import com.zetzaus.quickentry.database.middleOf
import com.zetzaus.quickentry.database.toSimpleLocation
import com.zetzaus.quickentry.extensions.getLeafLevel
import kotlinx.coroutines.launch

class WebFragmentViewModel(application: Application) : EntryViewModel(application) {

    /**
     * This variable will save which safe entry URL has been detected. This is as a workaround
     * for when [WebChromeClient.onProgressChanged] is called multiple times.
     */
    val detectedUrls = mutableSetOf<String>()

    private val _progressIndicatorVisibility = MutableLiveData<Boolean>()
    val progressIndicatorVisibility: LiveData<Boolean> = _progressIndicatorVisibility

    fun updateProgressIndicator(newProgress: Int) {
        _progressIndicatorVisibility.value = newProgress != 100
    }

    fun saveSpot(url: String, locationName: String, location: Location) = viewModelScope.launch {
        val targetUrlId = url getLeafLevel 0

        // Try to do upsert first
        val newSpot = repository.getById(targetUrlId, locationName)?.apply {
            Log.d(TAG, "Found an entry for this location, initial location: ${this.location}")
            this.location = this.location middleOf location
            Log.d(TAG, "Location is updated to ${this.location}")
        } ?: EntrySpot(
            urlId = targetUrlId,
            url = url,
            originalName = locationName,
            customName = locationName,
            location = location.toSimpleLocation()
        )

        repository.save(newSpot)
    }

    fun updateCheckIn(url: String, locationName: String, newCheckedIn: Boolean) =
        viewModelScope.launch {
            repository
                .getById(url getLeafLevel 1, locationName)
                ?.apply { checkedIn = newCheckedIn }
                ?.also { repository.save(it) }
        }

    companion object {
        val TAG = WebFragmentViewModel::class.simpleName
    }
}