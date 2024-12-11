package com.kumir.settingupdevices.formaters

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

    fun getModeMainReverse(code: Int?): String {
        return when (code) {
            0 -> "kumirNet"
            1 -> "клиент"
            2 -> "сервер"
            3 -> "модем"
            else -> ""
        }
    }

    fun getModePmReverse(code: Int?): String {
        return when (code) {
            0 -> "ROUTER"
            1 -> "CANPROXY"
            2 -> "RS485"
            3 -> "MONITOR"
            else -> ""
        }
    }

    fun getBandPmReverse(code: Int?): String {
        return when (code) {
            0 -> "1) 864-865МГц»"
            1 -> "2) 866-868МГц"
            2 -> "3) 868.7-869.2МГц"
            else -> ""
        }
    }

}