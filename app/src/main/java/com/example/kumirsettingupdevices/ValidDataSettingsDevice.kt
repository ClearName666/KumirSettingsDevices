package com.example.kumirsettingupdevices



// проверки на валидность данных
class ValidDataSettingsDevice {

    companion object {
        const val KEEPALIVE_MAX: Int = 600
        const val KEEPALIVE_MIN: Int = 10

        const val CTIMEOUT_MAX: Int = 4320
        const val CTIMEOUT_MIN: Int = 1

        const val TCPPORT_MAX: Int = 65535
        const val TCPPORT_MIN: Int = 1024

        const val POWER_MAX: Int = 14
        const val POWER_MIN: Int = -16


        // enfora
        const val PADBLK_MAX: Int = 1472
        const val PADBLK_MIN: Int = 3

        const val PADTO_MAX: Int = 65535
        const val PADTO_MIN: Int = 0
    }
    // проверка PADTO для enfora
    fun padtoValid(padto: String): Boolean {
        return try {
            !(padto.toInt() >= PADTO_MAX ||
                    padto.toInt() <= PADTO_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка PADBLK для enfora
    fun padblkValid(padto: String): Boolean {
        return try {
            !(padto.toInt() >= PADBLK_MAX ||
                    padto.toInt() <= PADBLK_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка errorKEEPALIVE диопазона от 10 до 600
    fun keepaliveValid(keepalive: String): Boolean {
        return try {
            !(keepalive.toInt() >= KEEPALIVE_MAX ||
                    keepalive.toInt() <= KEEPALIVE_MIN)
        } catch (e: Exception) {
            false
        }

    }

    fun ctimeoutValid(ctimeout: String): Boolean {
        return try {
            !(ctimeout.toInt() >= CTIMEOUT_MAX ||
                    ctimeout.toInt() <= CTIMEOUT_MIN)
        } catch (e: Exception) {
            false
        }
    }


    fun tcpPortValid(tcpPort: String): Boolean {
        return try {
            !(tcpPort.toInt() >= TCPPORT_MAX ||
                    tcpPort.toInt() <= TCPPORT_MIN)
        } catch (e: Exception) {
            false
        }
    }

    fun powerValid(power: String): Boolean {
        try {
            val powerNum: Int = power.toInt()
            if (powerNum in POWER_MIN..POWER_MAX) {
                return true
            }
        } catch (e: Exception) {}
        return false
    }

}