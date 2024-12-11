package com.kumir.settingupdevices.usb

import android.util.Log
import com.kumir.settingupdevices.LoadInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.usbFragments.FirmwareSTMFragment
import java.io.File
import java.util.Arrays
import kotlin.experimental.xor

enum class Stm {
    STM103, STM205, STM303, STM433, NULL
}

class StmLoader(private val usbCommandsProtocol: UsbCommandsProtocol, private val contextMain: MainActivity) {


    // флаг уведомления о том что устройство перезкгружено
    var flagResetOk: Boolean = false

    lateinit var stmFragment: FirmwareSTMFragment

    companion object {
        const val ACK: Byte = 0x79.toByte()
        const val NACK: Byte = 0x1F.toByte()
        private const val TIMEOUT = 3000L // Timeout для ожидания подтверждения в миллисекундах
        private const val TIMEOUT_STEP_CHECK_RESET = 50L
        private const val TIMEOUT_STEP_CLEAR_MEMORY = 20000L

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
            0x79.toByte(),
            0x30.toByte(),
            0x04.toByte(),
            0x01.toByte(),
            0x79.toByte(),
        )



        val ID1_STM_BYTES_STM205: ByteArray = byteArrayOf(
            0x79.toByte(),
            0x11.toByte(),
            0x04.toByte(),
            0x01.toByte(),
            0x79.toByte(),
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
                 flag205: Boolean = false, firmwareSTMFragment: FirmwareSTMFragment): Boolean {
        usbCommandsProtocol.flagWorkWrite = true
        val stm: Stm

        stmFragment = firmwareSTMFragment

        //  вывод данных об этапе прошивки
        contextMain.runOnUiThread {
            firmwareSTMFragment.currentTaskFlash("Инициализация")
        }


        Log.d("loadFileStm", "Инициализация")
        if (!init()) { // доп проверка для проверки на отмену
            contextMain.runOnUiThread {
                loadInterface.errorSend()
            }
            usbCommandsProtocol.flagWorkWrite = false
            return false
        } else {

            //  вывод данных об этапе прошивки
            contextMain.runOnUiThread {
                firmwareSTMFragment.currentTaskFlash("Получение PID")
            }

            stm = readPid()
        }



        //  вывод данных об этапе прошивки
        contextMain.runOnUiThread {
            firmwareSTMFragment.currentTaskFlash("Разблокировака чтения")
        }

        Log.d("loadFileStm", "Разблокировака чтения")
        if (!unblockRead(stm)) {
            contextMain.runOnUiThread {
                loadInterface.errorSend()
            }
            usbCommandsProtocol.flagWorkWrite = false
            return false
        }

        //  вывод данных об этапе прошивки
        contextMain.runOnUiThread {
            firmwareSTMFragment.currentTaskFlash("Ожидание физической перезагрузки")
        }

        // ожидание перезагрузки в случае если контроллер stm205
        if (flag205) {
            contextMain.runOnUiThread {
                firmwareSTMFragment.needResetPlease() // требование о том что нужно перезкгрузить модем
            }

            // надо добавить проверку в случае чего -----------------------------------------------------------------------
            while (!flagResetOk) {
                Thread.sleep(TIMEOUT_STEP_CHECK_RESET)
            }
            flagResetOk = false

            // инициализация
            if (!init()) {
                contextMain.runOnUiThread {
                    loadInterface.errorSend()
                }
                usbCommandsProtocol.flagWorkWrite = false
                return false
            }
        }

        //  вывод данных об этапе прошивки
        contextMain.runOnUiThread {
            firmwareSTMFragment.currentTaskFlash("Разблокировака записи")
        }

        Log.d("loadFileStm", "Разблокировака записи")
        if (!unblockWrite(stm)) {
            contextMain.runOnUiThread {
                loadInterface.errorSend()
            }
            usbCommandsProtocol.flagWorkWrite = false
            return false
        }

        //  вывод данных об этапе прошивки
        contextMain.runOnUiThread {
            firmwareSTMFragment.currentTaskFlash("Ожидание физической перезагрузки")
        }

        // ожидание перезагрузки в случае если контроллер stm205
        if (flag205) {
            contextMain.runOnUiThread {
                firmwareSTMFragment.needResetPlease() // требование о том что нужно перезкгрузить модем
            }

            // надо добавить проверку в случае чего -----------------------------------------------------------------------
            while (!flagResetOk) {
                Thread.sleep(TIMEOUT_STEP_CHECK_RESET)
            }
            flagResetOk = false

            // инициализация
            if (!init()) {
                contextMain.runOnUiThread {
                    loadInterface.errorSend()
                }
                usbCommandsProtocol.flagWorkWrite = false
                return false
            }
        }

        //  вывод данных об этапе прошивки
        contextMain.runOnUiThread {
            firmwareSTMFragment.currentTaskFlash("Получение версии прошивки BootLoader")
        }

        // чтение версии bootloader
        Log.d("loadFileStm", "Получение версии прошивки BootLoader")
        val versionBootLoader = readBootLoader()
        Log.d("loadFileStm", "Версия прошивки BootLoader: ${versionBootLoader.toHexString()}")




        Log.d("loadFileStm", "Получение flash")
        //  вывод данных об этапе прошивки
        contextMain.runOnUiThread {
            firmwareSTMFragment.currentTaskFlash("Получение flash")

            // так же выводим информацию о bootloader
            firmwareSTMFragment.showBootLoaderVersion(versionBootLoader.toHexString())
        }

        // проверка влезит ли бут лоадер
        val flash = readFlesh(stm)
        Log.d("loadFileStm", "Получен flash: $flash")
        Log.d("loadFileStm", "Проверяем что прошивка влезет в flash")
        if (flash > sizeBootLoader) {
            Log.d("loadFileStm", "Очистка контроллера")

            //  вывод данных об этапе прошивки
            contextMain.runOnUiThread {
                firmwareSTMFragment.currentTaskFlash("Очистка контроллера")
            }

            if (!clearFlash(flash)) {
                contextMain.runOnUiThread {
                    loadInterface.errorSend()
                }
                usbCommandsProtocol.flagWorkWrite = false
                return false
            } else {
                // инициализация
                if (flash > 1023 && !init()) {
                    contextMain.runOnUiThread {
                        loadInterface.errorSend()
                    }
                    usbCommandsProtocol.flagWorkWrite = false
                    return false
                }
            }
            Log.d("loadFileStm", "Очистка завершена")


            Log.d("loadFileStm", "Отправка bootloader")

            //  вывод данных об этапе прошивки
            contextMain.runOnUiThread {
                firmwareSTMFragment.currentTaskFlash("Отправка bootloader")
            }

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

                //  вывод данных об этапе прошивки
                contextMain.runOnUiThread {
                    firmwareSTMFragment.currentTaskFlash("Отправка program")
                }

                if (!writeFileToController(fileProgram, addressProgram, loadInterface)) {
                    contextMain.runOnUiThread {
                        loadInterface.errorSend()
                    }
                    usbCommandsProtocol.flagWorkWrite = false
                    return false
                }
                Log.d("loadFileStm", "Отправка program завершена")

            } else {
                contextMain.runOnUiThread {
                    loadInterface.errorSend()
                }
                usbCommandsProtocol.flagWorkWrite = false
                return false
            }
        } else {
            contextMain.runOnUiThread {
                loadInterface.errorSend()
            }
            usbCommandsProtocol.flagWorkWrite = false
            return false
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
        var byteArray: ByteArray = sendPack(writeCommand)
        if (byteArray.isEmpty() || byteArray[0] != ACK) return false

        // Шаг 2: Отправляем адрес в обратном порядке
        val addressBytes = byteArrayOf(
            (address shr 24).toByte(),
            (address shr 16).toByte(),
            (address shr 8).toByte(),
            address.toByte()
        )
        val addressXor = calculateXor(addressBytes)
        byteArray = sendPack(addressBytes + byteArrayOf(addressXor))
        if (byteArray.isEmpty() || byteArray[0] != ACK) return false

        // Шаг 3: Отправляем блок данных
        val length = (dataBlock.size - 1).toByte() // Длина блока (на 1 меньше)
        val dataXor = calculateXor(byteArrayOf(length) + dataBlock)
        byteArray = sendPack(byteArrayOf(length) + dataBlock + byteArrayOf(dataXor))
        return byteArray.isNotEmpty() && byteArray[0] == ACK && checkDataSand(address, dataBlock)

    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun checkDataSand(address: Int, dataBlock: ByteArray): Boolean {
        // Шаг 1: Отправляем команду записи
        val writeCommand = byteArrayOf(0x11.toByte(), 0xEE.toByte())
        var byteArray: ByteArray = sendPack(writeCommand)
        if (byteArray.isEmpty() || byteArray[0] != ACK) return false

        // Шаг 2: Отправляем адрес в обратном порядке
        val addressBytes = byteArrayOf(
            (address shr 24).toByte(),
            (address shr 16).toByte(),
            (address shr 8).toByte(),
            address.toByte()
        )
        val addressXor = calculateXor(addressBytes)
        byteArray = sendPack(addressBytes + byteArrayOf(addressXor))
        if (byteArray.isEmpty() || byteArray[0] != ACK) return false

        contextMain.currentDataByteAll = byteArrayOf()

        // отправляем сколько байт мне нужно считать а так же кс по всем отправленным данным
        contextMain.usb.writeDevice(
            "",
            false,
            (byteArrayOf(0xFF.toByte()) + calculateXor(
                byteArrayOf(0x11.toByte(), 0xEE.toByte()) +
                        (addressBytes.reversedArray()  + calculateXor(addressBytes)) +
                        byteArrayOf(0xFF.toByte())
            )),
            false
        )

        return try {
            if (!waitForResponce(5)) false
            val currentData = contextMain.currentDataByteAll.drop(1).toByteArray()

            for (i in currentData.indices) {
                if (currentData[i] != dataBlock[i]) false
            }
            true
        } catch (e: Exception) { false }
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
    private fun waitForResponce(slowly: Long = 1, dalay: Int = 1): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < TIMEOUT * dalay) {

            if (!contextMain.usb.checkConnectToDevice(reconnect = false) || stmFragment.flagCancellation)
                return false

            // проверка буфера ответа
            if (contextMain.currentDataByteAll.isNotEmpty()) {
                Thread.sleep(10 * slowly)
                return true
            }

        }

        return false
    }

    // инициализация
    private fun init(): Boolean {
        contextMain.currentDataByteAll = byteArrayOf()

        Log.d("loadFileStm", "Инициализация")
        // отправка байта для проверки готовности устройства
        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x7F.toByte()),
            false
        )

        // ждем ответ и смотрим что там
        return waitForResponce()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun readoutUnprotect(): Boolean {
        contextMain.currentDataByteAll = byteArrayOf()

        // отправка байта для разблокировки контроллера
        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x92.toByte(), 0x6D.toByte()),
            false
        )
        Log.d("loadFileStm", "отправка ${byteArrayOf(0x92.toByte(), 0x6D.toByte()).toHexString()}")


        return waitForResponce() && contextMain.currentDataByteAll[0] == ACK
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

        if (waitForResponce(5) && contextMain.currentDataByteAll.size == 5) {
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
    private fun unblockRead(stm: Stm): Boolean {
        contextMain.currentDataByteAll = byteArrayOf()

        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x92.toByte(), 0x6D.toByte()),
            false
        )

        if (waitForResponce() && contextMain.currentDataByteAll[0] == ACK) {
            Thread.sleep(500)
            return  if (stm != Stm.STM205 ) init()
            else true
        }
        return false
    }


    // Разблокирование записи
    private fun unblockWrite(stm: Stm): Boolean {

        contextMain.currentDataByteAll = byteArrayOf()

        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x73.toByte(), 0x8C.toByte()),
            false
        )

        if (waitForResponce(5) && contextMain.currentDataByteAll[0] == ACK ) {
            Thread.sleep(500)
            return  if (stm != Stm.STM205 ) init()
            else true
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
    @OptIn(ExperimentalStdlibApi::class)
    private fun readFlesh(stm: Stm): Int {

        // исключение из правил для 205
        if (stm == Stm.STM205) return 1024

        contextMain.currentDataByteAll = byteArrayOf()

        // отправляем команду для чтения памяти
        contextMain.usb.writeDevice(
            "",
            false,
            byteArrayOf(0x11.toByte(), 0xEE.toByte()),
            false
        )
        Log.d("loadFileStm", "отправлено ${byteArrayOf(0x11.toByte(), 0xEE.toByte()).toHexString()}")

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

        contextMain.currentDataByteAll = byteArrayOf()

        // отправляем запрос на получения flash памяти
        contextMain.usb.writeDevice(
            "",
            false,
            (flashAddress  + calculateXor(flashAddress)),
            false
        )
        Log.d("loadFileStm", "отправлено ${(flashAddress  + calculateXor(flashAddress)).toHexString()}")

        // ожидаем ответ
        if (!(waitForResponce() && contextMain.currentDataByteAll[0] == ACK))
            return 0

        contextMain.currentDataByteAll = byteArrayOf()

        // отправляем сколько байт мне нужно считать а так же кс по всем отправленным данным
        contextMain.usb.writeDevice(
            "",
            false,
            (byteArrayOf(0x01.toByte()) + calculateXor(
                    byteArrayOf(0x11.toByte(), 0xEE.toByte()) +
                    (flashAddress.reversedArray()  + calculateXor(flashAddress)) +
                    byteArrayOf(0x01.toByte())
                )),
            false
        )
        Log.d("loadFileStm", "отправлено ${(byteArrayOf(0x01.toByte()) + calculateXor(
            byteArrayOf(0x11.toByte(), 0xEE.toByte()) +
                    (flashAddress.reversedArray()  + calculateXor(flashAddress)) +
                    byteArrayOf(0x01.toByte())
        )).toHexString()}")

        return try {
            if (!waitForResponce(5)) 0
            var flashInt: Int = contextMain.currentDataByteAll[2].toInt()
            flashInt = (flashInt shl 8) or contextMain.currentDataByteAll[1].toInt()
            flashInt
        } catch (e: Exception) { 0 }

    }

    // глобальная отчистка
    private fun clearFlash(flashSize: Int): Boolean {

        // большой обьем памяти
        if (flashSize > 1023) {
            contextMain.currentDataByteAll = byteArrayOf()

            // Шаг 1: Разблокировка
            contextMain.usb.writeDevice(
                "",
                false,
                byteArrayOf(0x44.toByte(), 0xBB.toByte()),
                false
            )

            if (!waitForResponce() || contextMain.currentDataByteAll[0] != ACK) {
                return false
            }

            contextMain.currentDataByteAll = byteArrayOf()

            // Массовое стирание
            contextMain.usb.writeDevice(
                "",
                false,
                byteArrayOf(0xFF.toByte(), 0xFF.toByte()) + calculateXor(byteArrayOf(0xFF.toByte(), 0xFF.toByte())) + 0x00.toByte(),
                false
            )

            // ждем отчищения
            return waitForResponce(dalay = 7) // 7 это увеличинное время для того что бы программа не поняла что тип так долго нет ответа и продолжала ждать
        } else  { // малый обьем
            contextMain.currentDataByteAll = byteArrayOf()

            // Шаг 1: Разблокировка
            contextMain.usb.writeDevice(
                "",
                false,
                byteArrayOf(0x43.toByte(), 0xBC.toByte()),
                false
            )

            if (waitForResponce() && contextMain.currentDataByteAll[0] == ACK) {
                contextMain.currentDataByteAll = byteArrayOf()

                // Отправка второго ключа для завершения разблокировки
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