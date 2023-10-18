package com.example.temperaturemeasurement.TemperaturesDatabase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temperature_measurement_results")
data class Temperatures(
    @ColumnInfo(name = "path_temp") val pathTemp: String,
    @ColumnInfo(name = "value_temp") val valueTemp: String,
    @ColumnInfo(name = "path_name") val pathName: String,
    @ColumnInfo(name = "value_name") val valueName: String,
    @ColumnInfo(name = "time_stamp") val timeStamp: Long,
    @PrimaryKey(autoGenerate = true) val id: Int = 0)
