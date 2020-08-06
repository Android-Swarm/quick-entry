package com.zetzaus.quickentry.database

import android.content.Context

class EntrySpotRepository(context: Context) {

    private val dao = EntrySpotDatabase.createDatabase(context).entrySpotDao
    private val firebase = FirebaseHandler.getInstance(context)

    val entrySpotsFlow
        get() = dao.getAllSpots()

    fun getSpotsContaining(query: String) = dao.getSpotsContaining("%$query%")

    suspend fun save(entrySpot: EntrySpot) {
        dao.save(entrySpot)
        firebase.upsert(entrySpot)
    }

    /**
     * Gets an entry spot by using its url ID and its original name. If there is no match,
     * null is returned.
     *
     * @param urlId The URL id.
     * @param originalName The original location name.
     */
    suspend fun getById(urlId: String, originalName: String) = dao.getSpotByKey(urlId, originalName)
}