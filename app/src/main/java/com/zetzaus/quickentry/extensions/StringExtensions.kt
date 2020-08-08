package com.zetzaus.quickentry.extensions

infix fun String.matches(pattern: String) = Regex(pattern).matches(this)

fun String.isSafeEntryCodeURL() =
    this matches """^https://temperaturepass\.ndi-api\.gov\.sg/login/.+$"""

fun String.isSafeEntryURL() =
    this matches """^https://www\.safeentry-qr\.gov\.sg/tenant/.+$"""

fun String.isSafeEntryCompletionURL() =
    this matches """^https://www\.safeentry-qr\.gov\.sg/complete/.+/.+$"""

fun String.isNRICBarcode() =
    this matches """^[STFG]\d{7}[A-Z]\d{6}[A-Z]*$"""

infix fun String.getLeafLevel(level: Int) =
    this.split("/").run { get(lastIndex - level) }