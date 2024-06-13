package com.example.kumirsettingupdevices.formaters

class ValidDataIniFile {
    fun getModeMain(mode: String?): Int {
        return when (mode) {
            "kumirNet" -> 0
            "клиент" -> 1
            "сервер" -> 2
            "модем" -> 3
            else -> -1
        }
    }

    fun getModePm(mode: String?): Int {
        return when (mode) {
            "ROUTER" -> 0
            "CANPROXY" -> 1
            "RS485" -> 2
            "MONITOR" -> 3
            else -> -1
        }
    }

    fun getBandPm(band: String?): Int {
        return when (band) {
            "1) 864-865МГц»" -> 0
            "2) 866-868МГц" -> 1
            "3) 868.7-869.2МГц" -> 2
            else -> -1
        }
    }
}