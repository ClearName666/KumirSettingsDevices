package com.kumir.settingupdevices.usb

import android.util.Log
import com.kumir.settingupdevices.LoadInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.usbFragments.FirmwareSTMFragment
import java.io.File
import kotlin.experimental.xor

enum class Stm {
    STM103, STM205, STM303, STM433, NULL
}

class StmLoader(private val usbCommandsProtocol: UsbCommandsProtocol, private val contextMain: MainActivity) {


    // флаг уведомления о том что устройство перезкгружено
    var flagResetOk: Boolean = false

    companion object {
        const val ACK: Byte = 0x79.toByte()
        const val NACK: Byte = 0x1F.toByte()
        private const val TIMEOUT = 3000L // Timeout для ожидания подтверждения в миллисекундах
        private const val TIMEOUT_STEP_CHECK_RESET = 50L

        val ID1_STM_BYTES_STM103: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x10.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )
        val ID2_STM_BYTES_STM103: ByteArray = byteArrayOf(
            0x79.toByte(),
            0x14.toByte(),
            0x04.toByte(),
            0x01.toByte(),
            0x79.toByte()
        )
        val ID3_STM_BYTES_STM103: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x18.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )
        val ID4_STM_BYTES_STM103: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x30.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )



        val ID1_STM_BYTES_STM205: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x11.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )
        val ID2_STM_BYTES_STM205: ByteArray = byteArrayOf(
            0x2B.toByte(),
            0xA0.toByte(),
            0x14.toByte(),
            0x77.toByte(),
            0x00.toByte(),
        )



        val ID1_STM_BYTES_STM303: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x22.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )
        val ID2_STM_BYTES_STM303: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x38.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )
        val ID3_STM_BYTES_STM303: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x46.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )


        val ID1_STM_BYTES_STM433: ByteArray = byteArrayOf(
            0x04.toByte(),
            0x35.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )
    }


    // записьь прошивки
    @OptIn(ExperimentalStdlibApi::class)
    fun loadFile(fileBootLoader: File, fileProgram: File, addressBootLoader: Int, addressProgram: Int, sizeBootLoader: Int, sizeProgram: Int, loadInterface: LoadInterface,
                 flag203: Boolean = false,  firmwareSTMFragment: FirmwareSTMFragment? = null): Boolean {
        usbCommandsProtocol.flagWorkWrite = true

        Log.d("loadFileStm", "Инициализация")
        if (!init()) {
            contextMain.runOnUiThread {
                loadInterface.errorSend()
            }
            usbCommandsProtocol.flagWorkWrite = false
            return false
        }
        Log.d("loadFileStm", "Разблокировка чтения")
        if (!unblockRead()) {
            contextMain.runOnUiThread {
                loadInterface.errorSend()
            }
            usbCommandsProtocol.flagWorkWrite = false
            return false
        }

        // ожидание перезагрузки в случае если контроллер stm203
        if (flag203) {
            contextMain.runOnUiThread {
                firmwareSTMFragment?.needResetPlease() // требование о том что нужно перезкгрузить модем
            }

            // надо добавить проверку в случае чего -----------------------------------------------------------------------
            while (!flagResetOk) {
                Thread.sleep(TIMEOUT_STEP_CHECK_RESET)
            }
            flagResetOk = false
        }

        Log.d("loadFileStm", "Разблокировка записи")
        if (!unblockWrite()) {
            contextMain.runOnUiThread {
                loadInterface.errorSend()
            }
            usbCommandsProtocol.flagWorkWrite = false
            return false
        }

        // ожидание перезагрузки в случае если контроллер stm203
        if (flag203) {
            contextMain.runOnUiThread {
                firmwareSTMFragment?.needResetPlease() // требование о том что нужно перезкгрузить модем
            }

            // надо добавить проверку в случае чего -----------------------------------------------------------------------
            while (!flagResetOk) {
                Thread.sleep(TIMEOUT_STEP_CHECK_RESET)
            }
            flagResetOk = false
        }

        // чтение версии bootloader
        // Log.d("loadFileStm", "Получение версии прошивки BootLoader")
        // val versionBootLoader = readBootLoader()
        // Log.d("loadFileStm", "Версия прошивки BootLoader: ${versionBootLoader.toHexString()}")

        Log.d("loadFileStm", "Получение flash")
        // проверка влезит ли бут лоадер
        val flash = 100000 // readFlesh(readPid())--------------------------------------------------иправить
        Log.d("loadFileStm", "Получен flash: $flash")

        Log.d("loadFileStm", "Проверяем что прошивка влезит в flash")
        if (flash > sizeBootLoader) {
            Log.d("loadFileStm", "Отчистка контроллера")
            if (!clearFlash()) {
                contextMain.runOnUiThread {
                    loadInterface.errorSend()
                }
                usbCommandsProtocol.flagWorkWrite = false
                return false
            }
            Log.d("loadFileStm", "Отчистка завершина")


            Log.d("loadFileStm", "Отправка bootloader")
            if (!writeFileToController(fileBootLoader, addressBootLoader, loadInterface)) {
                contextMain.runOnUiThread {
                    loadInterface.errorSend()
                }
                usbCommandsProtocol.flagWorkWrite = false
                return false
            }
            Log.d("loadFileStm", "Отправка bootloader завершена")

            // если прошивка влезает то зашиваем ее
            if (flash - sizeBootLoader > sizeProgram) {
                Log.d("loadFileStm", "Отправка program")
                if (!writeFileToController(fileProgram, addressProgram, loadInterface)) {
                    contextMain.runOnUiThread {
                        loadInterface.errorSend()
                    }
                    usbCommandsProtocol.flagWorkWrite = false
                    return false
                }
                Log.d("loadFileStm", "Отправка program завершена")

            }
        }


        // отправляем сигнал что все успешно
        contextMain.runOnUiThread {
            loadInterface.closeMenuProgress()
        }

        usbCommandsProtocol.flagWorkWrite = false

        return true
    }

    // Функция для чтения файла
    private fun readFileToBytes(file: File): ByteArray {
        return file.readBytes()
    }

    private fun writeFileToController(file: File, startAddress: Int, loadInterface: LoadInterface): Boolean {
        // Читаем файл в байты
        val fileBytes = readFileToBytes(file)

        // Начинаем запись с указанного стартового адреса
        var currentAddress = startAddress
        val blockSize = 256

        // счетчик для контроля прогресса
        val totalBlocks = (fileBytes.size + blockSize - 1) / blockSize  // Correct block count
        val progressStep: Double = 100.0 / totalBlocks
        var progress = 0.0

        // Разбиваем файл на блоки по 256 байт
        for (i in 0..fileBytes.size step blockSize) {
        //for (i in fileBytes.indices step blockSize) {
            // Берём блок данных, если блок меньше 256 байт - дополняем его
            val block = fileBytes.copyOfRange(i, (i + blockSize).coerceAtMost(fileBytes.size))

            // Дополняем блок до 256 байт (если блок меньше)
            val paddedBlock = ByteArray(blockSize) { 0xFF.toByte() }
            block.copyInto(paddedBlock)

            // Шаги записи блока
            if (!writeBlockToController(currentAddress, paddedBlock)) {
                return false
            }

            // Увеличиваем адрес для следующего блока
            currentAddress += blockSize

            // вывод прогресса на экран
            progress += progressStep
            contextMain.runOnUiThread {
                loadInterface.loadingProgress(progress.toInt())
                Log.d("prograssBar", progress.toString())
            }
        }
        contextMain.currentDataByteAll = byteArrayOf()

        return true
    }

    private fun writeBlockToController(address: Int, dataBlock: ByteArray): Boolean {

        if (!contextMain.usb.checkConnectToDevice()) return false

        // Шаг 1: Отправляем команду записи
        val writeCommand = byteArrayOf(0x31.toByte(), 0xCE.toByte())
        sendPack(writeCommand)

        // Шаг 2: Отправляем адрес в обратном порядке
        val addressBytes = byteArrayOf(
            (address shr 24).toByte(),
            (address shr 16).toByte(),
            (address shr 8).toByte(),
            address.toByte()
        )
        val addressXor = calculateXor(addressBytes)
        sendPack(addressBytes + byteArrayOf(addressXor))

        // Шаг 3: Отправляем блок данных
        val length = (dataBlock.size - 1).toByte() // Длина блока (на 1 меньше)
        val dataXor = calculateXor(byteArrayOf(length) + dataBlock)
        sendPack(byteArrayOf(length) + dataBlock + byteArrayOf(dataXor))

        // Шаг 4: Проверка записанных данных (опционально)
        return true // verifyData(addressBytes, dataBlock)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sendPack(packet: ByteArray): ByteArray {
        contextMain.currentDataByteAll = byteArrayOf()
        contextMain.usb.writeDevice(
            "",
            false,
            packet,
            false
        )

        if (packet.size < 6) {
            Log.d("loadFileStm", "send: ${packet.toHexString()} [${packet.size}]")
        }
        else {
            Log.d("loadFileStm", "send: [${packet.size}]")
        }



        if (waitForResponce()) {
            return contextMain.currentDataByteAll
        }

        return byteArrayOf()
    }

    private fun verifyData(address: ByteArray, originalData: ByteArray): Boolean {
        val readCommand = byteArrayOf(0x11.toByte(), 0xEE.toByte()) + address + byteArrayOf(calculateXor(address))
        val response = sendPack(readCommand)

        // Проверяем, что ответ совпадает с исходными данными
        return response.copyOfRange(1, originalData.size + 1).contentEquals(originalData)
    }

    // Функция для расчёта XOR всех байтов
    private fun calculateXor(data: ByteArray): Byte {
        var result: Byte = 0
        for (b in data) {
            result = result xor b
        }
        return result
    }





    // ждем данные в ответ
    @OptIn(ExperimentalStdlibApi::class)
    private fun waitForResponce(slowly: Long = 1): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < TIMEOUT) {

            if (!contextMain.usb.checkConnectToDevice(reconnect = false))
                return false

            // проверка буфера ответа
            if (contextMain.currentDataByteAll.isNotEmpty()) {
                Thread.sleep(10 * slowly)
                Log.d("loadFileStm", contextMain.currentDataByteAll.toHexString())
                return true
            }

        }

        return false
    }

    // инициализация
    private fun init(): Boolean {
        contextMain.currentDataByteAll = byteArrayOf()

        // отправка байта для проверки готовности устройства
        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x7F.toByte()),
            false
        )

        // ждем ответ и смотрим что там
        if (waitForResponce()) {

            return if (contextMain.currentDataByteAll[0] == ACK) {
                true
            } else if (contextMain.currentDataByteAll[0] == NACK) {
                true
            } else {
                false
            }
        }
        return false
    }


    // чтениее pid
    private fun readPid(): Stm {
        contextMain.currentDataByteAll = byteArrayOf()

        // отправка байта для проверки pid
        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x02.toByte(), 0xFD.toByte()),
            false
        )

        if (waitForResponce() && contextMain.currentDataByteAll.size == 5) {
            usbCommandsProtocol.flagWorkWrite = false
            // определяем какой stm
            val dataSand = contextMain.currentDataByteAll.reversedArray()
            return when {
                dataSand.contentEquals(ID1_STM_BYTES_STM103) -> Stm.STM103
                dataSand.contentEquals(ID2_STM_BYTES_STM103) -> Stm.STM103
                dataSand.contentEquals(ID3_STM_BYTES_STM103) -> Stm.STM103
                dataSand.contentEquals(ID4_STM_BYTES_STM103) -> Stm.STM103

                dataSand.contentEquals(ID1_STM_BYTES_STM205) -> Stm.STM205
                dataSand.contentEquals(ID2_STM_BYTES_STM205) -> Stm.STM205

                dataSand.contentEquals(ID1_STM_BYTES_STM303) -> Stm.STM303
                dataSand.contentEquals(ID2_STM_BYTES_STM303) -> Stm.STM303
                dataSand.contentEquals(ID3_STM_BYTES_STM303) -> Stm.STM303

                dataSand.contentEquals(ID1_STM_BYTES_STM433) -> Stm.STM433

                else -> Stm.NULL
            }
        }

        return Stm.NULL

    }


    // Разблокирование чтения
    private fun unblockRead(): Boolean {

        // несколько попыток разблокировать
        for (i in 0..3) {
            contextMain.currentDataByteAll = byteArrayOf()

            contextMain.usb.writeDevice(
                "",
                false,
                byteArrayOf(0x92.toByte(), 0x6D.toByte()),
                false
            )

            if (waitForResponce() && (contextMain.currentDataByteAll[0] == ACK || contextMain.currentDataByteAll[0] == NACK)) {
                Thread.sleep(500)
                return init()
            }


        }
        return false
    }


    // Разблокирование записи
    private fun unblockWrite(): Boolean {

        // несколько попыток разблокировать
        for (i in 0..3) {
            contextMain.currentDataByteAll = byteArrayOf()

            contextMain.usb.writeDevice(
                "",
                false,
                byteArrayOf(0x73.toByte(), 0x8C.toByte()),
                false
            )

            if (waitForResponce() && contextMain.currentDataByteAll[0] == ACK) {
                Thread.sleep(500)
                return init()
            }
        }
        return false
    }


    // чтение версии bootloader
    private fun readBootLoader(): ByteArray {

        // несколько попыток разблокировать
        for (i in 0..3) {
            contextMain.currentDataByteAll = byteArrayOf()

            contextMain.usb.writeDevice(
                "",
                false,
                byteArrayOf(0x00.toByte(), 0xFF.toByte()),
                false
            )

            if (waitForResponce() && contextMain.currentDataByteAll.size > 3)
                return contextMain.currentDataByteAll.drop(3).toByteArray()
        }
        return byteArrayOf()
    }


    // чтения размера flash памяти
    private fun readFlesh(stm: Stm): Int {
        contextMain.currentDataByteAll = byteArrayOf()

        // отправляем команду для чтения памяти
        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x11.toByte(), 0xEE.toByte()),
            false
        )

        if (!(waitForResponce() && contextMain.currentDataByteAll[0] == ACK))
            return 0

        val flashAddress: ByteArray = if (stm == Stm.STM103) {
            byteArrayOf(0x1F.toByte(), 0xFF.toByte(), 0xF7.toByte(), 0xE0.toByte())
        } else if (stm == Stm.STM205) {
            byteArrayOf(0x1F.toByte(), 0xFF.toByte(), 0x7A.toByte(), 0x22.toByte())
        } else if (stm == Stm.STM303) {
            byteArrayOf(0x1F.toByte(), 0xFF.toByte(), 0xF7.toByte(), 0xCC.toByte())
        } else if (stm == Stm.STM433) {
            byteArrayOf(0x1F.toByte(), 0xFF.toByte(), 0x75.toByte(), 0xE0.toByte())
        } else {
            return 0
        }

        // отправляем запрос на получения flash памяти
        contextMain.usb.writeDevice(
            "",
            false,
            flashAddress.reversedArray(),
            false
        )

        // ожидаем ответ
        if (!(waitForResponce() && contextMain.currentDataByteAll[0] == ACK))
            return 0

        // отправляем сколько байт мне нужно считать а так же кс по всем отправленным данным
        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x02.toByte()) + calculateXor(flashAddress),
            false
        )

        if (waitForResponce(5) && contextMain.currentDataByteAll.size == 3) {
            // contextMain.currentDataByteAll.drop(1).toByteArray()
            // преобразую 2 последних байта в число и вызвращяю
            var flashInt: Int = contextMain.currentDataByteAll[2].toInt()
            flashInt = (flashInt shl 8) or contextMain.currentDataByteAll[1].toInt()
            return flashInt
        }


        return 0
    }

    // глобальная отчистка
    private fun clearFlash(): Boolean {

        // несколько попыток разблокировать
        for (i in 0..3) {
            contextMain.currentDataByteAll = byteArrayOf()

            contextMain.usb.writeDevice(
                "",
                false,
                byteArrayOf(0x43.toByte(), 0xBC.toByte()),
                false
            )

            if (waitForResponce() && contextMain.currentDataByteAll[0] == ACK) {
                contextMain.currentDataByteAll = byteArrayOf()

                contextMain.usb.writeDevice(
                    "",
                    false,
                    byteArrayOf(0xFF.toByte(), 0x00.toByte()),
                    false
                )

                if (waitForResponce() && contextMain.currentDataByteAll[0] == ACK) {
                    return true
                }
            }

        }
        return false
    }

}