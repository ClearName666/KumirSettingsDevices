package com.example.kumirsettingupdevices.dataBasePreset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.kumirsettingupdevices.MainActivity
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

    @Query("UPDATE Enfora SET apn = :apn, login = :login, password = :password, server1 = :server1, server2 = :server2, timeout = :timeout, sizeBuffer = :sizeBuffer WHERE name = :name")
    suspend fun updateByName(name: String, apn: String?, login: String?, password: String?, server1: String?, server2: String?, timeout: String?, sizeBuffer: String?)

    @Query("UPDATE Enfora SET name = :name, apn = :apn, login = :login, password = :password, server1 = :server1, server2 = :server2, timeout = :timeout, sizeBuffer = :sizeBuffer WHERE id = :id")
    suspend fun updateById(id: Int, name: String, apn: String?, login: String?, password: String?, server1: String?, server2: String?, timeout: String?, sizeBuffer: String?)

    @Query("SELECT * FROM Enfora WHERE name = :name LIMIT 1")
    suspend fun getFirstByName(name: String): Enfora?

    // Новый метод upsert
    suspend fun upsert(enfora: Enfora, context: MainActivity) {
        val existingEnfora = getFirstByName(enfora.name ?: "")
        if (existingEnfora != null) {
            context.runOnUiThread {
                context.menuUpdateName(null, null, enfora)
            }
        } else {
            insert(enfora)
        }
    }
}