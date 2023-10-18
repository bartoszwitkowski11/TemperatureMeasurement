package com.example.temperaturemeasurement

import android.app.Application
import android.content.Context
import com.example.temperaturemeasurement.UniqueSensorsDatabase.UniqueSensorsDatabase
import com.example.temperaturemeasurement.UniqueSensorsDatabase.UniqueSensorsRepository
import com.example.temperaturemeasurement.TemperaturesDatabase.TemperaturesDatabase
import com.example.temperaturemeasurement.TemperaturesDatabase.TemperaturesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class TemperaturesApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { TemperaturesDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TemperaturesRepository(database.temperaturesDao()) }

    val databaseS by lazy { UniqueSensorsDatabase.getDatabase(this, applicationScope) }
    val repositoryS by lazy { UniqueSensorsRepository(databaseS.sensorsDao()) }

}