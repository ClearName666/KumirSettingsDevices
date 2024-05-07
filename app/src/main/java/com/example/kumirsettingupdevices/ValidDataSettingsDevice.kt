package com.example.kumirsettingupdevices

import com.example.kumirsettingupdevices.usbFragments.M32Fragment


// проверки на валидность данных
class ValidDataSettingsDevice {

    companion object {
        const val KEEPALIVE_MAX: Int = 600
        const val KEEPALIVE_MIN: Int = 10

        const val CTIMEOUT_MAX: Int = 4320
        const val CTIMEOUT_MIN: Int = 1

        const val TCPPORT_MAX: Int = 65535
        const val TCPPORT_MIN: Int = 1024
    }

    // проверка errorKEEPALIVE диопазона от 10 до 600
    fun keepaliveValid(keepalive: String): Boolean {
        return !(keepalive.toInt() >= KEEPALIVE_MAX ||
                keepalive.toInt() <= KEEPALIVE_MIN)
    }

    fun ctimeoutValid(ctimeout: String): Boolean {
        return !(ctimeout.toInt() >= CTIMEOUT_MAX ||
                ctimeout.toInt() <= CTIMEOUT_MIN)
    }

    fun tcpPortValid(tcpPort: String): Boolean {
        return !(tcpPort.toInt() >= TCPPORT_MAX ||
                tcpPort.toInt() <= TCPPORT_MIN)
    }

}