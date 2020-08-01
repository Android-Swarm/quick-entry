package com.zetzaus.quickentry.extensions

infix fun String.matches(pattern: String) = Regex(pattern).matches(this)

fun String.isSafeEntryURL() =
    this matches """^https://temperaturepass\.ndi-api\.gov\.sg/login/.+$"""