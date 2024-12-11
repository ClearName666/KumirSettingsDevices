package com.kumir.settingupdevices.dataBasePreset

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Enfora(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "apn") val apn: String?,
    @ColumnInfo(name = "login") val login: String?,
    @ColumnInfo(name = "password") val password: String?,
    @ColumnInfo(name = "server1") val server1: String?,
    @ColumnInfo(name = "server2") val server2: String?,
    @ColumnInfo(name = "timeout") val timeout: String?,
    @ColumnInfo(name = "sizeBuffer") val sizeBuffer: String?
)