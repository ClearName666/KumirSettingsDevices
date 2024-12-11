package com.kumir.settingupdevices.formaters

class FormatModems {
    fun formatSpeedModBas(speed: Int): Int {
        return when(speed) {
            0 -> 0
            1 -> 1
            2 -> 2
            4 -> 3
            5 -> 4
            7 -> 5
            9 -> 6
            11 -> 7
            12 -> 8
            13 -> 9
            else -> -1
        }
    }

    fun formatBitDataModBas(bitData: Int): Int {
        return when(bitData) {
            8 -> 0
            7 -> 1
            else -> -1
        }
    }

    fun formatStopBitModBas(stopBit: Int): Int {
        return when(stopBit) {
            1 -> 0
            2 -> 1
            else -> -1
        }
    }

    fun formatPatityModBas(byteParity: Byte): Int {
        return when(byteParity) {
            0x4E.toByte() -> 0
            0x45.toByte() -> 1
            0x4F.toByte() -> 2
            else -> -1
        }
    }



    // Обратные методы для преобразования в исходные значения
    fun reverseFormatSpeedModBas(formattedSpeed: Int): Byte {
        return when(formattedSpeed) {
            0 -> 0.toByte()
            1 -> 1.toByte()
            2 -> 2.toByte()
            3 -> 4.toByte()
            4 -> 5.toByte()
            5 -> 7.toByte()
            6 -> 9.toByte()
            7 -> 11.toByte()
            8 -> 12.toByte()
            9 -> 13.toByte()
            else -> -1
        }
    }

    fun reverseFormatBitDataModBas(formattedBitData: Int): Byte {
        return when(formattedBitData) {
            0 -> 8.toByte()
            1 -> 7.toByte()
            else -> -1
        }
    }

    fun reverseFormatStopBitModBas(formattedStopBit: Int): Byte {
        return when(formattedStopBit) {
            0 -> 1.toByte()
            1 -> 2.toByte()
            else -> -1
        }
    }

    fun reverseFormatPatityModBas(formattedByteParity: Int): Byte {
        return when(formattedByteParity) {
            0 -> 0x4E.toByte()
            1 -> 0x45.toByte()
            2 -> 0x4F.toByte()
            else -> (-1).toByte()
        }
    }
}