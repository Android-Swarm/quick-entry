package com.zetzaus.quickentry.ui

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    private val _lastLocation = MutableLiveData<Location>()
    val lastLocation = Transformations.switchMap(_lastLocation) {
        MutableLiveData(it)
    }

    fun updateLocation(location: Location) {
        _lastLocation.postValue(location)
    }
}