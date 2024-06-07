package com.example.kumirsettingupdevices.dataBasePreset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnforaDao {
    @Insert
    suspend fun insert(enfora: Enfora)

    @Query("SELECT * FROM Enfora")
    fun getAll(): Flow<List<Enfora>>

    @Delete
    suspend fun delete(enfora: Enfora)

    @Query("DELETE FROM Enfora WHERE name = :name")
    suspend fun deleteByName(name: String)
}