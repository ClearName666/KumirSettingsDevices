package com.kumir.settingupdevices.formaters

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

        // валидность строки кода символа от 1 до 255
        const val CHAR_BYTE_MAX: Int = 255
        const val CHAR_BYTE_MIN: Int = 0
    }

    private val patterns = mapOf(
        "devmode" to "^(?:KNET|TCP(?:CLIENT|SERVER)|MODEM)$".toRegex(),
        "keepalive" to "^\\d{2,3}$".toRegex(),
        "ctimeout" to "^\\d{1,4}$".toRegex(),
        "smspin" to "^(\\d{4}|DISABLED)$".toRegex(),
        "tzdata" to "^.{1,128}$".toRegex(),
        "port1" to "^(\\d{4,6}),(\\d),([NOE]),(\\d),(\\d{1,4}),(\\d{3,5})$".toRegex(),
        "port2" to "^(\\d{4,6}),(\\d),([NOE]),(\\d),(\\d{1,4}),(\\d{3,5})$".toRegex(),
        "profile1" to "^\\d{1,4}$".toRegex(),
        "profile2" to "^\\d{1,4}$".toRegex(),
        "sim1pin" to "^(\\d{4}|DISABLED)$".toRegex(),
        "sim1apn" to "^[\\dA-Za-z\\-.]{1,128}$".toRegex(),
        "sim1knet" to "^[\\dA-Za-z\\-.]{1,122}(?::\\d{1,5})?$".toRegex(),
        "sim1sntp" to "^(?:[\\dA-Za-z\\-.]{1,128}|\\r)$".toRegex(),
        "sim1tcp1" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "sim1tcp2" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "sim1tcp3" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "sim1tcp4" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "sim2pin" to "^(\\d{4}|DISABLED)$".toRegex(),
        "sim2apn" to "^[\\dA-Za-z\\-.]{1,128}$".toRegex(),
        "sim2knet" to "^[\\dA-Za-z\\-.]{1,122}(?::\\d{1,5})?$".toRegex(),
        "sim2sntp" to "^(?:[\\dA-Za-z\\-.]{1,128}|\\r)$".toRegex(),
        "sim2tcp1" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "sim2tcp2" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "sim2tcp3" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "sim2tcp4" to "^(?:RS(?:232|485)@[\\dA-Za-z\\-.]{1,116}(?::\\d{1,5})?|\\r)$".toRegex(),
        "tcpport1" to "^\\d{1,5}$".toRegex(),
        "tcpport2" to "^\\d{1,5}$".toRegex()
    )

    fun validDevmode(input: String): Boolean = patterns["devmode"]?.matches(input) ?: false
    fun validKeepalive(input: String): Boolean = patterns["keepalive"]?.matches(input) ?: false
    fun validCtimeout(input: String): Boolean = patterns["ctimeout"]?.matches(input) ?: false
    fun validSmspin(input: String): Boolean = input.length == 4
    fun validTzdata(input: String): Boolean = patterns["tzdata"]?.matches(input) ?: false
    fun validPort1(input: String): Boolean = patterns["port1"]?.matches(input) ?: false
    fun validPort2(input: String): Boolean = patterns["port2"]?.matches(input) ?: false
    fun validProfile1(input: String): Boolean = patterns["profile1"]?.matches(input) ?: false
    fun validProfile2(input: String): Boolean = patterns["profile2"]?.matches(input) ?: false
    fun validSim1pin(input: String): Boolean = input.length == 4
    fun validSim1apn(input: String): Boolean = patterns["sim1apn"]?.matches(input) ?: false
    fun validSim1knet(input: String): Boolean = patterns["sim1knet"]?.matches(input) ?: false
    fun validSim1sntp(input: String): Boolean = patterns["sim1sntp"]?.matches(input) ?: false
    fun validSim1tcp1(input: String): Boolean = patterns["sim1tcp1"]?.matches(input) ?: false
    fun validSim1tcp2(input: String): Boolean = patterns["sim1tcp2"]?.matches(input) ?: false
    fun validSim1tcp3(input: String): Boolean = patterns["sim1tcp3"]?.matches(input) ?: false
    fun validSim1tcp4(input: String): Boolean = patterns["sim1tcp4"]?.matches(input) ?: false
    fun validSim2pin(input: String): Boolean = input.length == 4
    fun validSim2apn(input: String): Boolean = patterns["sim2apn"]?.matches(input) ?: false
    fun validSim2knet(input: String): Boolean = patterns["sim2knet"]?.matches(input) ?: false
    fun validSim2sntp(input: String): Boolean = patterns["sim2sntp"]?.matches(input) ?: false
    fun validSim2tcp1(input: String): Boolean = patterns["sim2tcp1"]?.matches(input) ?: false
    fun validSim2tcp2(input: String): Boolean = patterns["sim2tcp2"]?.matches(input) ?: false
    fun validSim2tcp3(input: String): Boolean = patterns["sim2tcp3"]?.matches(input) ?: false
    fun validSim2tcp4(input: String): Boolean = patterns["sim2tcp4"]?.matches(input) ?: false
    fun validTcpport1(input: String): Boolean = patterns["tcpport1"]?.matches(input) ?: false
    fun validTcpport2(input: String): Boolean = patterns["tcpport2"]?.matches(input) ?: false




    private fun isCp1251String(str: String): Boolean {
        val charsetCp1251 = Charset.forName("windows-1251")
        val bytes = str.toByteArray(charsetCp1251)
        val decodedString = String(bytes, charsetCp1251)
        return str == decodedString
    }

    // проверка на то что строка хекс
    fun isValidHex(str: String): Boolean {
        val hexRegex = Regex("^[0-9a-fA-F]+$")
        return hexRegex.matches(str)
    }

    // проверка на аски символы
    fun isAscii(input: String): Boolean {
        if (!input.matches("^\\p{ASCII}*$".toRegex())) return false

        for (c in input) {
            if ((c.code !in 48..57 && (c.code < 65 ||  c.code > 122)) ||
                (c.code in 91..96))
                if (c.code != 95) return false
        }

        return true
    }

    fun validCharStringCode(char: String): Boolean {
        return try {
            !(char.toInt() >= CHAR_BYTE_MAX ||
                    char.toInt() <= CHAR_BYTE_MIN)
        } catch (e: Exception) {
            false
        }
    }

    fun validPassword(password: String): Boolean {
        return password.length == PASSWORD_SIZE
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
            !(timeout.toInt() > RENGE_P101_MAX ||
                    timeout.toInt() < RENGE_P101_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка задержки п101
    fun validTimeOutP101(timeout: String): Boolean {
        return try {
            !(timeout.toInt() > TIMEOUT_P101_MAX ||
                    timeout.toInt() < TIMEOUT_P101_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка ожидания п101
    fun validTimeP101(timeout: String): Boolean {
        return try {
            !(timeout.toInt() > TIME_P101_MAX ||
                    timeout.toInt() < TIME_P101_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка п101 индитификатора девайса
    /*fun validIdDeviceP101(devId: String): Boolean {
        return devId == "234" || devId == "236" || devId == "204" || devId == "230"
    }*/

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
            !(padto.toInt() > PADTO_MAX ||
                    padto.toInt() < PADTO_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка PADBLK для enfora
    fun padblkValid(padblk: String): Boolean {
        return try {
            !(padblk.toInt() > PADBLK_MAX ||
                    padblk.toInt() < PADBLK_MIN)
        } catch (e: Exception) {
            false
        }
    }

    // проверка errorKEEPALIVE диопазона от 10 до 600
    fun keepaliveValid(keepalive: String): Boolean {
        return try {
            !(keepalive.toInt() > KEEPALIVE_MAX ||
                    keepalive.toInt() < KEEPALIVE_MIN)
        } catch (e: Exception) {
            false
        }

    }

    fun ctimeoutValid(ctimeout: String): Boolean {
        return try {
            !(ctimeout.toInt() > CTIMEOUT_MAX ||
                    ctimeout.toInt() < CTIMEOUT_MIN)
        } catch (e: Exception) {
            false
        }
    }


    fun tcpPortValid(tcpPort: String): Boolean {
        return try {
            !(tcpPort.toInt() > TCPPORT_MAX ||
                    tcpPort.toInt() < TCPPORT_MIN)
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