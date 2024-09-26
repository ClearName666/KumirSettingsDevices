package com.kumir.settingupdevices.usb

import android.R.attr.data
import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbCTSCallback
import com.felhr.usbserial.UsbSerialInterface.UsbDSRCallback
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import com.kumir.settingupdevices.R
import com.kumir.testappusb.settings.ConstUsbSettings
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
        private const val RECONNECT_TIMEOUT = 2000L

    }

    val speedList: ArrayList<Int> = arrayListOf(
        300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 230400) // скорости в бодах

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

    private lateinit var threadAtCommand: Thread
    private var flagActivThreadATCommand: Boolean = false
    private var flagActivThreadChaeckConnection: Boolean = false


    var flagAtCommandYesNo: Boolean = false

    private var flagAtCommand: Boolean = true
    private var flagReadDsrCts: Boolean = false
    private var flagIgnorRead: Boolean = false

    private var flagSandAtOk: Boolean = false

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
                3 -> it.setParity(UsbSerialInterface.PARITY_MARK)
                4 -> it.setParity(UsbSerialInterface.PARITY_SPACE)
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
    }

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
        return data + checkSum(data)
    }

    fun checkSum(data: ByteArray): ByteArray {
        val crc = CRC16Modbus()
        crc.update(data)
        return crc.crcBytes
    }

    // проверка подклюения девайса к устройству
    fun checkConnectToDevice(show: Boolean = false, at: Boolean = false, reconnect: Boolean = true): Boolean {
        if (context is UsbActivityInterface) {
            val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val devises: HashMap<String, UsbDevice> = usbManager.deviceList

            for (device in devises) {
                if (device.value.deviceName == deviceUsb?.deviceName && connection != null) {
                    return true
                }
            }

            onClear()

            // поток попуток пповторного подключения
            if (reconnect) {            // если выставлен флаг переподключения то пытаемся переподключиться
                Thread {
                    for (i in 0..CNT_RECONNECT_DEVISE) {
                        Thread.sleep(TIMEOUT_RECONNECT)
                        if (attemptConnect()) {
                            if (at) flagAtCommandYesNo = true
                            break
                        }
                    }
                }.start()
            }

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

        flagActivThreadATCommand = false
        flagActivThreadChaeckConnection = false

        dsrState = false
        ctsState = false
        //serialPort?.close() // xmodem

        connection?.close()
        usbSerialDevice?.close()

        connection = null
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
    @OptIn(ExperimentalStdlibApi::class)
    fun writeDevice(message: String, flagPrint: Boolean = true, byteArray: ByteArray? = null, flagCheckSum: Boolean = true): Boolean {
        return if (usbSerialDevice != null) {
            executorUsb.execute {
                try {
                    // вычисление кс если нужно если не нужно то тогда просто помещяем строку или просто массив бдайт без нечего
                    val bytesToSend =
                        if (byteArray != null) {
                            if (flagCheckSum) calculateChecksum(byteArray)
                            else byteArray
                        } else {
                            (message + lineFeed).toByteArray()
                        }

                    Log.d("usbData", "write: ${bytesToSend.toHexString()} size = ${bytesToSend.size}")
                    Log.d("usbData", "write_message: $message")

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

    // переподключение для п101
    /*fun reconnectToDevice() {
        executorUsb.execute {

            onClear() // очищение подключения
            Thread.sleep(2000)

            // повторное подключение
            attemptConnect()
        }
    }*/

    // переподлючения
    fun reconnectCDC() {
        /*executorUsb.execute {*/
            if (checkConnectToDevice()) {
                connection?.controlTransfer(
                    0x21,
                    0x22,
                    0x0000,
                    0,
                    null,
                    0,
                    5000
                )

                connection?.controlTransfer(
                    0x21,
                    0x22,
                    0x0003,
                    0,
                    null,
                    0,
                    5000
                )
            }
        /*}*/

    }

    // ожиадание подключения по usb
    fun waitConnection(): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < RECONNECT_TIMEOUT) {
            if (checkConnectToDevice()) return true
            Thread.sleep(2)
        }

        return false
    }

    // регистрация широковещятельного приемника
    @OptIn(ExperimentalStdlibApi::class)
    fun connect(connection: UsbDeviceConnection?, currentDevice: UsbDevice) {
        try {
            if (connection != null) {
                try {
                    usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(
                        currentDevice, connection)
                    usbSerialDevice?.open()

                    usbSerialDevice?.let {
                        if (it.open()) {
                            val readCallback = UsbReadCallback { bytes ->
                                if (!flagIgnorRead) {
                                    printUIThread(String(bytes, Charsets.UTF_8), bytes)
                                } else {
                                    flagSandAtOk = String(bytes, Charsets.UTF_8).contains("OK")
                                }

                                Log.d("usbData", "read: ${bytes.toHexString()}")
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
                    deviceUsb = currentDevice
                    curentDeviceName = currentDevice.deviceId.toString()

                    this.connection = connection

                    // поток для отправки в фоновом режиме at команды
                    if (!flagActivThreadATCommand) {
                        threadAtCommand = Thread {
                            Log.d("threadInfo", "AT поток создан")
                            flagAtCommand = true
                            end@while (flagAtCommand) {
                                while (flagAtCommandYesNo) {
                                    Thread.sleep(TIMEOUT_MOVE_AT)
                                    if (checkConnectToDevice() ) {
                                        if (flagAtCommand && flagAtCommandYesNo) {
                                            writeDevice(at, false)
                                            Log.d("atSand", at)
                                            flagIgnorRead = true
                                            Thread.sleep(TIMEOUT_IGNORE_AT)
                                            flagIgnorRead = false
                                        }
                                    } else { // нету подключение вылет из потока
                                        break@end
                                    }
                                }
                                Thread.sleep(TIMEOUT_MOVE_AT / 30)
                            }

                            Log.d("threadInfo", "AT поток удален ")
                        }

                        threadAtCommand.start()
                        flagActivThreadATCommand = true
                    }


                    // постоянная проверка подключения к устройству
                    if (!flagActivThreadChaeckConnection) {
                        Thread {
                            if (context is UsbActivityInterface) {
                                while (checkConnectToDevice(true)) {
                                    Thread.sleep(TIMEOUT_CHECK_CONNECT)
                                }
                            }
                        }.start()
                        flagActivThreadChaeckConnection = true
                    }


                    (context as Activity).runOnUiThread {
                        if (context is UsbActivityInterface) {
                            context.showDeviceName(currentDevice.productName.toString())
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

