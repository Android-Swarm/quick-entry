package com.zetzaus.quickentry.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

class WebFragmentViewModel : ViewModel() {
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
}