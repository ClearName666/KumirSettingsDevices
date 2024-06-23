package com.example.kumirsettingupdevices.dataBasePreset


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Insert
    suspend fun insert(preset: Preset)

    @Query("SELECT * FROM Preset")
    fun getAll(): Flow<List<Preset>>

    @Delete
    fun delete(preset: Preset)

    @Query("DELETE FROM Preset WHERE name = :presetName")
    suspend fun deleteByName(presetName: String)

    @Query("UPDATE Preset SET mode = :mode, apn = :apn, server = :server, port = :port, login = :login, password = :password WHERE name = :name")
        suspend fun updateByName(name: String, mode: Int?, apn: String?, server: String?, port: String?, login: String?, password: String?)

    @Query("UPDATE Preset SET name = :name, mode = :mode, apn = :apn, server = :server, port = :port, login = :login, password = :password WHERE id = :id")
    suspend fun updateById(id: Int, name: String, mode: Int?, apn: String?, server: String?, port: String?, login: String?, password: String?)


    @Query("SELECT * FROM Preset WHERE name = :presetName LIMIT 1")
    suspend fun getFirstByName(presetName: String): Preset?
}