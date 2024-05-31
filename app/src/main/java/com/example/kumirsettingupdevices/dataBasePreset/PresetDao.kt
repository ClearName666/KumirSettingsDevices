package com.example.kumirsettingupdevices.dataBasePreset


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Insert
    suspend fun insert(preset: Preset)

    @Query("SELECT * FROM Preset")
    fun getAll(): Flow<List<Preset>>
}