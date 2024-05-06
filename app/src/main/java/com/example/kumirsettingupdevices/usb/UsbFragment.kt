package com.example.kumirsettingupdevices.usb

interface UsbFragment {
    fun printSerifalNumber(serialNumber: String)
    fun printVersionProgram(versionProgram: String)
    fun printSettingDevice(settingMap: Map<String, String>)
    fun readSettingStart()
    fun writeSettingStart()
}