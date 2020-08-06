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

    /**
     * Returns all entry spots that contains the query string in the spot's custom name.
     *
     * @param query The query string that must exist in the spot's custom name.
     *
     * @return all entry spots that contains the query string in the spot's custom name.
     */
    @Query("SELECT * FROM entry_spot WHERE customName LIKE :query ORDER BY checkedIn, customName")
    fun getSpotsContaining(query: String): Flow<List<EntrySpot>>

    /**
     * Returns an entry spot according to the given id.
     *
     * @param urlId The ID of the spot. In the Safe Entry URL it is the leaf of the URL. In the check in
     *              or check out page, it is the second last path of the URL.
     *
     * @return An entry spot with the given id.
     */
    @Query("SELECT * FROM entry_spot WHERE urlId = :urlId AND originalName LIKE :originalName")
    suspend fun getSpotByKey(urlId: String, originalName: String): EntrySpot?

    /**
     * Persists an [EntrySpot] to the database. Use this method to do an update as well.
     *
     * @param spot The [EntrySpot] object to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(spot: EntrySpot)
}