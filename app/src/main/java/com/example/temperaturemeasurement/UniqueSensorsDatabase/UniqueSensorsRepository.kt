package com.example.temperaturemeasurement.UniqueSensorsDatabase

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class UniqueSensorsRepository(private val sensorsDao: UniqueSensorsDao) {
    val allSensors: Flow<List<UniqueSensors>> = sensorsDao.getSensors()
    val PathName: Array<String> = sensorsDao.getPathName()
    val PathTemp: Array<String> = sensorsDao.getPathTemp()
    val ToRead: Array<Boolean> = sensorsDao.getToRead()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(sensors: UniqueSensors) {
        sensorsDao.insert(sensors)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun update(valName: String, valTemp: String) {
        sensorsDao.update(valName, valTemp)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun getToReadSingle(valName: String) {
        sensorsDao.getToReadSingle(valName)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updateToRead(valName: String, valRead: Boolean) {
        sensorsDao.updateToRead(valName, valRead)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updateAll(sensors: UniqueSensors) {
        sensorsDao.updateAll(sensors)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun deleteAll() {
        sensorsDao.deleteAll()
    }


}