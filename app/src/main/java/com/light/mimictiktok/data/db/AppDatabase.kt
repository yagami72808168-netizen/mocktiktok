package com.light.mimictiktok.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [VideoEntity::class, PlaylistEntity::class, PlaylistVideoCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "videos.db"
                )
                .addCallback(DatabaseCallback())
                .build().also { instance = it }
            }
        }

        fun destroyInstance() {
            instance = null
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Prepopulate database with default data
            instance?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.appDao())
                }
            }
        }

        private suspend fun populateDatabase(dao: AppDao) {
            // Create default playlists
            val favoritesPlaylist = PlaylistEntity(
                id = "favorites",
                name = "Favorites",
                createTime = System.currentTimeMillis(),
                updateTime = System.currentTimeMillis()
            )
            
            val recentPlaylist = PlaylistEntity(
                id = "recent",
                name = "Recent",
                createTime = System.currentTimeMillis(),
                updateTime = System.currentTimeMillis()
            )

            dao.insertPlaylist(favoritesPlaylist)
            dao.insertPlaylist(recentPlaylist)
        }
    }
}