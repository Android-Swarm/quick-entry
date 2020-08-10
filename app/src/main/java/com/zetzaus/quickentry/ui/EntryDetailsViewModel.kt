package com.zetzaus.quickentry.ui

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.zetzaus.quickentry.database.EntrySpot
import kotlinx.coroutines.launch

class EntryDetailsViewModel(application: Application) : EntryViewModel(application) {

    private val _currentSpot = MutableLiveData<EntrySpot>()
    val currentSpot = Transformations.switchMap(_currentSpot) { MutableLiveData(it) }

    fun fetchSpotAsync(urlId: String, originalName: String) =
        viewModelScope.launch {
            _currentSpot.postValue(repository.getById(urlId, originalName)!!)
        }

    fun updateSpotData(entrySpot: EntrySpot) =
        viewModelScope.launch { repository.save(entrySpot) }

}