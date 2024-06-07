package com.example.kumirsettingupdevices.settings

import com.example.kumirsettingupdevices.dataBasePreset.Pm

// класс хранит в себе все существующие присеты Pm
class PrisetsPmValue {
    companion object {
        val presets: MutableMap<String, Pm> = mutableMapOf()
    }

}