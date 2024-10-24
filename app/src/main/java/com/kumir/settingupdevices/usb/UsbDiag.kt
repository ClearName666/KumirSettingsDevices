package com.kumir.settingupdevices.usb

interface UsbDiag {
    fun printAllInfo(info: String)
    fun printAllOperator(allOperators: String)
    fun printError()
    fun noConnect()
}