package com.zetzaus.quickentry.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.zetzaus.quickentry.database.EntrySpot
import com.zetzaus.quickentry.database.EntrySpotRepository
import kotlinx.coroutines.launch

class EntryDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val entryRepository = EntrySpotRepository(application.baseContext)

    private val _currentSpot = MutableLiveData<EntrySpot>()
    val currentSpot = Transformations.switchMap(_currentSpot) { MutableLiveData(it) }

    fun fetchSpotAsync(urlId: String, originalName: String) =
        viewModelScope.launch {
            _currentSpot.postValue(entryRepository.getById(urlId, originalName)!!)
        }

    fun updateSpotData(entrySpot: EntrySpot) =
        viewModelScope.launch { entryRepository.save(entrySpot) }

}