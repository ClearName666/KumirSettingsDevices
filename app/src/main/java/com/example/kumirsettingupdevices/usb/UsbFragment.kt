package com.example.kumirsettingupdevices.usb

interface UsbFragment {
    val usbCommandsProtocol: UsbCommandsProtocol
    fun printSerifalNumber(serialNumber: String)
    fun printVersionProgram(versionProgram: String)
    fun printSettingDevice(settingMap: Map<String, String>)
    fun readSettingStart()
    fun writeSettingStart()
    fun lockFromDisconnected(connect: Boolean)
}