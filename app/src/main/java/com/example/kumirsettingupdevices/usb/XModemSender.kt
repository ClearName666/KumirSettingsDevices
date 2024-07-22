package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.kumirsettingupdevices.LoadInterface
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Arrays
import kotlin.experimental.inv

class XModemSender(
    private val serialPort: UsbSerialDevice?,
    val context: MainActivity,
    val loadInterface: LoadInterface
) {

    private var ackReceived = false
    private var nakReceived = false

    private val mCallback = UsbSerialInterface.UsbReadCallback { data ->
        if (data.isNotEmpty()) {
            Log.d(TAG, data.joinToString { "%02x".format(it) })
            when (data[0]) {
                ACK -> {
                    ackReceived = true
                    Log.d(TAG, "ACK received")
                }
                NAK -> {
                    nakReceived = true
                    Log.d(TAG, "NAK received")
                }
                else -> {
                    Log.e(TAG, "Unexpected response: ${data[0]}")
                }
            }
        }
    }

    init {
        serialPort?.read(mCallback)
    }

    @Throws(IOException::class)
    fun sendFile(file: File?) {
        // Проверка на подключение устройства
        if (serialPort == null) {
            throw IOException("Serial port not connected")
        }

        // для прогресс бара
        val sizeFile = file?.length()
        val progressK = sizeFile?.div(128)?.div(100)
        var progress = 0

        Log.d(TAG, "Начало отправки файла")

        val fis = FileInputStream(file)
        val buffer = ByteArray(PACKET_SIZE)
        var bytesRead: Int
        var packetNumber = 1
        try {
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                Log.d(TAG, "Чтение пакета номер $packetNumber, байтов прочитано: $bytesRead")

                if (bytesRead < PACKET_SIZE) {
                    Arrays.fill(buffer, bytesRead, PACKET_SIZE, 0x1A.toByte()) // Заполнение EOF
                    Log.d(TAG, "Заполнение оставшегося буфера символом EOF")
                }
                if (!context.usb.checkConnectToDevice())
                    throw IOException("Serial port not connected")
                sendPacket(buffer, packetNumber)
                packetNumber++

                // вывод прогресса на экран
                if (progressK != null && packetNumber % progressK.toInt() == 0)
                    (context as Activity).runOnUiThread {
                        progress++
                        loadInterface.loadingProgress(progress)
                    }
            }
            sendEOT()

            // прогресс на 100%
            (context as Activity).runOnUiThread {
                loadInterface.loadingProgress(100)
                if (context.usb.checkConnectToDevice())
                    loadInterface.closeMenuProgress()
            }

        } catch (e: IOException) {
            Log.e(TAG, "Ошибка при отправке файла: ${e.message}")
            (context as Activity).runOnUiThread {
                loadInterface.errorSend()
            }
            throw e
        } finally {
            Log.d(TAG, "Закрытие файла")

            // закрываем меню загрузки
            (context as Activity).runOnUiThread {
                loadInterface.errorCloseMenuProgress()
            }

            fis.close()
        }
    }

    @Throws(IOException::class)
    private fun sendPacket(data: ByteArray, packetNumber: Int) {
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
                throw IOException("Serial port disconnected during packet sending")
            }

            ackReceived = false
            nakReceived = false
            if (!context.usb.checkConnectToDevice())
                throw IOException("Serial port not connected")
            serialPort.write(packet)

            waitForAckOrNak()
            if (ackReceived) {
                Log.d(TAG, "Пакет номер $packetNumber успешно отправлен и подтвержден")
                return
            } else if (nakReceived) {
                Log.d(TAG, "Получен NAK для пакета номер $packetNumber, попытка повторной отправки $attempt")
            }
        }

        (context as Activity).runOnUiThread {
            loadInterface.errorSend()
        }

        throw IOException("ACK not received for packet number $packetNumber after $MAX_RETRIES retries")
    }

    @Throws(IOException::class)
    private fun sendEOT() {
        Log.d(TAG, "Отправка EOT")
        val eot = byteArrayOf(EOT)

        repeat(MAX_RETRIES) { attempt ->
            if (serialPort == null) {
                throw IOException("Serial port disconnected during EOT sending")
            }

            ackReceived = false
            nakReceived = false
            if (!context.usb.checkConnectToDevice())
                throw IOException("Serial port not connected")
            serialPort.write(eot)

            waitForAckOrNak()
            if (ackReceived) {
                Log.d(TAG, "EOT успешно отправлен и подтвержден")
                return
            } else if (nakReceived) {
                Log.d(TAG, "Получен NAK для EOT, попытка повторной отправки $attempt")
            }
        }

        (context as Activity).runOnUiThread {
            loadInterface.errorSend()
        }

        throw IOException("ACK not received for EOT after $MAX_RETRIES retries")
    }

    @Throws(IOException::class)
    private fun waitForAckOrNak() {
        val startTime = System.currentTimeMillis()
        while (!ackReceived && !nakReceived && System.currentTimeMillis() - startTime < TIMEOUT) {
            if (!context.usb.checkConnectToDevice())
                throw IOException("Serial port not connected")
        }
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
        private const val MAX_RETRIES = 5 // Максимальное количество повторных попыток отправки
    }
}
