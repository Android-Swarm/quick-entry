package com.zetzaus.quickentry.ui

import android.app.Application
import android.location.Location
import android.util.Log
import android.webkit.WebChromeClient
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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
    val detectedUrls = mutableListOf<String>()

    private val _progressIndicatorVisibility = MutableLiveData<Boolean>()
    val progressIndicatorVisibility = Transformations.switchMap(_progressIndicatorVisibility) {
        MutableLiveData(it)
    }

    fun updateProgressIndicator(newProgress: Int) {
        if (newProgress == 100) {
            _progressIndicatorVisibility.postValue(false)
        } else {
            _progressIndicatorVisibility.postValue(true)
        }
    }

    fun saveSpot(url: String, locationName: String, location: Location) = viewModelScope.launch {
        val targetUrlId = url getLeafLevel 0

        // Try to do upsert first
        repository.getByIdOrNull(targetUrlId)?.apply {
            Log.d(TAG, "Found an entry for this location, initial location: ${this.location}")
            this.location = this.location middleOf location
            Log.d(TAG, "Location is updated to ${this.location}")
        } ?: EntrySpot(
            urlId = targetUrlId,
            url = url,
            originalName = locationName,
            customName = locationName,
            location = location.toSimpleLocation()
        ).also { repository.save(it) }
    }

    fun updateCheckIn(url: String, newCheckedIn: Boolean) = viewModelScope.launch {
        repository
            .getById(url getLeafLevel 1)
            .apply { checkedIn = newCheckedIn }
            .also { repository.save(it) }
    }

    companion object {
        val TAG = WebFragmentViewModel::class.simpleName
    }
}