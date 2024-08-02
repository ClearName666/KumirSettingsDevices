package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.model.recyclerModel.StSearchOneWire
import com.example.testappusb.settings.ConstUsbSettings
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbCTSCallback
import com.felhr.usbserial.UsbSerialInterface.UsbDSRCallback
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.abs


class Usb(private val context: Context) {

    val ACTION_USB_PERMISSION: String = "com.android.example.USB_PERMISSION"
    companion object {
        const val TIMEOUT_CHECK_CONNECT: Long = 100 // таймаут для проверки подключения
        const val TIMEOUT_MOVE_AT: Long = 3000
        const val TIMEOUT_IGNORE_AT: Long = 30

        // при попытки повторного подключения ...
        const val CNT_RECONNECT_DEVISE: Int = 700
        const val TIMEOUT_RECONNECT: Long = 10
        const val TIME_MAX_DEL_ONE_WIRE: Int = 100

        val speedList: ArrayList<Int> = arrayListOf(
            300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200) // скорости в бодах
    }

    // для общего доступа
    val TIMEOUT_GET_ONEWIRE: Long = 3000

    // переводы строк
    private var lineFeed = "\r"
    private var lineFeedRead = "\r"

    //var serialPort: UsbSerialPort? = null

    var connection: UsbDeviceConnection? = null
    var usbSerialDevice: UsbSerialDevice? = null
    var deviceUsb: UsbDevice? = null

    private var curentDeviceName: String? = null

    // поток для usb
    private val executorUsb: ExecutorService = Executors.newSingleThreadExecutor()

    var flagAtCommandYesNo: Boolean = false

    private var flagAtCommand: Boolean = true
    private var flagReadDsrCts: Boolean = false
    private var flagIgnorRead: Boolean = false

    private var flagSandAtOk: Boolean = false

    private var dsrState = false
    private var ctsState = false

    // ат команда по умолчанию
    var at: String = "AT"

    // лист для хранения адресов на линии
    var listOneWireAddres = mutableListOf<String>()




    // настрока dsr cts
    fun onSelectDsrCts(numDsrCts: Int) {
        ConstUsbSettings.numDsrCts = numDsrCts
        usbSerialDevice?.let {
            when(numDsrCts) {
                0 -> {
                    it.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)

                    dsrState = false
                    ctsState = false

                    if (context is UsbActivityInterface) {
                        (context as Activity).runOnUiThread {
                            context.printDSR_CTS(0, 0)
                        }
                    }
                }
                1 -> {
                    it.close()
                    it.open()
                    onStartSerialSetting(false)

                    it.setFlowControl(UsbSerialInterface.FLOW_CONTROL_RTS_CTS)

                    dsrState = false
                    if (context is UsbActivityInterface) {
                        (context as Activity).runOnUiThread {
                            context.printDSR_CTS(0, 1)
                        }
                    }
                }
                2 -> {
                    it.close()
                    it.open()
                    onStartSerialSetting(false)

                    it.setFlowControl(UsbSerialInterface.FLOW_CONTROL_DSR_DTR)
                    ctsState = false
                    if (context is UsbActivityInterface) {
                        (context as Activity).runOnUiThread {
                            context.printDSR_CTS(1, 0)
                        }
                    }
                }
            }
        }
    }

    // настрока сериал порта <ЧИСЛО БИТ>
    fun onSelectUumBit(numBit: Boolean) {
        ConstUsbSettings.numBit = numBit

        usbSerialDevice?.let {
            when (numBit) {
                true -> it.setDataBits(UsbSerialInterface.DATA_BITS_8)
                false -> it.setDataBits(UsbSerialInterface.DATA_BITS_7)
            }
        }
    }

    // настрока сериал порта <СКОРОСТЬ В БОДАХ>
    fun onSerialSpeed(speedIndex: Int) {
        ConstUsbSettings.speedIndex = speedIndex

        usbSerialDevice?.let {
            if (speedList.size > speedIndex) {
                it.setBaudRate(speedList[speedIndex])
            }
        }
    }

    // настрока сериал порта <ЧЕТНОСТЬ>
    fun onSerialParity(parityIndex: Int) {
        ConstUsbSettings.parityIndex = parityIndex

        usbSerialDevice?.let {
            when (parityIndex) {
                0 -> it.setParity(UsbSerialInterface.PARITY_NONE)
                1 -> it.setParity(UsbSerialInterface.PARITY_EVEN)
                2 -> it.setParity(UsbSerialInterface.PARITY_ODD)
                else -> {}
            }
        }
    }

    // настрока сериал порта <СТОП БИТЫ>
    fun onSerialStopBits(stopBitsIndex: Int) {
        ConstUsbSettings.stopBit = stopBitsIndex

        usbSerialDevice?.let {
            when (stopBitsIndex) {
                0 -> it.setStopBits(UsbSerialInterface.STOP_BITS_1)
                1 -> it.setStopBits(UsbSerialInterface.STOP_BITS_2)
                else -> {}
            }
        }
    }

    // настрока перевода строки при отправки данных
    fun onSerialLineFeed(lineFeedIndex: Int) {
        when (lineFeedIndex) {
            0 -> lineFeed = "\r"
            1 -> lineFeed = "\n"
            2 -> lineFeed = "\r\n"
            3 -> lineFeed = "\n\r"
            else -> {}
        }
    }

    // настрока перевода строки при получении данных
    fun onSerialLineFeedRead(lineFeedIndex: Int) {
        when (lineFeedIndex) {
            0 -> lineFeedRead = "\r"
            1 -> lineFeedRead = "\n"
            2 -> lineFeedRead = "\r\n"
            3 -> lineFeedRead = "\n\r"
        }
    }

   /* // настрока сериал порта <DTR>
    fun onSerialDTR(indexDTR: Int) {
        ConstUsbSettings.dtr = indexDTR

        usbSerialDevice?.let {
            when (indexDTR) {
                0 -> it.setDTR(false)
                1 -> it.setDTR(true)
                else -> {}
            }
        }
    }

    // настрока сериал порта <RTS>
    fun onSerialRTS(indexRTS: Int) {
        ConstUsbSettings.rts = indexRTS

        usbSerialDevice?.let {
            when (indexRTS) {
                0 -> it.setRTS(false)
                1 -> it.setRTS(true)
                else -> {}
            }
        }
    }*/

    // настройка серийного порта при подключении
    fun onStartSerialSetting(flagOnSelectDsrCts: Boolean = true) {
        onSelectUumBit(ConstUsbSettings.numBit)
        onSerialSpeed(ConstUsbSettings.speedIndex)
        onSerialParity(ConstUsbSettings.parityIndex)
        onSerialStopBits(ConstUsbSettings.stopBit)
        //onSerialRTS(ConstUsbSettings.rts)
        //onSerialDTR(ConstUsbSettings.dtr)

        if (flagOnSelectDsrCts) {
            onSelectDsrCts(ConstUsbSettings.numDsrCts)
        }
        //Log.d("UsbMy", "ОК")
    }

    // вычисление контрольной суммы данных
    private fun calculateChecksum(data: ByteArray): ByteArray {
        val crc = CRC16Modbus()
        crc.update(data)
        val checkSum = crc.crcBytes
        return data + checkSum
    }


    // проверка подклюения девайса к устройству
    fun checkConnectToDevice(show: Boolean = false, at: Boolean = false): Boolean {
        if (context is UsbActivityInterface) {
            val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val devises: HashMap<String, UsbDevice> = usbManager.deviceList

            for (device in devises) {
                if (device.value.deviceName == deviceUsb?.deviceName) {
                    return true
                }
            }
            onClear()

            // поток попуток пповторного подключения
            Thread {
                for (i in 0..CNT_RECONNECT_DEVISE) {
                    Thread.sleep(TIMEOUT_RECONNECT)
                    if (attemptConnect()) {
                        if (at) flagAtCommandYesNo = true
                        break
                    }
                }
            }.start()

            // отобрадения статуса отключено
            if (show) {
                (context as Activity).runOnUiThread {
                    context.disconnected()
                }
            }

        }
        return false
    }

    // фукция для попыток повторного подключения
    private fun attemptConnect(): Boolean {
        val usbStaticMethods = UsbStaticMethods()

        val listDevice = usbStaticMethods.getAllUsbDevices(context)

        // если хоть что то подключено то
        for (device in listDevice) {
            if (context is UsbActivityInterface) {
                (context as Activity).runOnUiThread {
                    context.connectToUsbDevice(device) // подключаемся заного
                }
                return true
            }
        }
        return false
    }

    // очищение ресурсов после отклчения диваса
    fun onClear() {
        flagReadDsrCts = false
        flagAtCommand = false
        dsrState = false
        ctsState = false
        connection?.close()
        //serialPort?.close() // xmodem
        connection = null
        usbSerialDevice?.close()
        usbSerialDevice = null
        deviceUsb = null
        if (context is UsbActivityInterface) {
            (context as Activity).runOnUiThread {
                context.showButtonConnection(false)
                context.showDeviceName("")
                context.printDSR_CTS(0, 0)
            }
        }
    }
    // удаление всего что связано с usb
    fun onDestroy() {
        flagAtCommandYesNo = false
        /*try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {}*/

        onClear()
        executorUsb.shutdown()
    }

    // отправка данных в сериал порт
    fun writeDevice(message: String, flagPrint: Boolean = true, byteArray: ByteArray? = null): Boolean {
        return if (usbSerialDevice != null) {
            executorUsb.execute {
                try {
                    var bytesToSend = (message + lineFeed).toByteArray()

                    if (byteArray != null)
                        bytesToSend = calculateChecksum(byteArray)

                    when (ConstUsbSettings.numDsrCts) {
                        0 -> usbSerialDevice?.write(bytesToSend)
                        1 -> {
                            if (ctsState) {
                                usbSerialDevice?.write(bytesToSend)
                            }
                        }
                        2 -> {
                            if (dsrState) {
                                usbSerialDevice?.write(bytesToSend)
                            }
                        }
                    }


                    if (flagPrint) {
                        printUIThread(message, bytesToSend)
                    }
                } catch (e: Exception) {
                    printWithdrawalsShow("${context.getString(R.string.Usb_ErrorWriteData)} ${e.message}")
                }
            }
            true
        } else {
            printWithdrawalsShow(context.getString(R.string.Usb_NoneConnect))
            false
        }
    }

    // отправка полученных и отправленых данных в ui радительский поток
    private fun printUIThread(msg: String, data: ByteArray) {
        if (context is UsbActivityInterface) {
            (context as Activity).runOnUiThread {
                context.printData(msg)
                context.printDataByte(data)
            }
        }
    }
    // отправка сообщений в ui радительский поток
    private fun printWithdrawalsShow(msg: String) {
        if (context is UsbActivityInterface) {
            (context as Activity).runOnUiThread {
                context.withdrawalsShow(msg)
            }
        }
    }

    fun reconnect() {
        executorUsb.execute {
            onClear() // очищение

            checkConnectToDevice(at = true)
        }
    }

    fun setAtCommand(commandAt: String?) {
        at = commandAt ?: "AT"
    }

    // регистрация широковещятельного приемника
    fun connect(connection: UsbDeviceConnection?, curentDevice: UsbDevice) {
        try {
            if (connection != null) {
                try {
                    usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(
                        curentDevice, connection)
                    usbSerialDevice?.open()

                    usbSerialDevice?.let {
                        if (it.open()) {
                            val readCallback = UsbReadCallback { bytes ->
                                if (!flagIgnorRead) {
                                    printUIThread(String(bytes, Charsets.UTF_8), bytes)
                                } else {
                                    flagSandAtOk = String(bytes, Charsets.UTF_8).contains("OK")
                                }
                            }

                            // чтение cts
                            val ctsCallback =
                                UsbCTSCallback { state ->
                                    ctsState = state
                                    Log.d("UsbMy", "ctsState $state")
                                    //printUIThread("${context.getString(R.string.cts)} = $it\n")
                                    if (context is UsbActivityInterface) {
                                        (context as Activity).runOnUiThread {
                                            context.printDSR_CTS(
                                                0,
                                                if (ctsState) 2 else 1
                                            )
                                        }
                                    }
                                }

                            // чтение dsr
                            val dsrCallback =
                                UsbDSRCallback { state ->
                                    dsrState = state
                                    Log.d("UsbMy", "dsrState $state")
                                    //printUIThread("${context.getString(R.string.rts)} = $it\n")
                                    if (context is UsbActivityInterface) {
                                        (context as Activity).runOnUiThread {
                                            context.printDSR_CTS(
                                                if (dsrState) 2 else 1,
                                                0
                                            )
                                        }
                                    }
                                }

                            it.read(readCallback)
                            it.getCTS(ctsCallback)
                            it.getDSR(dsrCallback)
                        }
                    }
                    if (context is UsbActivityInterface) {
                        context.showButtonConnection(true)
                    }

                    onStartSerialSetting()
                    deviceUsb = curentDevice
                    curentDeviceName = curentDevice.deviceId.toString()

                    // Инициализация USB и получение serialPort
                    /*val driver = UsbSerialProber.getDefaultProber().probeDevice(deviceUsb)
                    if (driver != null) {
                        val ports = driver.ports
                        if (ports.isNotEmpty()) {
                            serialPort = ports[0] // Используем первый доступный порт
                            try {
                                serialPort?.open(connection)
                                Log.d("XModemSender", "Serial port успешно открыт")
                            } catch (e: IOException) {
                                Log.e("XModemSender", "Ошибка при открытии serial port", e)
                            }
                        } else {
                            Log.e("XModemSender", "Нет доступных портов")
                        }
                    } else {
                        Log.e("XModemSender", "Не удалось найти драйвер для устройства USB")
                    }*/

                    // поток для отправки в фоновом режиме at команды
                    Thread {
                        flagAtCommand = true
                        while (flagAtCommand) {
                            while (flagAtCommandYesNo) {
                                Thread.sleep(TIMEOUT_MOVE_AT)
                                if (checkConnectToDevice() && flagAtCommand && flagAtCommandYesNo) {
                                    writeDevice(at, false)
                                    Log.d("atSand", at)
                                    flagIgnorRead = true
                                    Thread.sleep(TIMEOUT_IGNORE_AT)
                                    flagIgnorRead = false
                                }
                            }
                            Thread.sleep(TIMEOUT_MOVE_AT / 30)
                        }

                    }.start()

                    // постоянная проверка подключения к устройству
                    Thread {
                        if (context is UsbActivityInterface) {
                            while (checkConnectToDevice(true)) {
                                Thread.sleep(TIMEOUT_CHECK_CONNECT)
                            }
                        }
                    }.start()

                    (context as Activity).runOnUiThread {
                        if (context is UsbActivityInterface) {
                            context.showDeviceName(curentDevice.productName.toString())
                        }
                    }

                } catch (e: IOException) {
                    printWithdrawalsShow(context.getString(R.string.Usb_ErrorConnect))

                    onClear()
                }
            } else {
                // Обработка ошибки подключения
                printWithdrawalsShow(context.getString(R.string.Usb_ErrorConnect))
            }
        } catch (e: Exception) {
            // Обработка исключений
            printWithdrawalsShow(context.getString(R.string.Usb_ErrorConnect))

        }
    }

























    // протокол OneWire вынести в отдельный класс
    // код для получения адресов oneWire
    fun scanOneWireDevice(usbCommandsProtocol: UsbCommandsProtocol, context: MainActivity) {
        Thread {

            usbCommandsProtocol.flagWorkRead = true

            // изменение сскорости на 9600
            onSerialSpeed(5)
            sendSleepDataReceive(context, byteArrayOf(0xF0.toByte()))


            if (context.curentDataByteAll.isNotEmpty() && context.curentDataByteAll[0] != 0xF0.toByte()) {
                val addresses = mutableListOf<String>()

                // иинициализация буфера длля отправки и буфераа для ответа
                val size = 64 // 64 на данные


                // командный байт 0x33 -> 0011_0011
                val bufCommand = byteArrayOf(
                    0xC0.toByte(),
                    0xC0.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte(),

                    0xC0.toByte(),
                    0xC0.toByte(),
                    0xFF.toByte(),
                    0xFF.toByte()
                )
                val buf = ByteArray(size) { 0xFF.toByte() }

                // переключение скорости на 115200
                onSerialSpeed(9)

                // отправка реверсивно начиная с младщего байта ВАЖНО
                sendSleepDataReceive(context, bufCommand.reversedArray())

                // отправка буфера данных
                sendSleepDataReceive(context, buf)

                // получение и распарс данных
                if (context.curentDataByteAll.size == 64) {
                    addresses.add(parseOneWireAddress(context.curentDataByteAll.reversedArray(), size))

                    Log.d("dataOneWire", context.curentDataByte.joinToString(separator = " ") { byte -> "%02X".format(byte) })
                    listOneWireAddres = addresses
                } else {
                    Log.d("dataOneWire", "По какой то причине данные не валидны")
                }
            } else {
                val byteRez = context.curentDataByte.joinToString(separator = " ") { byte -> "%02X".format(byte) }
                Log.d("dataOneWire", "На линии некого нет. Пришел байт {$byteRez}")
            }

            usbCommandsProtocol.flagWorkRead = false
        }.start()
    }

    // для ожидания прихода данных максимальная забержка TIME_MAX_DEL_ONE_WIRE
    private fun sendSleepDataReceive(context: MainActivity, byteArray: ByteArray) {
        // очищение буферов
        context.curentDataByteAll = byteArrayOf()
        /*Log.d("dataOneWire", "буфер отчищен curentDataByteAll {${
            context.curentDataByteAll.joinToString(separator = " ") { byte -> "%02X".format(byte) }
        }}")*/
        context.curentDataByte = byteArrayOf()
        /*Log.d("dataOneWire", "буфер отчищен curentDataByte {${
            context.curentDataByte.joinToString(separator = " ") { byte -> "%02X".format(byte) }
        }}")*/


        // отправка
        usbSerialDevice?.write(byteArray)
        Log.d("dataOneWire", "отправлено byteArray ${
            byteArray.joinToString(separator = " ") { byte -> "%02X".format(byte) }
        }")


        // ждем ответа
        var time = 0
        while (context.curentDataByteAll.isEmpty() && time <= TIME_MAX_DEL_ONE_WIRE) {
            time++

            Thread.sleep(1)
        }
        //Thread.sleep(TIMEOUT_RECONNECT*5)
        Log.d("dataOneWire", "выход time = $time получены данные: {${
            context.curentDataByteAll.joinToString(separator = " ") { byte -> "%02X".format(byte) }
        }}")

    }

    private fun parseOneWireAddress(buf: ByteArray, size: Int): String {
        // Шаг 1: Преобразуем байты в строку из 1 и 0
        val binaryString = buf.joinToString("") { byte ->
            when (byte) {
                0xFF.toByte() -> "1"
                0xFC.toByte() -> "0"
                else -> error("Unexpected byte value")
            }
        }

        // Шаг 2: Разбиваем строку на части по 8 символов и преобразуем в символы ASCII
        return try {
            val result = StringBuilder()
            for (i in 0 until size step 8) {
                val byteString = binaryString.substring(i, i + 8)
                val ascii2Char = getChar2Byte16(byteString)
                result.append(ascii2Char)
            }

            // Итоговая строка из 16 символов
            result.toString()
        } catch (e: Exception) {
            context.getString(R.string.error)
        }
    }

    private fun getChar2Byte16(byteString: String): String { // "11111101" -> пример ввода
        // первый символ
        val charByte2_1 = byteString.drop(4)
        val charByte2_2 = byteString.dropLast(4)

        return convertToByte16(charByte2_2).toString() + convertToByte16(charByte2_1)

    }

    private fun convertToByte16(byteString: String): Char {
        return when(byteString) {
            "0000" -> '0'
            "0001" -> '1'
            "0010" -> '2'
            "0011" -> '3'
            "0100" -> '4'
            "0101" -> '5'
            "0110" -> '6'
            "0111" -> '7'
            "1000" -> '8'
            "1001" -> '9'
            "1010" -> 'A'
            "1011" -> 'B'
            "1100" -> 'C'
            "1101" -> 'D'
            "1110" -> 'E'
            "1111" -> 'F'
            else -> 'n'
        }
    }


    fun scanOneWireDevices(usbCommandsProtocol: UsbCommandsProtocol, context: MainActivity) {
        Thread {
            usbCommandsProtocol.flagWorkRead = true

            // обьект для полученя адресов
            val stSearch = StSearchOneWire(0, 0, 0, byteArrayOf(
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),

                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte()
            ))
            var address: ByteArray

            // пока есть устройства опраиваем
            do {
                address = owSearchNext(context, stSearch)

                if (address.isNotEmpty()) {
                    try {
                        // пребразуем число в строку из  1 и 0 и после  разделяем их по 4 символа
                        val strAddress: String = address.reversedArray().joinToString(separator = " ") { byte -> "%02X".format(byte) }.replace(" ", "")
                        Log.d("dataOneWire", "Адресс: $strAddress")

                        // добавление результата
                        listOneWireAddres.add(strAddress)
                    } catch (_: Exception) {
                        Log.d("dataOneWire", "Произошла ошибка")
                    }
                } else {
                    Log.d("dataOneWire", "Алгоритм неправельный")
                }

            } while (address.isNotEmpty())


            usbCommandsProtocol.flagWorkRead = false
        }.start()
    }


    // проверка есть ли кто то не линии
    private fun owReset(context: MainActivity): Boolean {
        // изменение сскорости на 9600
        onSerialSpeed(5)
        sendSleepDataReceive(context, byteArrayOf(0xF0.toByte()))

        // возвращяем скорость 115200
        onSerialSpeed(9)

        return context.curentDataByteAll.isNotEmpty() &&
                context.curentDataByteAll[0] != 0xF0.toByte()
    }


    private fun owSearchNext(context: MainActivity, stSearch: StSearchOneWire): ByteArray {
        var iSearchDirection: Byte
        var iIDBit: Int
        var iCmpIDBit: Int

        /* Инициализация для поиска */
        var iROMByteMask: Byte = 1
        var iCRC: Byte = 0
        var iIDBitNumber: Int = 1
        var iLastZero: Int = 0
        var iROMByteNumber: Int = 0
        var iSearchResult: Int = 0

        if (stSearch.iLastDeviceFlag == 0x00.toByte()) {

            // если некого на линии нету то сбрасываем и выходим
            if (!owReset(context)) {
                stSearch.onClearSearch()
                return byteArrayOf()
            }

            // байт поиска 0xF0 1111_0000
            val byteSearch = byteArrayOf(
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),

                0xC0.toByte(),
                0xC0.toByte(),
                0xC0.toByte(),
                0xC0.toByte()
            )

            sendSleepDataReceive(context, byteSearch.reversedArray())

            do {
                // отправляем (FF,FF) и читаем что ответит устройства
                Log.d("dataOneWire", "Отправляем (FF,FF) и читаем что ответит устройство")
                sendSleepDataReceive(context, byteArrayOf(0xFF.toByte(), 0xFF.toByte()))
                try {
                    // проверяем 1 и 2 бит которые пришли
                    Log.d("dataOneWire", "Проверяем 1 и 2 бит которые пришли")
                    iIDBit = if (context.curentDataByteAll[0] == 0xFF.toByte()) 1 else 0
                    iCmpIDBit = if (context.curentDataByteAll[1] == 0xFF.toByte()) 1 else 0

                    // если некого нет то выходим из цикла
                    Log.d("dataOneWire", "Если некто не ответил, выходим из цикла")
                    if ((iIDBit == 1) && (iCmpIDBit == 1)) break

                    // кто-то ответил
                    Log.d("dataOneWire", "Кто-то ответил")
                    if (iIDBit != iCmpIDBit) {
                        // все подключенные устройства имеют одинаковое начало адреса
                        Log.d("dataOneWire", "Все подключенные устройства имеют одинаковое начало адреса")
                        iSearchDirection = iIDBit.toByte()
                    } else {
                        iSearchDirection = if (iIDBitNumber < stSearch.iLastDiscrepancy) {
                            if ((stSearch.ROM[iROMByteNumber] and iROMByteMask) > 0) 1 else 0
                        } else
                            if (iIDBitNumber.toByte() == stSearch.iLastDiscrepancy) 1 else 0

                        if (iSearchDirection == 0x00.toByte()) {
                            iLastZero = iIDBitNumber

                            if (iLastZero < 9)
                                stSearch.iLastFamilyDiscrepancy = iLastZero.toByte()
                        }
                    }

                    // Установить или сбросить бит в байте ROM с помощью маски rom_byte_mask
                    Log.d("dataOneWire", "Устанавливаем или сбрасываем бит в байте ROM с помощью маски rom_byte_mask")
                    if (iSearchDirection.toInt() == 1) {
                        stSearch.ROM[iROMByteNumber] = stSearch.ROM[iROMByteNumber] or iROMByteMask
                    } else {
                        stSearch.ROM[iROMByteNumber] = stSearch.ROM[iROMByteNumber] and iROMByteMask.inv()
                    }

                    // Установка направления поиска серийного номера
                    Log.d("dataOneWire", "Установка направления поиска серийного номера")
                    val byteSend = if (iSearchDirection > 0) 0xFF.toByte() else 0xC0.toByte()
                    sendSleepDataReceive(context, byteArrayOf(byteSend))

                    iIDBitNumber++
                    iROMByteMask = (iROMByteMask.toInt() shl 1).toByte()

                    // Если маска равна 0, перейти к новому байту ROM и сбросить маску
                    Log.d("dataOneWire", "Если маска равна 0, переходим к новому байту ROM и сбрасываем маску")
                    if (iROMByteMask == 0x00.toByte()) {
                        // Накопление CRC
                        Log.d("dataOneWire", "Накопление CRC")
                        iCRC = stSearch.ROM[iROMByteNumber] xor iCRC
                        Log.e("dataOneWire", "iCRC = " +
                                "${stSearch.ROM[iROMByteNumber].toString(16)} xor ${iCRC.toString(16)}  " +
                                "результат ${(stSearch.ROM[iROMByteNumber] xor iCRC).toString(16)}"
                        )
                        iROMByteNumber++
                        iROMByteMask = 1
                    }

                } catch (e: Exception) {
                    // ошибка нечего не пришло в ответ или пришло неправильно
                    Log.d("dataOneWire", "Ошибка: нечего не пришло в ответ или пришло неправильно")
                    return byteArrayOf()
                }

            } while (iROMByteNumber < 8) // Цикл до тех пор, пока не пройдем все байты ROM с 0 до 7


            Log.d("dataOneWire", "iIDBitNumber = $iIDBitNumber")
            Log.d("dataOneWire", "iCRC = $iCRC")


            /* Если поиск был успешным, тогда */
            if (!((iIDBitNumber < 65) || (iCRC == 0x00.toByte()))) {
                stSearch.iLastDiscrepancy = iLastZero.toByte()

                /* Проверка на последнее устройство */
                if (stSearch.iLastDiscrepancy == 0x00.toByte())
                    stSearch.iLastDeviceFlag = 1

                iSearchResult = 1
                Log.d("dataOneWire", "stSearch.iLastDiscrepancy = $stSearch.iLastDiscrepancy")
            }
        }

        /* Если устройство не найдено, сбрасываем счетчики, чтобы следующий поиск был как первый */
        if (iSearchResult == 0 || stSearch.ROM[0] == 0x00.toByte()) {
            stSearch.iLastDiscrepancy = 0
            stSearch.iLastDeviceFlag = 0
            stSearch.iLastFamilyDiscrepancy = 0
            return byteArrayOf()
        }

        return stSearch.ROM
    }
}

