package com.example.kumirsettingupdevices.sensors

interface RealUpdateTempInterface <T> {
    fun updateTempItem(index: Int, newItem: T)
}