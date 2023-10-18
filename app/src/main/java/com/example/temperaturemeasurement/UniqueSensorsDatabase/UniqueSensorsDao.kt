package com.example.temperaturemeasurement.UniqueSensorsDatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface UniqueSensorsDao {

    @Query("SELECT * FROM unique_working_sensors ORDER BY valueName ASC")
    fun getAll(): List<UniqueSensors>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sensors: UniqueSensors)

   @Query("UPDATE unique_working_sensors SET value_temp = :valTemp WHERE valueName = :valName")
    suspend fun update(valName: String, valTemp: String)

    @Query("UPDATE unique_working_sensors SET to_read = :valRead WHERE valueName = :valName")
    suspend fun updateToRead(valName: String, valRead: Boolean)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(sensors: UniqueSensors)

    @Delete
    suspend fun delete(sensors: UniqueSensors)

    @Query("DELETE FROM unique_working_sensors")
    suspend fun deleteAll()

    @Query("SELECT path_name FROM unique_working_sensors ORDER BY valueName ASC")
    fun getPathName(): Array<String>

    @Query("SELECT path_temp FROM unique_working_sensors ORDER BY valueName ASC")
    fun getPathTemp(): Array<String>

    @Query("SELECT valueName FROM unique_working_sensors ORDER BY valueName ASC")
    fun getName(): Array<String>

    @Query("SELECT to_read FROM unique_working_sensors ORDER BY valueName ASC")
    fun getToRead(): Array<Boolean>

    @Query("SELECT to_read FROM unique_working_sensors WHERE valueName = :valName")
    fun getToReadSingle(valName: String): Boolean

    @Query("SELECT * FROM unique_working_sensors ORDER BY valueName ASC")
    fun getSensors(): Flow<List<UniqueSensors>>

}