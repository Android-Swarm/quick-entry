package com.zetzaus.quickentry.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EntrySpot::class], version = 1)
abstract class EntrySpotDatabase : RoomDatabase() {
    abstract val entrySpotDao: EntrySpotDao

    companion object {
        private lateinit var INSTANCE: EntrySpotDatabase

        fun createDatabase(context: Context) =
            synchronized(EntrySpotDatabase::class.java) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        EntrySpotDatabase::class.java,
                        "entry_spot"
                    )
//                        .fallbackToDestructiveMigration()
                        .build()
                }

                INSTANCE

            }
    }
}