package com.example.kumirsettingupdevices.dataBasePreset

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "mode") val mode: Int?,
    @ColumnInfo(name = "apn") val apn: String?,
    @ColumnInfo(name = "server") val server: String?,
    @ColumnInfo(name = "port") val port: String?,
    @ColumnInfo(name = "login") val login: String?,
    @ColumnInfo(name = "password") val password: String?,
)