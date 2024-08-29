package com.kumir.settingupdevices.usb

import android.app.Activity
import android.util.Log
import com.kumir.settingupdevices.LoadInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.felhr.usbserial.UsbSerialDevice
import java.io.File
import java.io.FileInputStream
import java.util.Arrays
import kotlin.experimental.inv

class XModemSender(
    private val serialPort: UsbSerialDevice?,
    val context: MainActivity,
    private val loadInterface: LoadInterface,
    private val nameDriver: String
) {

    private var ackReceived = false
    private var nakReceived = false


    // обна попытка переподключения для встановление
    var flagReconnect: Boolean = true
    var flagSendDataRestart: Boolean = false
    var flagReconnectionCompleted: Boolean = false

    fun sendFile(file: File?): Boolean {

        // Проверка на подключение устройства
        if (serialPort == null) {
            (context as Activity).runOnUiThread {
                loadInterface.errorSend()
            }
            return false
        }

        // для прогресс бара
        val sizeFile = file?.length()
        val progressK = sizeFile?.div(128)?.div(100)
        var progress = 0

        //Log.d(TAG, "Начало отправки файла")

        val fis = FileInputStream(file)
        val buffer = ByteArray(PACKET_SIZE)
        var bytesRead: Int
        var packetNumber = 1


        while (fis.read(buffer).also { bytesRead = it } != -1) {
            //Log.d(TAG, "Чтение пакета номер $packetNumber, байтов прочитано: $bytesRead")

            if (bytesRead < PACKET_SIZE) {
                Arrays.fill(buffer, bytesRead, PACKET_SIZE, 0x1A.toByte()) // Заполнение EOF
                //Log.d(TAG, "Заполнение оставшегося буфера символом EOF")
            }
            if (!context.usb.checkConnectToDevice(reconnect = false)) {
                (context as Activity).runOnUiThread {
                    loadInterface.errorSend()
                }
                fis.close()
                return false
            }

            // если в отправке ошибка то
            if (!sendPacket(buffer, packetNumber)) {

                // проверяяем можем ли мы переподключиться для то го что бы заного начать загружать драйвер
                if (flagSendDataRestart && flagReconnectionCompleted) {
                    fis.close()
                    return sendFile(file)
                } else { // мы не можем тогда выходим с ошибкой
                    (context as Activity).runOnUiThread {
                        loadInterface.errorSend()
                    }
                    fis.close()
                    return false
                }

            }
            packetNumber++

            // вывод прогресса на экран
            if (progressK != null && packetNumber % progressK.toInt() == 0) {
                (context as Activity).runOnUiThread {
                    progress++
                    loadInterface.loadingProgress(progress)
                }
            }

        }

        // если EOT не отправлен то вылитаем с ошибкой
        if (!sendEOT()) {
            (context as Activity).runOnUiThread {
                loadInterface.errorSend()
            }
            fis.close()
            return false
        }

        // прогресс на 100%
        (context as Activity).runOnUiThread {
            loadInterface.loadingProgress(100)
            loadInterface.closeMenuProgress()
        }

        fis.close()
        return true
    }


    private fun sendPacket(data: ByteArray, packetNumber: Int): Boolean {
        Log.d(TAG, "Отправка пакета номер $packetNumber")
        val packet = ByteArray(PACKET_SIZE + 4)
        packet[0] = SOH
        packet[1] = packetNumber.toByte()
        packet[2] = packetNumber.toByte().inv()
        System.arraycopy(data, 0, packet, 3, PACKET_SIZE)
        packet[PACKET_SIZE + 3] = calculateChecksum(data)
        Log.d(TAG, "Пакет сформирован: ${packet.joinToString { "%02x".format(it) }}")

        repeat(MAX_RETRIES) { attempt ->
            if (serialPort == null) {
                return false
            }

            ackReceived = false
            nakReceived = false
            if (!context.usb.checkConnectToDevice(reconnect = false))
                return false

            context.currentDataByteAll = byteArrayOf()
            serialPort.write(packet)
            //context.usb.writeDevice("", false, packet, false)

            // проверка получин ли ответ
            if (!waitForAckOrNak()) {
                return false
            }


            if (ackReceived) {
                Log.d(TAG, "Пакет номер $packetNumber успешно отправлен и подтвержден")
                return true
            } else if (nakReceived) {
                Log.d(TAG, "Получен NAK для пакета номер $packetNumber, попытка повторной отправки $attempt")
            }
        }
        return false
    }

    private fun sendEOT(): Boolean {
        Log.d(TAG, "Отправка EOT")
        val eot = byteArrayOf(EOT)

        repeat(MAX_RETRIES) { attempt ->
            if (serialPort == null) {
                return false
            }

            ackReceived = false
            nakReceived = false
            if (!context.usb.checkConnectToDevice(reconnect = false))
                return false

            context.currentDataByteAll = byteArrayOf()
            serialPort.write(eot)

            // задержка потому что он долго думает
            Thread.sleep(TIMEOUT_EOT)
            //context.usb.writeDevice("", false, eot, false)

            // проверка поступил ти ответ если нет то выходим с флагом false
            if (!waitForAckOrNak()) {
                (context as Activity).runOnUiThread {
                    loadInterface.errorSend()
                }
                return false
            }
            if (ackReceived) {
                Log.d(TAG, "EOT успешно отправлен и подтвержден")
                return true
            } else if (nakReceived) {
                Log.d(TAG, "Получен NAK для EOT, попытка повторной отправки $attempt")
            }
        }

        return false
    }




    // метод для того что бы мадем п101 перезагрузился а то он лагает
    private fun reconnectSendToXModemP101(): Boolean {

        // очищение
        context.usb.onClear()
        // переподключение
        context.usb.reconnectCDC()

        // ждем возможное подключение
        if (context.usb.waitConnection()) {
            context.currentDataByteAll = byteArrayOf()

            // отправка прибору команду длдя того что бы войти в режим загрузки драйверов
            context.usb.writeDevice(context.getString(R.string.commandSetDriverMode) +
                nameDriver,
                false
            )
            Thread.sleep(100)
            if (context.currentDataByteAll.isEmpty()) {
                return true
            }
        }
        return false

    }

    // ждем данные в ответ
    private fun waitForAckOrNak(): Boolean {
        val startTime = System.currentTimeMillis()
        while (!ackReceived && !nakReceived && System.currentTimeMillis() - startTime < TIMEOUT) {

            // проверка буфера ответа
            if (context.currentDataByteAll.isNotEmpty()) {
                when (context.currentDataByteAll[0]) {
                    ACK -> {
                        ackReceived = true
                        Log.d(TAG, "ACK received")
                    }
                    NAK -> {
                        nakReceived = true
                        Log.d(TAG, "NAK received")
                    }
                    else -> {
                        return false
                    }
                }
            }
            if (!context.usb.checkConnectToDevice(reconnect = false))
                return false
        }

        // если нечсего не ответил то пробуем перезапустить потому что это странно
        if (flagReconnect && !ackReceived && !nakReceived) {

            flagReconnect = false
            flagSendDataRestart = true

            flagReconnectionCompleted = reconnectSendToXModemP101()
            return false
        }

        return true
    }

    private fun calculateChecksum(data: ByteArray): Byte {
        var crc: Byte = 0

        for (element in data) {
            crc = (crc + element).toByte()
        }

        return crc
    }

    companion object {
        private const val TAG = "XModemSender"
        private const val PACKET_SIZE = 128
        private const val SOH: Byte = 0x01
        private const val EOT: Byte = 0x04
        private const val ACK: Byte = 0x06
        private const val NAK: Byte = 0x15
        private const val TIMEOUT = 1000L // Timeout для ожидания подтверждения в миллисекундах
        private const val TIMEOUT_EOT = 2000L
        private const val MAX_RETRIES = 5 // Максимальное количество повторных попыток отправки
    }
}
