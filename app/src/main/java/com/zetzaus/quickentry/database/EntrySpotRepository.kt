package com.zetzaus.quickentry.database

import android.content.Context

class EntrySpotRepository(context: Context) {

    private val dao = EntrySpotDatabase.createDatabase(context).entrySpotDao

    val entrySpotsFlow
        get() = dao.getAllSpots()

    fun getSpotsContaining(query: String) = dao.getSpotsContaining("%$query%")

    suspend fun save(entrySpot: EntrySpot) = dao.save(entrySpot)

    suspend fun getById(urlId: String) = dao.getSpotById(urlId)

    suspend fun getByIdOrNull(urlId: String) = try {
        getById(urlId)
    } catch (e: Exception) {
        null
    }
}