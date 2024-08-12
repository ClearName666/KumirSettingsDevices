package com.example.kumirsettingupdevices.usb

import android.content.Context
import android.util.Log
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSensorID
import com.example.kumirsettingupdevices.model.recyclerModel.StSearchOneWire
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

class OneWire(val usb: Usb, private val context: Context) {

    companion object {
        const val TIME_MAX_DEL_ONE_WIRE: Int = 100
        const val SIZE_BUF_GET_TEMP: Int = 72 // 9 байт
        const val TIME_READ_TEMP: Long = 1500 // 1.5 сек

        // таблица контрольных сумм
        val crcTable = listOf(
            0, 94, 188, 226, 97, 63, 221, 131, 194, 156, 126, 32, 163, 253, 31, 65,
            157, 195, 33, 127, 252, 162, 64, 30, 95, 1, 227, 189, 62, 96, 130, 220,
            35, 125, 159, 193, 66, 28, 254, 160, 225, 191, 93, 3, 128, 222, 60, 98,
            190, 224, 2, 92, 223, 129, 99, 61, 124, 34, 192, 158, 29, 67, 161, 255,
            70, 24, 250, 164, 39, 121, 155, 197, 132, 218, 56, 102, 229, 187, 89, 7,
            219, 133, 103, 57, 186, 228, 6, 88, 25, 71, 165, 251, 120, 38, 196, 154,
            101, 59, 217, 135, 4, 90, 184, 230, 167, 249, 27, 69, 198, 152, 122, 36,
            248, 166, 68, 26, 153, 199, 37, 123, 58, 100, 134, 216, 91, 5, 231, 185,
            140, 210, 48, 110, 237, 179, 81, 15, 78, 16, 242, 172, 47, 113, 147, 205,
            17, 79, 173, 243, 112, 46, 204, 146, 211, 141, 111, 49, 178, 236, 14, 80,
            175, 241, 19, 77, 206, 144, 114, 44, 109, 51, 209, 143, 12, 82, 176, 238,
            50, 108, 142, 208, 83, 13, 239, 177, 240, 174, 76, 18, 145, 207, 45, 115,
            202, 148, 118, 40, 171, 245, 23, 73, 8, 86, 180, 234, 105, 55, 213, 139,
            87, 9, 235, 181, 54, 104, 138, 212, 149, 203, 41, 119, 244, 170, 72, 22,
            233, 183, 85, 11, 136, 214, 52, 106, 43, 117, 151, 201, 74, 20, 246, 168,
            116, 42, 200, 150, 21, 75, 169, 247, 182, 232, 10, 84, 215, 137, 107, 53
        )

        val DEFUALT_BUF = byteArrayOf(
            0x28.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),

            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte()
        )

        // байт поиска 0xF0 1111_0000
        val COMMAND_SEARCH_ADDRESS_F0 = byteArrayOf(
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),

            0xC0.toByte(),
            0xC0.toByte(),
            0xC0.toByte(),
            0xC0.toByte()
        )

        val COMMAND_START_TEMP_44 = byteArrayOf(
            0xC0.toByte(),
            0xFF.toByte(),
            0xC0.toByte(),
            0xC0.toByte(),

            0xC0.toByte(),
            0xFF.toByte(),
            0xC0.toByte(),
            0xC0.toByte()
        )

        val COMMAND_READ_CURENT_TEMP_BE = byteArrayOf(
            0xFF.toByte(),
            0xC0.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),

            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xC0.toByte()
        )

        val COMMAND_SKIP_CC = byteArrayOf(
            0xFF.toByte(),
            0xFF.toByte(),
            0xC0.toByte(),
            0xC0.toByte(),

            0xFF.toByte(),
            0xFF.toByte(),
            0xC0.toByte(),
            0xC0.toByte()
        )

        val SELECT_SENSOR_55 = byteArrayOf(
            0xC0.toByte(),
            0xFF.toByte(),
            0xC0.toByte(),
            0xFF.toByte(),

            0xC0.toByte(),
            0xFF.toByte(),
            0xC0.toByte(),
            0xFF.toByte()
        )

        // 72 бита
        val GET_TEMP_BITS: ByteArray = ByteArray(SIZE_BUF_GET_TEMP) { 0xFF.toByte() }
    }




    // лист для хранения адресов на линии
    var listOneWireAddres = mutableListOf<ItemSensorID>()
    private val listOneWireAddresHex: MutableList<ByteArray> = mutableListOf()

    // хранит температуры всех датчиков
    //var listTempDT112 = mutableMapOf<String, Int>()

    // функция для того что бы запускать в ней отправку данных с обязательным ответом
    private fun necessarilySendSleepDataReceive(context: MainActivity, byteArray: ByteArray) {
        if (sendSleepDataReceive(context, byteArray)) // успешная отправка
            return


        throw Exception()
    }


    // для ожидания прихода данных максимальная забержка TIME_MAX_DEL_ONE_WIRE
    private fun sendSleepDataReceive(context: MainActivity, byteArray: ByteArray): Boolean {


        // очищение буферов
        context.curentDataByteAll = byteArrayOf()
        context.curentDataByte = byteArrayOf()


        // отправка
        usb.usbSerialDevice?.write(byteArray)
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


        return context.curentDataByteAll.isNotEmpty()
    }

    // получение адресов
    fun scanOneWireDevices(usbCommandsProtocol: UsbCommandsProtocol, context: MainActivity) {
        // отчистка прошлых данных
        listOneWireAddres.clear()
        listOneWireAddresHex.clear()

        Thread {
            usb.onSerialRTS(1)

            usbCommandsProtocol.flagWorkRead = true
            usbCommandsProtocol.flagWorkOneWire = true

            // обьект для полученя адресов
            val stSearch = StSearchOneWire(0, 64, 0, DEFUALT_BUF)
            var address: ByteArray

            // пока есть устройства опраиваем
            //var addresCnt = 0
            var numberAddress = 10


            while (numberAddress > 0) {
                //addresCnt++
                address = owSearchNext(context, stSearch)

                if (address.isNotEmpty()) {
                    try {
                        // пребразуем число в строку из  1 и 0 и после  разделяем их по 4 символа
                        val strAddress: String = address.reversedArray().joinToString(separator = " ") { byte -> "%02X".format(byte) }.replace(" ", "")
                        Log.d("dataOneWire", "Адрес: $strAddress")

                        // добавление результата
                        if (listOneWireAddres.none { it.sensorID == strAddress }) {
                            numberAddress = 10
                            listOneWireAddres.add(ItemSensorID(strAddress, -300F))
                            //val addressAdd = address
                            listOneWireAddresHex.add(address.copyOf())
                        }
                        else
                            --numberAddress

                    } catch (_: Exception) {
                        Log.d("dataOneWire", "Произошла ошибка")
                        break
                    }
                }

                // проверка если подключение разорвано
                if (!usb.checkConnectToDevice() || context.curentDataByteAll.isEmpty()) {
                    listOneWireAddres.clear()
                    listOneWireAddresHex.clear()
                    break
                }
            }
            usb.onSerialRTS(0)

            usbCommandsProtocol.flagWorkRead = false
            usbCommandsProtocol.flagWorkOneWire = false
        }.start()
    }


    // проверка есть ли кто то не линии
    private fun owReset(context: MainActivity): Boolean {
        // изменение сскорости на 9600
        usb.onSerialSpeed(5)
        sendSleepDataReceive(context, byteArrayOf(0xF0.toByte()))

        // возвращяем скорость 115200
        usb.onSerialSpeed(9)

        return context.curentDataByteAll.isNotEmpty() &&
                context.curentDataByteAll[0] != 0xF0.toByte()
    }


    // алгоритм поиска адресов
    private fun owSearchNext(context: MainActivity, stSearch: StSearchOneWire): ByteArray {
        var iSearchDirection: Byte
        var iIDBit: Int
        var iCmpIDBit: Int

        // фдаг для контроля выхода
        var flagExit = false

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

            sendSleepDataReceive(context, COMMAND_SEARCH_ADDRESS_F0.reversedArray())

            do {
                // отправляем (FF,FF) и читаем что ответит устройства
                if (!sendSleepDataReceive(context, byteArrayOf(0xFF.toByte(), 0xFF.toByte()))){
                    flagExit = true
                    break
                }
                try {
                    // проверяем 1 и 2 бит которые пришли
                    iIDBit = if (context.curentDataByteAll[0] == 0xFF.toByte()) 1 else 0
                    iCmpIDBit = if (context.curentDataByteAll[1] == 0xFF.toByte()) 1 else 0

                    // если некого нет то выходим из цикла
                    if ((iIDBit == 1) && (iCmpIDBit == 1)) break

                    // кто-то ответил
                    if (iIDBit != iCmpIDBit) {
                        iSearchDirection = iIDBit.toByte()
                    } else {
                        iSearchDirection = if (iIDBitNumber < stSearch.iLastDiscrepancy) {
                            if (((stSearch.ROM[iROMByteNumber] and iROMByteMask)).toInt() != 0) 1 else 0
                        } else
                            if (iIDBitNumber.toByte() == stSearch.iLastDiscrepancy) 1 else 0

                        if (iSearchDirection == 0x00.toByte()) {
                            iLastZero = iIDBitNumber

                            if (iLastZero < 9)
                                stSearch.iLastFamilyDiscrepancy = iLastZero.toByte()
                        }
                    }

                    // Установить или сбросить бит в байте ROM с помощью маски rom_byte_mask
                    if (iSearchDirection.toInt() == 1) {
                        stSearch.ROM[iROMByteNumber] = stSearch.ROM[iROMByteNumber] or iROMByteMask
                    } else {
                        stSearch.ROM[iROMByteNumber] = stSearch.ROM[iROMByteNumber] and iROMByteMask.inv()
                    }

                    // Установка направления поиска серийного номера
                    val byteSend = if (iSearchDirection > 0) 0xFF.toByte() else 0xC0.toByte()
                    necessarilySendSleepDataReceive(context, byteArrayOf(byteSend))

                    iIDBitNumber++
                    iROMByteMask = (iROMByteMask.toUInt() shl 1).toByte()

                    // Если маска равна 0, перейти к новому байту ROM и сбросить маску
                    if (iROMByteMask == 0x00.toByte()) {
                        // Накопление CRC
                        iCRC = crcTable[(stSearch.ROM[iROMByteNumber] xor iCRC).toUByte().toInt()].toByte()
                        iROMByteNumber++
                        iROMByteMask = 1
                    }
                } catch (e: Exception) {
                    return byteArrayOf()
                }
            } while (iROMByteNumber < 8) // Цикл до тех пор, пока не пройдем все байты ROM с 0 до 7

            /* Если поиск был успешным, тогда */
            if (!(iIDBitNumber < 65 || iCRC != 0x00.toByte())) {
                stSearch.iLastDiscrepancy = iLastZero.toByte()

                /* Проверка на последнее устройство */
                if (stSearch.iLastDiscrepancy == 0x00.toByte())
                    stSearch.iLastDeviceFlag = 1

                iSearchResult = 1
            }
        }

        /* Если устройство не найдено, сбрасываем счетчики, чтобы следующий поиск был как первый */
        if (iSearchResult == 0 || stSearch.ROM[0] == 0x00.toByte() || flagExit) {
            stSearch.iLastDiscrepancy = 0
            stSearch.iLastDeviceFlag = 0
            stSearch.iLastFamilyDiscrepancy = 0
            return byteArrayOf()
        }

        return stSearch.ROM
    }


    // получение температыры всех датчиков
    fun getTempsDT112(usbCommandsProtocol: UsbCommandsProtocol) {

        // проверка получены ли адресы
        if (listOneWireAddresHex.isEmpty()) return

        Thread {
            usb.onSerialRTS(1)

            usbCommandsProtocol.flagWorkRead = true
            usbCommandsProtocol.flagWorkOneWire = true

            var iCRC: Byte = 0

            // команда на измерение температуры
            owReset((context as MainActivity))

            sendSleepDataReceive(context, COMMAND_SKIP_CC.reversedArray())
            sendSleepDataReceive(context, COMMAND_START_TEMP_44.reversedArray())
            Thread.sleep(TIME_READ_TEMP)


            var curent_index = 0
            var flagSuc = false

            // перебор всех адресов для получения температур
            for (address in listOneWireAddresHex) {
                if (!usb.checkConnectToDevice() || context.curentDataByteAll.isEmpty()) break

                // чтение данных
                for (io in 1..3) { // 3 попытки

                    selectSensor(address)

                    flagSuc = false

                    sendSleepDataReceive(context, COMMAND_READ_CURENT_TEMP_BE.reversedArray())

                    sendSleepDataReceive(context, GET_TEMP_BITS)


                    val sendByteArr = convertToArrByte(context.curentDataByteAll)

                    if (sendByteArr.size < 8) {
                        iCRC = 0
                        continue
                    }

                    for (i in 0..7) {
                        iCRC = crcTable[(sendByteArr[i] xor iCRC).toUByte().toInt()].toByte()
                    }

                    if (iCRC != sendByteArr[8]) {
                        iCRC = 0
                        continue
                    }

                    // делаем преобразование умножаем на 0,0625-------------------
                    var temp: Int = sendByteArr[0].toUByte().toInt()
                    temp = ((sendByteArr[1].toInt() shl 8) or temp)

                    // если ошибок нету то заполняем данные температурой
                    if (!(temp == 1360 && sendByteArr[6] == 0x0C.toByte())) {
                        val itemSensorID = listOneWireAddres[curent_index]
                        itemSensorID.temp = (temp * 0.0625).toFloat()
                        listOneWireAddres[curent_index] = itemSensorID
                    }

                    iCRC = 0

                    flagSuc = true
                    break
                }

                if (!flagSuc) break
                curent_index++
            }

            usb.onSerialRTS(0)

            usbCommandsProtocol.flagWorkRead = false
            usbCommandsProtocol.flagWorkOneWire = false
        }.start()
    }

    private fun selectSensor(address: ByteArray) {
        // если есть датчикки на линии
        owReset(context as MainActivity)

        sendSleepDataReceive(context, SELECT_SENSOR_55.reversedArray())

        // отправка адреса
        for (i in convertToByteArray(address) ) {
            sendSleepDataReceive(context, byteArrayOf(i))
        }
    }


    // преобразование из масива байт 0xFF and 0xC0 в байты
    private fun convertToArrByte(byteArray: ByteArray): ByteArray {
        if (byteArray.size < 8) return byteArrayOf()

        val retByteArray = ByteArray(byteArray.size / 8)

        for (i in byteArray.indices step 8) {
            // Берем подмассив из 8 элементов или меньше, если оставшихся элементов меньше 8
            val chunk = byteArray.copyOfRange(i, (i + 8).coerceAtMost(byteArray.size))

            retByteArray[i / 8] = convertToByte(chunk)
        }

        return retByteArray
    }


    // конвертаци байтов в вид 0xFF и 0xC0
    private fun convertToByteArray(byte: ByteArray): ByteArray {
        val byteArray = ByteArray(byte.size * 8)
        var maskByte: Byte = 1

        // перебираем биты
        for (i in 0..<byte.size * 8) {
            if (i % 8 == 0) maskByte = 1 // если байт сменился то обнуляем маску

            // при помощм маски проверяем биты и в зависемости от того 0 или 1 помещяем либо
            // 0xFF либо 0xC0 в результирующий массив
            byteArray[i] = if ((byte[i/8].toInt() and maskByte.toInt()) != 0) {
                0xFF.toByte()
            } else {
                0xC0.toByte()
            }

            maskByte = (maskByte.toInt() shl 1).toByte()
        }
        return byteArray
    }

    // преобразование из масива байт 0xFF and 0xC0 в байт
    private fun convertToByte(byteArray: ByteArray): Byte {
        if (byteArray.size != 8) return 0

        var byte: Byte = 0
        var maskByte: Byte = 1
        for (i in 0..7) {
            if (byteArray[i] == 0xFF.toByte())
                byte = byte or maskByte
            maskByte = (maskByte.toInt() shl 1).toByte()
        }

        return byte
    }
}