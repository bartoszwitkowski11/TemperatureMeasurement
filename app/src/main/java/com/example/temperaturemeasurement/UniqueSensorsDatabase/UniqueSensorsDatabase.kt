package com.example.temperaturemeasurement.UniqueSensorsDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Database(entities = [UniqueSensors::class], version = 1, exportSchema = false)
abstract class UniqueSensorsDatabase : RoomDatabase() {
    abstract fun sensorsDao() : UniqueSensorsDao

    private class UniqueSensorsDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.sensorsDao())
                }
            }
        }

        suspend fun populateDatabase(sensorsDao: UniqueSensorsDao) {
            sensorsDao.deleteAll()
        }

    }

    companion object{

        @Volatile
        private var INSTANCE : UniqueSensorsDatabase?= null

        fun getDatabase(context: Context, scope: CoroutineScope): UniqueSensorsDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UniqueSensorsDatabase::class.java,
                    "unique_sensors_database"
                )
                    .allowMainThreadQueries()
                    .addCallback(UniqueSensorsDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}