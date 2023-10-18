package com.example.temperaturemeasurement.TemperaturesDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [Temperatures::class], version = 1, exportSchema = false)
abstract class TemperaturesDatabase  : RoomDatabase() {
    abstract fun temperaturesDao(): TemperaturesDao

    private class TemperaturesDatabaseCallback(private val scope: CoroutineScope) :
        RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.temperaturesDao())
                }
            }
        }

        suspend fun populateDatabase(temperaturesDao: TemperaturesDao) {
            temperaturesDao.deleteAll()

            var temps =
                Temperatures("test", "21.0", "test", "Sample name3", System.currentTimeMillis())
            temperaturesDao.insert(temps)
        }

    }

    companion object {

        @Volatile
        private var INSTANCE: TemperaturesDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TemperaturesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TemperaturesDatabase::class.java,
                    "temperature_results_database"
                )
                    .allowMainThreadQueries()
                    .addCallback(TemperaturesDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

