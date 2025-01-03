package com.kumir.settingupdevices.dataBasePreset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
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

    suspend fun upsert(pm: Pm, context: MainActivity) {
        val existingPm = getFirstByName(pm.name ?: "")

        val reservedName = listOf(
            context.getString(R.string.priset1),
            context.getString(R.string.priset2),
            context.getString(R.string.priset3),
            context.getString(R.string.priset4),
            context.getString(R.string.priset5),
        )

        val flagPresence: Boolean = pm.name in reservedName

        if (existingPm != null || flagPresence) {
            context.runOnUiThread {
                context.menuUpdateName(null, pm, null)
            }
        } else {
            insert(pm)
            context.runOnUiThread {
                if (context.flagLoadPreset) {
                    context.showAlertDialog(context.getString(R.string.sucPresetLoadDataBase))

                    // выозврат флага вдруг он соит в позиции диалога с успешным сохранением
                    context.flagLoadPreset = false
                } else {
                    context.showAlertDialog(context.getString(R.string.sucPresetSaveDataBase))
                }
            }
        }


    }
}