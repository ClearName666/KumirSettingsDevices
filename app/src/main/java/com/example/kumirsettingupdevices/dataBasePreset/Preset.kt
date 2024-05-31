package com.example.kumirsettingupdevices.dataBasePreset

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val mode: Int,
    val apn: String,
    val server: String,
    val port: String,
    val login: String,
    val password: String
)