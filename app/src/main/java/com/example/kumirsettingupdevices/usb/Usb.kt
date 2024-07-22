package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.example.kumirsettingupdevices.R
import com.example.testappusb.settings.ConstUsbSettings
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbCTSCallback
import com.felhr.usbserial.UsbSerialInterface.UsbDSRCallback
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class Usb(private val context: Context) {

    val ACTION_USB_PERMISSION: String = "com.android.example.USB_PERMISSION"
    companion object {
        const val TIMEOUT_CHECK_CONNECT: Long = 100 // таймаут для проверки подключения
        const val TIMEOUT_MOVE_AT: Long = 3000
        const val TIMEOUT_IGNORE_AT: Long = 30

        // при попытки повторного подключения ...
        const val CNT_RECONNECT_DEVISE: Int = 700
        const val TIMEOUT_RECONNECT: Long = 10

        val speedList: ArrayList<Int> = arrayListOf(
            300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200) // скорости в бодах
    }

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

    private var dsrState = false
    private var ctsState = false

    // ат команда по умолчанию
    var at: String = "AT"


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
    fun checkConnectToDevice(show: Boolean = false): Boolean {
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
                    if (attemptConnect()) break
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

            // переподлключение
            if (attemptConnect())
                flagAtCommandYesNo = true
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
                    if (!flagAtCommand) {
                        Thread {
                            flagAtCommand = true
                            while (flagAtCommand) {
                                while (flagAtCommandYesNo && flagAtCommand) {
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
                    }

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
}