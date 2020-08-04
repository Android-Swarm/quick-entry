package com.zetzaus.quickentry.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zetzaus.quickentry.database.EntrySpotRepository

open class EntryViewModel(application: Application) : AndroidViewModel(application) {
    protected val repository = EntrySpotRepository(application.baseContext)
}