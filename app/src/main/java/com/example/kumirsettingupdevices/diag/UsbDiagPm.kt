package com.example.kumirsettingupdevices.diag

interface UsbDiagPm {
    fun printError()
    fun printAllBaseStations(allBaseStations: String)
    fun printAllBasePingsend(pingsend: String)
    fun stopDiag()
}