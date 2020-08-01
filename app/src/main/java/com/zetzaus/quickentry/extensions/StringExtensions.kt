package com.zetzaus.quickentry.extensions

infix fun String.matches(pattern: String) = Regex(pattern).matches(this)

fun String.isSafeEntryCodeURL() =
    this matches """^https://temperaturepass\.ndi-api\.gov\.sg/login/.+$"""

fun String.isSafeEntryURL() =
    this matches """^https://www\.safeentry-qr\.gov\.sg/tenant/.+$"""