package com.zetzaus.quickentry.database

import android.location.Location
import androidx.room.Embedded
import androidx.room.Entity
import com.google.firebase.database.Exclude

class SimpleLocation(var latitude: Double = 0.0, var longitude: Double = 0.0) {
    override fun toString(): String {
        return "$latitude, $longitude"
    }
}

@Entity(tableName = "entry_spot", primaryKeys = ["urlId", "originalName"])
data class EntrySpot(
    val urlId: String = "",
    val originalName: String = "",
    @get:Exclude var customName: String = "",
    val url: String = "",
    @Embedded var location: SimpleLocation = SimpleLocation(),
    @get:Exclude var checkedIn: Boolean = false
)

/**
 * This class wraps an [EntrySpot] object with the distance from a given location in meters.
 *
 * @property entrySpot Which entry spot this belongs to.
 * @property distance The distance from current location to the entry spot.
 */
data class DistancedSpot(
    val entrySpot: EntrySpot,
    val distance: Float?
)

/** Converts a [SimpleLocation] to [Location]. */
fun SimpleLocation.toLocation() = Location("").apply {
    latitude = this@toLocation.latitude
    longitude = this@toLocation.longitude
}

/** Converts a [Location] to [SimpleLocation]. */
fun Location.toSimpleLocation() = SimpleLocation(this.latitude, this.longitude)

/**
 * Returns the middle location between 2 [SimpleLocation] objects.
 *
 * @param other The other location.
 */
infix fun SimpleLocation.middleOf(other: Location) =
    other.toSimpleLocation().let { otherSimpleLocation ->
        SimpleLocation(
            (otherSimpleLocation.latitude + this.latitude) / 2,
            (otherSimpleLocation.longitude + this.longitude) / 2
        )
    }