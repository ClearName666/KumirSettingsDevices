package com.example.kumirsettingupdevices.usb

import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Arrays
import kotlin.experimental.inv

class XModemSender(private val serialPort: UsbSerialDevice) {

    private var ackReceived = false
    private var nakReceived = false

    private val mCallback = UsbSerialInterface.UsbReadCallback { data ->
        if (data.isNotEmpty()) {
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
        serialPort.read(mCallback)
    }

    @Throws(IOException::class)
    fun sendFile(file: File?) {
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
                sendPacket(buffer, packetNumber)
                packetNumber++
            }
            sendEOT()
        } finally {
            Log.d(TAG, "Закрытие файла")
            fis.close()
        }
        Log.d(TAG, "Отправка файла завершена")
    }

    @Throws(IOException::class)
    private fun sendPacket(data: ByteArray, packetNumber: Int) {
        Log.d(TAG, "Отправка пакета номер $packetNumber")
        val packet = ByteArray(PACKET_SIZE + 5)
        packet[0] = SOH
        packet[1] = packetNumber.toByte()
        packet[2] = packetNumber.toByte().inv()
        System.arraycopy(data, 0, packet, 3, PACKET_SIZE)
        packet[PACKET_SIZE + 3] = calculateChecksum(data)
        Log.d(TAG, "Пакет сформирован: ${packet.joinToString { "%02x".format(it) }}")

        repeat(MAX_RETRIES) { attempt ->
            ackReceived = false
            nakReceived = false
            serialPort.write(packet)

            waitForAckOrNak()
            if (ackReceived) {
                Log.d(TAG, "Пакет номер $packetNumber успешно отправлен и подтвержден")
                return
            } else if (nakReceived) {
                Log.d(TAG, "Получен NAK для пакета номер $packetNumber, попытка повторной отправки $attempt")
            }
        }

        if (!ackReceived) {
            throw IOException("ACK not received for packet number $packetNumber after $MAX_RETRIES retries")
        }
    }

    @Throws(IOException::class)
    private fun sendEOT() {
        Log.d(TAG, "Отправка EOT")
        val eot = byteArrayOf(EOT)

        repeat(MAX_RETRIES) { attempt ->
            ackReceived = false
            nakReceived = false
            serialPort.write(eot)

            waitForAckOrNak()
            if (ackReceived) {
                Log.d(TAG, "EOT успешно отправлен и подтвержден")
                return
            } else if (nakReceived) {
                Log.d(TAG, "Получен NAK для EOT, попытка повторной отправки $attempt")
            }
        }

        if (!ackReceived) {
            throw IOException("ACK not received for EOT after $MAX_RETRIES retries")
        }
    }

    private fun waitForAckOrNak() {
        val startTime = System.currentTimeMillis()
        while (!ackReceived && !nakReceived && System.currentTimeMillis() - startTime < TIMEOUT) {
            // Ждем подтверждения ACK или NAK
        }
    }

    private fun calculateChecksum(data: ByteArray): Byte {
        var checksum = 0
        for (b in data) {
            checksum = (checksum + b) % 256
        }
        return checksum.toByte()
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






