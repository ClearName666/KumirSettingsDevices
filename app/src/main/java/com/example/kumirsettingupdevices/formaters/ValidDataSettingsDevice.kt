package com.example.kumirsettingupdevices.formaters

import java.nio.charset.Charset


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

        const val KEYNET_CHAR_MAX: Int = 60


        // enfora
        const val PADBLK_MAX: Int = 1472
        const val PADBLK_MIN: Int = 3

        const val PADTO_MAX: Int = 65535
        const val PADTO_MIN: Int = 0

        const val APN_CHAR_MAX: Int = 128


        // m32
        const val PROV_CHAR_MAX = 63
        const val PROV_CHAR_MIN = 0

        // пин код для sim
        const val PIN_SIM_CHAR = 4

        // пароль п101
        const val PASSWORD_SIZE: Int = 6

        // интервал п101
        const val RENGE_P101_MAX: Int = 200
        const val RENGE_P101_MIN: Int = 10

        // задержка п101
        const val TIMEOUT_P101_MAX: Int = 600
        const val TIMEOUT_P101_MIN: Int = 1

        // ожидение п101
        const val TIME_P101_MAX: Int = 5000
        const val TIME_P101_MIN: Int = 200

        // время ожидания вывзова
        const val TIME_AB_MAX: Int = 30
        const val TIME_AB_MIN: Int = 2
    }


    private fun isCp1251String(str: String): Boolean {
        val charsetCp1251 = Charset.forName("windows-1251")
        val bytes = str.toByteArray(charsetCp1251)
        val decodedString = String(bytes, charsetCp1251)
        return str == decodedString
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val regex = Regex("^\\+7\\d{10}\$")
        return regex.matches(phoneNumber)
    }

    fun validTimeAbonent(abTime: String): Boolean {
        return try {
            !(abTime.toInt() >= TIME_AB_MAX ||
                    abTime.toInt() <= TIME_AB_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка праоля на 6 символов а так же на ASCII символы
    fun validPasswordP101(password: String): Boolean {
        return password.length == PASSWORD_SIZE && password.matches(Regex("^[\\x00-\\x7F]*$"))
    }

    // проверка имени  на 1251 символы
    fun validNameP101(name: String): Boolean {
        return isCp1251String(name) && name.trim().isNotEmpty()
    }

    // проверка интервала п101
    fun validRangeP101(timeout: String): Boolean {
        return try {
            !(timeout.toInt() >= RENGE_P101_MAX ||
                    timeout.toInt() <= RENGE_P101_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка задержки п101
    fun validTimeOutP101(timeout: String): Boolean {
        return try {
            !(timeout.toInt() >= TIMEOUT_P101_MAX ||
                    timeout.toInt() <= TIMEOUT_P101_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка ожидания п101
    fun validTimeP101(timeout: String): Boolean {
        return try {
            !(timeout.toInt() >= TIME_P101_MAX ||
                    timeout.toInt() <= TIME_P101_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка п101 индитификатора девайса
    fun validIdDeviceP101(devId: String): Boolean {
        return devId == "234" || devId == "236" || devId == "204" || devId == "230"
    }

    fun validPM81KeyNet(keyNet: String): Boolean {
        return keyNet.length <= KEYNET_CHAR_MAX
    }

    fun validAPNEnfora(apn: String): Boolean {
        return apn.length <= APN_CHAR_MAX
    }

    fun validServer(server: String): Boolean {
        val ipPattern = "^([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})$".toRegex()

        // Проверяем, соответствует ли строка регулярному выражению
        val matchResult = ipPattern.matchEntire(server) ?: return false
        // Получаем группы чисел из совпадения
        val (part1, part2, part3, part4) = matchResult.destructured
        // Проверяем, что каждое число находится в диапазоне от 0 до 255
        return listOf(part1, part2, part3, part4).all { it.toInt() in 0..255 }
    }


    fun charPROV_CHAR_MAXValid(str: String): Boolean {
        return str.length <= PROV_CHAR_MAX
    }

    // проверка паролей на смс и сим
    fun simPasswordValid(simPin: String): Boolean {
        return simPin.length == PIN_SIM_CHAR
    }


    // проверка логина и пароля
    fun loginPasswordValid(loginPassword: String): Boolean {
        if (loginPassword.split(",").size != 2) {
            return false
        }
        for (c in loginPassword) {
            if (c == ' ' || c == '\"') {
                return false
            }
        }

        return true
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
    fun padblkValid(padblk: String): Boolean {
        return try {
            !(padblk.toInt() >= PADBLK_MAX ||
                    padblk.toInt() <= PADBLK_MIN)
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

    // прерка на русские символы
    fun serverValid(server: String): Boolean {
        for (c in server) {
            if (c !in 'A'..'Z' && c !in 'a'..'z' && c !in '0'..'9')
                if (c !='.' && c != '-')
                    return false
        }
        return true
    }

}