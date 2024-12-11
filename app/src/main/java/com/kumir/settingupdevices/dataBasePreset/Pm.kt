package com.kumir.settingupdevices.dataBasePreset

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Pm(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "mode") val mode: Int,
    @ColumnInfo(name = "keyNet") val keyNet: String?,
    @ColumnInfo(name = "power") val power: String?,
    @ColumnInfo(name = "diopozone") val diopozone: Int
)