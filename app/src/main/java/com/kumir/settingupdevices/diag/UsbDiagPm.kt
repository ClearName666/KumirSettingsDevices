package com.kumir.settingupdevices.diag

interface UsbDiagPm {
    var keyNet: String
    var mode: String
    var range: Int
    fun printError()
    fun printAll(allBaseStations: String)
    fun stopDiag()
}