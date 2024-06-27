package com.example.kumirsettingupdevices.diag

interface UsbDiagPm {
    var keyNet: String
    var mode: String
    fun printError()
    fun printAll(allBaseStations: String)
    fun stopDiag()
}