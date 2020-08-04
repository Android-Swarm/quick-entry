package com.zetzaus.quickentry.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

class SimpleLocation(var latitude: Double, var longitude: Double)

@Entity(tableName = "entry_spot")
data class EntrySpot(
    @PrimaryKey val urlId: String,
    val originalName: String,
    var customName: String,
    val url: String,
    @Embedded val location: SimpleLocation,
    var checkedIn: Boolean = false
)