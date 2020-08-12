package com.zetzaus.quickentry.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                        .build()
                }

                INSTANCE

            }
    }
}

class FirebaseHandler private constructor(context: Context) {
    private val database by lazy { Firebase.database.reference }
    private val dao = EntrySpotDatabase.createDatabase(context).entrySpotDao

    /**
     * Tries to update [EntrySpot] first before doing an insertion. The update mechanism is similar
     * to the local [EntrySpot] update operation.
     *
     * @param spot The new data to upsert.
     */
    suspend fun upsert(spot: EntrySpot) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Start of upsert() method")
        database.child(ROOT).child(spot.firebaseSingleId())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to read in save(): ", error.toException())
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val existing = snapshot.getValue<EntrySpot>()
                    existing?.apply {
                        Log.d(TAG, "Found an existing data, recalculating location")
                        location = spot.location middleOf this.location.toLocation()
                    }?.also { overwrite(it) } ?: overwrite(spot)
                }
            })
    }

    /**
     * Overwrites an [EntrySpot] in Firebase Realtime Database.
     *
     * @param spot The new data to write.
     */
    private fun overwrite(spot: EntrySpot) {
        database.child(ROOT).child(spot.firebaseSingleId()).setValue(spot)
            .addOnCompleteListener {
                Log.d(
                    TAG, if (it.isSuccessful) {
                        "$spot has been saved to firebase"
                    } else {
                        "Failed to save to firebase: ${it.exception}"
                    }
                )
            }
    }

    /**
     * Read all [EntrySpot] from Firebase Realtime Database. This function will then insert all
     * [EntrySpot] location which is not in the database yet.
     *
     * @param scope The coroutine scope to update the local database. Use `viewModelScope` if this
     * code is called inside a `ViewModel`. Otherwise, choose the appropriate scope where this code
     * is called.
     */
    suspend fun readAll(scope: CoroutineScope) = withContext(Dispatchers.IO) {
        database.child(ROOT).addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error when reading from firebase", error.toException())
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Received data change event, updating local database")
                Log.d(TAG, "Found ${snapshot.childrenCount} items")
                snapshot.children
                    .mapNotNull {
                        it.getValue<EntrySpot>()?.apply {
                            customName = originalName
                            checkedIn = false
                        }?.also { spot ->
                            Log.d(TAG, spot.originalName)
                        }
                    }.forEach { spot ->
                        scope.launch {
                            dao.getSpotByKey(spot.urlId, spot.originalName) ?: dao.save(spot)
                        }
                    }
            }
        })
    }

    /**
     * Returns a valid Firebase Database path. The path must not contain '.', '#', '$', ']', or '['.
     *
     */
    private fun EntrySpot.firebaseSingleId() =
        this.createSingleId().replace(Regex("""[\[\].#$]"""), "")

    companion object {
        val TAG = FirebaseHandler::class.simpleName
        const val ROOT = "entry_spot"
        private lateinit var INSTANCE: FirebaseHandler

        /**
         * Creates a new [FirebaseHandler], or return an existing one.
         *
         * @param context The context used to retrieve [EntrySpotDao].
         */
        fun getInstance(context: Context) =
            synchronized(FirebaseHandler::class.java) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = FirebaseHandler(context)
                }

                INSTANCE
            }
    }
}