package com.example.temperaturemeasurement.UniqueSensorsDatabase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unique_working_sensors")
data class UniqueSensors(
    @ColumnInfo(name = "path_temp") val pathTemp: String,
    @ColumnInfo(name = "value_temp") val valueTemp: String,
    @ColumnInfo(name = "path_name") val pathName: String,
    @ColumnInfo(name = "to_read") val toRead: Boolean,
    @PrimaryKey(autoGenerate = false) val valueName: String)