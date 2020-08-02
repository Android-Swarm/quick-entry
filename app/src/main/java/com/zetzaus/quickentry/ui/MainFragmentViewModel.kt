package com.zetzaus.quickentry.ui

import android.location.Location
import androidx.lifecycle.ViewModel

class MainFragmentViewModel : ViewModel() {
    var lastLocation: Location? = null
}