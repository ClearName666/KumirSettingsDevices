package com.example.kumirsettingupdevices.dataBasePreset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PmDao {
    @Insert
    suspend fun insert(pm: Pm)

    @Query("SELECT * FROM Pm")
    fun getAll(): Flow<List<Pm>>

    @Delete
    suspend fun delete(pm: Pm)

    @Query("DELETE FROM Pm WHERE name = :name")
    suspend fun deleteByName(name: String)
}