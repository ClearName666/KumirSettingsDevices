package com.kumir.settingupdevices.settings

import com.kumir.settingupdevices.dataBasePreset.Pm

// класс хранит в себе все существующие присеты Pm
class PrisetsPmValue {
    companion object {
        val presets: MutableMap<String, Pm> = mutableMapOf()
    }

}