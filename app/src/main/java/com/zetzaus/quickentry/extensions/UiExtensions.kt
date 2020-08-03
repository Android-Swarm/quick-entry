package com.zetzaus.quickentry.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

private fun getSimpleName(thing: Any) = thing::class.simpleName

val AppCompatActivity.TAG
    get() = getSimpleName(this)

val Fragment.TAG
    get() = getSimpleName(this)