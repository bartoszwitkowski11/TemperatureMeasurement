package com.example.temperaturemeasurement.TemperaturesDatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemperaturesDao {
    @Query("SELECT * FROM temperature_measurement_results")
    fun getAllYourTableData(): List<Temperatures>

    @Insert
    suspend fun insert(temperatures: Temperatures)

    @Query("DELETE FROM temperature_measurement_results")
    suspend fun deleteAll()

    @Query("SELECT value_name FROM temperature_measurement_results")
    fun getName(): Array<String>

    @Query("SELECT * FROM temperature_measurement_results ORDER BY id ASC")
    fun getTemperatures(): Flow<List<Temperatures>>

}