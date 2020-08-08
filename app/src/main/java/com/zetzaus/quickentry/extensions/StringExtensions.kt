package com.zetzaus.quickentry.extensions

/**
 * Returns `true` if the string matches the given [Regex] pattern. The matching will use the
 * default [Regex] object configuration.
 *
 * @param pattern The regular expression pattern to match.
 */
infix fun String.matches(pattern: String) = Regex(pattern).matches(this)

/** Returns `true` if the [String] is a raw SafeEntry URL scanned from a QR code. */
fun String.isSafeEntryCodeURL() =
    this matches """^https://temperaturepass\.ndi-api\.gov\.sg/login/.+$"""

/** Returns `true` if the [String] is the actual SafeEntry URL. Please note that the URL from the
 *  QR code is different than the actual URL that user will arrive at. For raw QR code URL,
 *  use [String.isSafeEntryCodeURL].
 *
 */
fun String.isSafeEntryURL() =
    this matches """^https://www\.safeentry-qr\.gov\.sg/tenant/.+$"""

/** Returns `true` if the [String] is a SafeEntry checked-in URL or a SafeEntry checked-out URL. */
fun String.isSafeEntryCompletionURL() =
    this matches """^https://www\.safeentry-qr\.gov\.sg/complete/.+/.+$"""

/** Returns `true` if the [String] is a valid NRIC barcode value. */
fun String.isNRICBarcode() =
    this matches """^[STFG]\d{7}[A-Z]\d{6}[A-Z]*$"""

/**
 * Returns the leaf path of the given level from the [String] URL.
 *
 * @param level The leaf path level. 0 means the leaf path, 1 means the parent of the leaf path, etc.
 */
infix fun String.getLeafLevel(level: Int) =
    this.split("/").run { get(lastIndex - level) }