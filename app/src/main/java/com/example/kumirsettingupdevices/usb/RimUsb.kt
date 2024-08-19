package com.example.kumirsettingupdevices.usb

import android.util.Log
import com.example.kumirsettingupdevices.DataShowInterface
import com.example.kumirsettingupdevices.MainActivity
import com.example.testappusb.settings.ConstUsbSettings
import com.felhr.usbserial.UsbSerialInterface

class RimUsb(val context: MainActivity, val usbCommandsProtocol: UsbCommandsProtocol) {
    companion object {
        const val TIME_WAIT_SWND_MAX: Int = 200
        const val ERROR_PASSWORD: Byte = 0x94.toByte()
    }


    // отправка данных пл протоколу rs485
    fun writeRS485(address: Byte, data: ByteArray, dataShowInterface: DataShowInterface, flagMainSend: Boolean = false): Boolean {
        usbCommandsProtocol.flagWorkWrite = true

        // влаг успешности
        var flagSuc = true

        // переключение на четность PARITY_MARK и отправка
        context.usb.onSerialParity(3)
        Thread.sleep(500)
        if (!waitAndSand(byteArrayOf(address), false)) {
            if (!waitAndSand(byteArrayOf(address), false)) {
                usbCommandsProtocol.flagWorkWrite = false
                flagSuc = false
            }
        }

        // если прошлая отправка увенчалась успехом
        if (flagSuc) {
            context.usb.onSerialParity(4) // PARITY_SPACE
            Thread.sleep(500)
            if (!waitAndSand(data, true)) {
                if (!waitAndSand(data, true)) {
                    flagSuc = false
                }
            }
        }


        // отправка результатов для вывода во внешний поток
        showUI(context, flagMainSend, flagSuc, dataShowInterface)
        usbCommandsProtocol.flagWorkWrite = false

        return flagSuc
    }

    private fun showUI(context: MainActivity, flagMainSend: Boolean, flagSuc: Boolean, dataShowInterface: DataShowInterface) {
        context.runOnUiThread {
            if (context.curentDataByteAll.isNotEmpty() && context.curentDataByteAll[0] == ERROR_PASSWORD) {
                dataShowInterface.showData("error_password")
            } else {
                if (flagMainSend) {
                    dataShowInterface.showData(if (flagSuc) "yes" else "no")
                } else if (!flagSuc) {
                    dataShowInterface.showData("no")
                }
            }
        }
    }

    fun writeModBus(data: ByteArray, dataShowInterface: DataShowInterface, flagMainSend: Boolean = false): Boolean {
        usbCommandsProtocol.flagWorkWrite = true

        // флаг успешности
        var flagSuc = true

        // в связи с тем что в библиотеки subSerial не реализоаны методы по установки стоп битов и бит данных пришлось установить четность
        context.usb.onSerialParity(3)

        Thread.sleep(500)
        if (!waitAndSand(data)) {
            if (!waitAndSand(data)) {
                flagSuc = false
            }
        }

        // отправка результатов для вывода во внешний поток
        showUI(context, flagMainSend, flagSuc, dataShowInterface)

        usbCommandsProtocol.flagWorkWrite = false

        return flagSuc
    }

    // ожидание ответа
    @OptIn(ExperimentalStdlibApi::class)
    private fun waitAndSand(byteArraySand: ByteArray, chackSum: Boolean = true): Boolean {
        Log.d("usbData", "1")
        // очищение буфера
        context.curentDataByteAll = byteArrayOf()
        Log.d("usbData", "2")

        // отправка
        context.usb.writeDevice("", false, byteArraySand, chackSum)
        Log.d("usbData", "3")

        // ожидание ответа
        for(i in 0..TIME_WAIT_SWND_MAX) {
            if (context.curentDataByteAll.isNotEmpty()) {
                Log.d("usbData", context.curentDataByteAll.toHexString())
                return true
            }
            Thread.sleep(15)
        }
        Log.d("usbData", "4")

        if (context.curentDataByteAll.size == 1) return true
        Log.d("usbData", "5")

        try {
            // вычисленение контрольно суммы по всему пакету для того что бы проверить данные
            val checkSum = context.usb.checkSum(context.curentDataByteAll)
            Log.d("usbData", "6")

            if (context.curentDataByteAll[0] != byteArraySand[0] ||
                checkSum[0] != 0x00.toByte() || checkSum[1] != 0x00.toByte()) {
                return false
            }
        } catch (_: Exception) {
            return false
        }

        Log.d("usbData", "7")

        return false
    }
}