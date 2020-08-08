package com.zetzaus.quickentry.extensions

import androidx.navigation.NavController
import androidx.navigation.NavDirections

/**
 * Ensures only one call to the [NavController.navigate] function. This means other calls to this
 * function other than the first call will be ignored. This function should be used when there are
 * possibilities of the [NavController.navigate] function to be called multiple times.
 *
 * @param from The source destination id.
 * @param to The target destination direction.
 */
fun NavController.navigateOnce(from: Int, to: NavDirections) {
    if (currentDestination?.id == from) navigate(to)
}