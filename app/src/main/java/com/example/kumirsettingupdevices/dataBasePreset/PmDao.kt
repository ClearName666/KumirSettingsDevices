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


    @Query("UPDATE Pm SET mode = :mode, keyNet = :keyNet, power = :power, diopozone = :diopozone WHERE name = :name")
    suspend fun updateByName(name: String, mode: Int, keyNet: String?, power: String?, diopozone: Int)

    @Query("UPDATE Pm SET name = :name, mode = :mode, keyNet = :keyNet, power = :power, diopozone = :diopozone WHERE id = :id")
    suspend fun updateById(id: Int, name: String, mode: Int, keyNet: String?, power: String?, diopozone: Int)

    @Query("SELECT * FROM Pm WHERE name = :name LIMIT 1")
    suspend fun getFirstByName(name: String): Pm?
}