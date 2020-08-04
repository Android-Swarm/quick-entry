package com.zetzaus.quickentry.ui

import android.app.Application
import android.location.Location
import android.webkit.WebChromeClient
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.zetzaus.quickentry.database.EntrySpot
import com.zetzaus.quickentry.database.SimpleLocation
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
        repository.save(
            EntrySpot(
                urlId = url getLeafLevel 0,
                url = url,
                originalName = locationName,
                customName = locationName,
                location = SimpleLocation(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        )
    }

    fun updateCheckIn(url: String, newCheckedIn: Boolean) = viewModelScope.launch {
        repository
            .getById(url getLeafLevel 1)
            .apply { checkedIn = newCheckedIn }
            .also {
                repository.save(it)
            }
    }


}