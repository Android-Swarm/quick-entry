package com.zetzaus.quickentry.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntrySpotDao {

    /**
     * Returns all entry spots ordered by whether the user checked in to the spot and by name.
     *
     * @return all entry spots ordered by whether the user checked in to the spot and by name.
     */
    @Query("SELECT * FROM entry_spot ORDER BY checkedIn, customName")
    fun getAllSpots(): Flow<List<EntrySpot>>

    @Query("SELECT * FROM entry_spot WHERE urlId = :urlId")
    suspend fun getSpotById(urlId: String): EntrySpot

    /**
     * Persists an [EntrySpot] to the database. Use this method to do an update as well.
     *
     * @param spot The [EntrySpot] object to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(spot: EntrySpot)
}