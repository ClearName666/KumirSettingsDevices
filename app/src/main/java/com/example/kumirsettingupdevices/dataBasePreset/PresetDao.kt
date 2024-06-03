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
}