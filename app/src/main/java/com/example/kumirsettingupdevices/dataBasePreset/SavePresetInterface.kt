package com.example.kumirsettingupdevices.dataBasePreset

interface SavePresetInterface {
    fun savePreset(mode: Int, apn: String, server: String, port: String, login: String, password: String)
}