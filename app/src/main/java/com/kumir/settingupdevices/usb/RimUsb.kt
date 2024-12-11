package com.kumir.settingupdevices.usb

import com.kumir.settingupdevices.DataShowInterface
import com.kumir.settingupdevices.MainActivity

class RimUsb(val context: MainActivity, val usbCommandsProtocol: UsbCommandsProtocol) {

    companion object {
        const val TIME_WAIT_SWND_MAX: Int = 200
        const val ERROR_PASSWORD: Byte = 0x94.toByte()
        const val ERROR_SETTINGS: Byte = 0x83.toByte()
    }


    // отправка данных пл протоколу rs485
    fun writeRS485(address: Byte, data: ByteArray, dataShowInterface: DataShowInterface, flagMainSend: Boolean = false, flagShow: Boolean = true): Boolean {
        usbCommandsProtocol.flagWorkWrite = true

        // влаг успешности
        var flagSuc = true

        // переключение на четность PARITY_MARK и отправка
        context.usb.onSerialParity(3)
        Thread.sleep(500)
        if (!waitAndSand(byteArrayOf(address), false)) {
            if (!waitAndSand(byteArrayOf(address), false)) {
                flagSuc = false
            }
        }

        // если прошлая отправка увенчалась успехом
        if (flagSuc) {
            context.usb.onSerialParity(4) // PARITY_SPACE
            Thread.sleep(500)
            if (!waitAndSand(data, true)) {
                if (!waitAndSand(data, true)) {
                    if (!waitAndSand(data)) {
                        flagSuc = false
                    }
                }
            }
        }


        // отправка результатов для вывода во внешний поток
        if (flagShow) showUI(context, flagMainSend, flagSuc, dataShowInterface)
        usbCommandsProtocol.flagWorkWrite = false

        return flagSuc
    }

    private fun showUI(context: MainActivity, flagMainSend: Boolean, flagSuc: Boolean, dataShowInterface: DataShowInterface, shift: Int = 0) {

        // shift это смещение в данных если протокол modbus то оно должно быть 1 потому что 1 байт занимает адресс

        val data = context.currentDataByteAll

        if (data.isNotEmpty() && data[0 + shift] == ERROR_PASSWORD) {
            context.runOnUiThread {
                dataShowInterface.showData("error_password", data)
            }
        } else if (data.isNotEmpty() && data[0 + shift] == ERROR_SETTINGS) {
            context.runOnUiThread {
                dataShowInterface.showData("error_settings", data)
            }
        } else {
            if (data.isNotEmpty() && data[shift] == 0x00.toByte()) {
                context.runOnUiThread {
                    dataShowInterface.showData("version_programming", data)
                }
            } else if (data.isNotEmpty() && data[shift] == 0x01.toByte()) {
                context.runOnUiThread {
                    dataShowInterface.showData("serial_number", data)
                }
            } else if (flagMainSend) {
                context.runOnUiThread {
                    dataShowInterface.showData(if (flagSuc) "yes" else "no", data)
                }
            } else if (!flagSuc) {
                context.runOnUiThread {
                    dataShowInterface.showData("no", data)
                }
            }
        }
    }

    fun writeModBus(data: ByteArray, dataShowInterface: DataShowInterface, flagMainSend: Boolean = false, flagShow: Boolean = true): Boolean {
        usbCommandsProtocol.flagWorkWrite = true

        // флаг успешности
        var flagSuc = true

        // в связи с тем что в библиотеки subSerial не реализоаны методы по установки стоп битов и бит данных пришлось установить четность
        context.usb.onSerialParity(3)

        Thread.sleep(500)
        if (!waitAndSand(data)) {
            if (!waitAndSand(data)) {
                if (!waitAndSand(data)) {
                    flagSuc = false
                }
            }
        }

        // отправка результатов для вывода во внешний поток
        if (flagShow) showUI(context, flagMainSend, flagSuc, dataShowInterface, 1)

        usbCommandsProtocol.flagWorkWrite = false

        return flagSuc
    }

    // ожидание ответа
    private fun waitAndSand(byteArraySand: ByteArray, chackSum: Boolean = true): Boolean {
        // очищение буфера
        context.currentDataByteAll = byteArrayOf()

        // отправка
        if (!context.usb.checkConnectToDevice()) return false
        context.usb.writeDevice("", false, byteArraySand, chackSum)

        // ожидание ответа
        for(i in 0..TIME_WAIT_SWND_MAX) {
            if (context.currentDataByteAll.isNotEmpty()) {
                return true
            }
            Thread.sleep(4)
        }

        if (context.currentDataByteAll.size == 1) return true

        try {
            // вычисленение контрольно суммы по всему пакету для того что бы проверить данные
            val checkSum = context.usb.checkSum(context.currentDataByteAll)

            if (context.currentDataByteAll[0] != byteArraySand[0] ||
                checkSum[0] != 0x00.toByte() || checkSum[1] != 0x00.toByte()) {
                return false
            }
        } catch (_: Exception) {
            return false
        }

        return false
    }
}