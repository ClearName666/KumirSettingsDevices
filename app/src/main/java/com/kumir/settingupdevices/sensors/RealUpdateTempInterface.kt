package com.kumir.settingupdevices.sensors

interface RealUpdateTempInterface <T> {
    fun updateTempItem(index: Int, newItem: T)
}