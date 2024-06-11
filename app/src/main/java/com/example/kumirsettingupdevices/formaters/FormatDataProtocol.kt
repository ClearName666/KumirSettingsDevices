package com.example.kumirsettingupdevices.formaters

import androidx.compose.ui.text.toLowerCase
import java.util.Locale

class FormatDataProtocol {



    fun reCalculateFormat(state: String): List<Int> {
        return when (state) {
            "1" -> listOf(0, 1, 0) // 8 данных, 2 стопа, без четности
            "2" -> listOf(0, 0, 1) // 8 данных, 1 стоп, с четностью
            "3" -> listOf(0, 0, 0) // 8 данных, 1 стоп, без четности
            "4" -> listOf(1, 1, 0) // 7 данных, 2 стопа, без четности
            "5" -> listOf(1, 0, 1) // 7 данных, 1 стоп, с четностью
            "6" -> listOf(1, 0, 0) // 7 данных, 1 стоп, без четности
            else -> throw IllegalArgumentException("Invalid reCalculateFormat ${javaClass.name}")
        }
    }

    fun getSpeedIndax(speed: String): Int {
        try {
            return when(speed) {
                "300" -> 0
                "600" -> 1
                "1200" -> 2
                "2400" -> 3
                "4800" -> 4
                "9600" -> 5
                "19200" -> 6
                "38400" -> 7
                "57600" -> 8
                "115200" -> 9
                else -> -1
            }
        } catch (e: Exception) {
            return -1
        }
    }

    fun formatBitData(bitData: String): Int {
        return when(bitData) {
            "8" -> 0
            "7" -> 1
            else -> -1
        }
    }

    fun formatStopBit(stopBit: String): Int {
        return when(stopBit) {
            "1" -> 0
            "2" -> 1
            else -> -1
        }
    }

    fun formatPatity(byteParity: String): Int {
        return when(byteParity.lowercase(Locale.ROOT)) {
            "n" -> 0
            "e" -> 1
            "o" -> 2
            else -> -1
        }
    }

    fun getSpeedFromIndex(index: Int): String {
        return when(index) {
            0 -> "300"
            1 -> "600"
            2 -> "1200"
            3 -> "2400"
            4 -> "4800"
            5 -> "9600"
            6 -> "19200"
            7 -> "38400"
            8 -> "57600"
            9 -> "115200"
            else -> ""
        }
    }

    fun formatBitDataFromIndex(index: Int): String {
        return when(index) {
            0 -> "8"
            1 -> "7"
            else -> ""
        }
    }

    fun formatStopBitFromIndex(index: Int): String {
        return when(index) {
            0 -> "1"
            1 -> "2"
            else -> ""
        }
    }

    fun formatParityFromIndex(index: Int): String {
        return when(index) {
            0 -> "n"
            1 -> "e"
            2 -> "o"
            else -> ""
        }
    }



}