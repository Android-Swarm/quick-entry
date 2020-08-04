package com.zetzaus.quickentry.ui

import android.app.Application
import androidx.lifecycle.asLiveData

class MainFragmentViewModel(application: Application) : EntryViewModel(application) {
    val liveEntrySpots
        get() = repository.entrySpotsFlow.asLiveData()
}