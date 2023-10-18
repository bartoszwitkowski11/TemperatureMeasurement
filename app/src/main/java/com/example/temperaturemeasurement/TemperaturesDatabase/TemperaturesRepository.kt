package com.example.temperaturemeasurement.TemperaturesDatabase

import android.content.Context
import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class TemperaturesRepository(private val temperaturesDao: TemperaturesDao) {
    val allTemperatures: Flow<List<Temperatures>> = temperaturesDao.getTemperatures()
    val SensorName: Array<String> = temperaturesDao.getName()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    fun getAllData(): List<Temperatures> {
        return temperaturesDao.getAllYourTableData()
    }

            @Suppress("RedundantSuspendModifier")
            @WorkerThread
            suspend fun insert(temperatures: Temperatures) {
                temperaturesDao.insert(temperatures)
            }


            @Suppress("RedundantSuspendModifier")
            @WorkerThread
            suspend fun deleteAll() {
                temperaturesDao.deleteAll()
            }
}