package com.example.kumirsettingupdevices.usb

import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Arrays
import kotlin.experimental.inv


class XModemSender(port: UsbSerialPort) {
    private val port: UsbSerialPort

    init {
        this.port = port
    }

    @Throws(IOException::class)
    fun sendFile(file: File?) {
        val fis = FileInputStream(file)
        val buffer = ByteArray(PACKET_SIZE)
        var bytesRead: Int
        var packetNumber = 1
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            if (bytesRead < PACKET_SIZE) {
                Arrays.fill(buffer, bytesRead, PACKET_SIZE, 0x1A.toByte()) // Заполнение EOF
            }
            sendPacket(buffer, packetNumber)
            packetNumber++
        }
        sendEOT()
        fis.close()
    }

    @Throws(IOException::class)
    private fun sendPacket(data: ByteArray, packetNumber: Int) {
        val packet = ByteArray(PACKET_SIZE + 5)
        packet[0] = SOH
        packet[1] = packetNumber.toByte()
        packet[2] = packetNumber.toByte().inv()
        System.arraycopy(data, 0, packet, 3, PACKET_SIZE)
        packet[PACKET_SIZE + 3] = calculateChecksum(data)
        port.write(packet, 1000)

        // Ожидание подтверждения
        val response = ByteArray(1)
        port.read(response, 1000)
        if (response[0] != ACK) {
            throw IOException("ACK not received")
        }
    }

    @Throws(IOException::class)
    private fun sendEOT() {
        val eot = byteArrayOf(EOT)
        port.write(eot, 1000)

        // Ожидание подтверждения
        val response = ByteArray(1)
        port.read(response, 1000)
        if (response[0] != ACK) {
            throw IOException("ACK not received for EOT")
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
        private const val PACKET_SIZE = 128
        private const val SOH: Byte = 0x01
        private const val EOT: Byte = 0x04
        private const val ACK: Byte = 0x06
        private const val NAK: Byte = 0x15
    }
}