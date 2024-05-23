package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.usbFragments.Enfora1318Fragment

class UsbCommandsProtocol {

    var flagWorkChackSignal: Boolean = false
    var flagWorkWrite: Boolean = false

    private val listCommandNotFormater: List<String> = listOf(
        "AT\$FRIEND?"
    )

    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 90
        const val WAITING_FOR_THE_TEAMS_RESPONSE_FOR_SPEED: Long = 100

        const val MAX_CNT_EXPECTATION_SAND: Int = 30
        const val MAX_RATIO_EXPECTATION_NEW_SPEED: Int = 15

        //const val TIMEOUT_START_DEVICE: Long = 1500

        // для поиска скорости
        private const val SPEED_INDEX_MAX = 9
        private const val SPEED_INDEX_MIN = 0

        private const val PARITY_INDEX_MAX = 2
        private const val PARITY_INDEX_MIN = 0

        private const val STOPBIT_INDEX_MAX = 1
        private const val STOPBIT_INDEX_MIN = 0

        private const val BITDATA_INDEX_MAX = 1
        private const val BITDATA_INDEX_MIN = 0


    }



    // метод для получения настроек устройства
    fun readSettingDevice(commands: List<String>, context: Context,
                          usbFragment: UsbFragment, speedFind: Boolean = false) {
        val settingData: MutableMap<String, String> = mutableMapOf()
        var flagsSuccess: Boolean = true


        Thread {
            if (context is MainActivity) {
                // открываем диалог с загрузочным меню
                (context as Activity).runOnUiThread {
                    context.openCloseLoadingView(true)
                }

                // прогресс на единицу то есть каждая команда сколько то процентов
                val progressUnit: Int = 100 / commands.size
                var prograss: Int = 0

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // если нужно нати скорость перед использованием то
                if (speedFind && !findSpeed(context)) {
                    // все скорости переброны и нет ответа ошибка
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog(context.getString(R.string.errorFindSpeedSerial))
                    }
                    flagsSuccess = false
                }

                // перебор всех команд и получение ответов устройства
                outer@ for (command in commands) {
                    // 2 попытки на отправку
                    for (i in 1..2) {
                        // очищение прошлых данных
                        context.curentData = ""

                        context.usb.writeDevice(command, false)

                        // система получения ответа и ожидание полной отправки данных
                        if (!expectationSand(context)) {

                            // достигнуто ваксимальное время и нет ответа ошибка
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                            }
                            flagsSuccess = false
                            break@outer
                        }


                        if (context.curentData.isNotEmpty()) {

                            // нормализуем только если не входит в список команд которые не нужно нормализовать
                            settingData[command] = if (command !in listCommandNotFormater)
                                formatDataCommandsNormolize(context.curentData) else context.curentData

                            val curentData: String = context.curentData
                            // вывод в загрузочное диалог информации
                            (context as Activity).runOnUiThread {
                                context.printInfoTermAndLoaging(command, prograss)
                                context.printInfoTermAndLoaging(curentData, prograss)
                            }

                            prograss += progressUnit

                        } else {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.identifyDeviceFailed))
                            }
                            flagsSuccess = false
                            break
                        }

                        // проверка на валидность принатых данных возможно нужно еще раз опрасить
                        // проверка принялись ли данные
                        if (!sandOkCommand(context, command)) {

                            // если 2 попытка не сработала то выбрасываемся
                            if (i == 2) {
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(
                                        command + context.getString(R.string.errorSendDataRead)
                                    )
                                }
                                flagsSuccess = false
                                break@outer
                            }
                        } else {
                            break
                        }
                    }
                }

                // включение ат команд
                context.usb.flagAtCommandYesNo = true

                if (flagsSuccess) {
                    (context as Activity).runOnUiThread {
                        usbFragment.printSettingDevice(settingData)
                    }
                }

                context.curentData = ""
            }
        }.start()
    }

    fun writeSettingDevice(data: Map<String, String>, context: Context, usbFragment: UsbFragment,
                            saveFlag: Boolean = true, longSleepX: Int = 1) {

        Thread {
            flagWorkWrite = true
            if (context is MainActivity){
                var flagError: Boolean = false

                // открываем диалог с загрузочным меню
                (context as Activity).runOnUiThread {
                    context.openCloseLoadingView(true)
                }

                // прогресс на единицу то есть каждая команда сколько то процентов
                val progressUnit: Int = 100 / data.size
                var prograss: Int = 0

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // отправка всех настроек в устройство
                out@for ((key, value) in data) {
                    // 2 попытки на отправку
                    for (i in 1..2) {
                        // очищение прошлых данных
                        context.curentData = ""

                        val dataSend: String = key + value
                        context.usb.writeDevice(dataSend, false)


                        // система получения ответа и ожидание полной отправки данных
                        if (!expectationSand(context)) {

                            // достигнуто ваксимальное время и нет ответа ошибка
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                            }
                            flagError = true
                            flagWorkWrite = false
                            break@out
                        }

                        // дополнительное услоие если не пришло не ERROR не OK
                        if (!context.curentData.contains(context.getString(R.string.error)) && !context.curentData.contains(
                                context.getString(R.string.okSand)
                            )
                        ) {
                            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE * longSleepX * longSleepX)
                        }

                        val curentData: String = context.curentData
                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            prograss += progressUnit
                            context.printInfoTermAndLoaging(key + value, prograss)
                            context.printInfoTermAndLoaging(curentData, prograss)
                        }


                        // проверка принялись ли данные
                        if (!sandOkCommand(context, key)) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    key + value + context.getString(R.string.errorSendDataWrite)
                                )
                            }
                            flagWorkWrite = false
                            flagError = true
                            break
                        }

                        // проверка на ккоманды изменения скорости или настроек передачи
                        if (!commandNewSpeed(context, key, value)) {

                            // если 2 попытка не сработала то выбрасываемся
                            if (i == 2) {
                                flagError = true
                                flagWorkWrite = false

                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(context.getString(R.string.errorSpeed))
                                }
                                break@out
                            }
                        } else {
                            break
                        }
                    }
                }

                if (!flagError && saveFlag) {

                    // сохранение данных AT$LOAD
                    /*context.usb.writeDevice(context.getString(R.string.commandLoadSettings), false)
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)*/

                    // сохранение данных AT$SAVE
                    context.usb.writeDevice(context.getString(R.string.commandSaveSettings), false)
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                    usbFragment.readSettingStart()
                }

                context.curentData = ""

                // включение ат команд
                context.usb.flagAtCommandYesNo = true
            }
            flagWorkWrite = false
        }.start()
    }

    // метод для получения настроек устройства
    fun readSignalEnfora(command: String, context: Context, usbFragment: UsbFragment) {
        var flagsSuccess: Boolean = true
        flagWorkChackSignal = true
        Thread {
            if (context is MainActivity) {
                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                out@while (flagWorkChackSignal) {
                    // очищение прошлых данных
                    context.curentData = ""

                    context.usb.writeDevice(command, false)

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }
                        flagsSuccess = false
                        break@out
                    }

                    // нормаизируем данняе и разделяем
                    val data: List<String> = formatDataCommandsNormolize(context.curentData).split(",")

                    // после получения данных отправляем их на отображение
                    (context as Activity).runOnUiThread {
                        if (usbFragment is Enfora1318Fragment) {
                            try {
                                usbFragment.onPrintSignal(data[0], data[1])
                            } catch (e: Exception) {
                                flagsSuccess = false
                            }
                        }
                    }

                }


                // подкл ат команд
                context.usb.flagAtCommandYesNo = true

                if (!flagsSuccess)  {
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog(context.getString(R.string.identifyDeviceFailed))

                        // меняем текст кнопки
                        if (usbFragment is Enfora1318Fragment) {
                            usbFragment.onErrorStopChackSignal()
                        }
                    }
                }
                context.curentData = ""
            }
        }.start()
    }


    private fun commandNewSpeed(context: MainActivity, key: String, value: String): Boolean {
        // проверка на ккоманды изменения скорости или настроек передачи
        when(key) {
            context.getString(R.string.commandSetSpeed) -> {
                val speedIndex: Int = getSpeedIndax(value)
                // дополнительная задержка в случае если скорость слишком мала
                val sleepForMinSpeed = if (speedIndex < 5) (MAX_RATIO_EXPECTATION_NEW_SPEED - speedIndex + 1) else 1

                context.usb.onSerialSpeed(speedIndex)
                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE * sleepForMinSpeed)
            }
            context.getString(R.string.commandSetFormatParity) -> {
                try {
                    // получения format и четности
                    val formatAndParity: List<String> = value.split(",")

                    // преобразование farmat в настроки битов данных и стоп биты а так же наличие четности
                    val setPortList = reCalculateFormat(formatAndParity[0])

                    // применение настроек
                    context.usb.onSerialStopBits(setPortList[1])
                    context.usb.onSelectUumBit(setPortList[0] == 0)

                    // если есть четность то устанавливаем нужную
                    if (setPortList[2] == 1) {
                        if (formatAndParity[1].toInt() == 0) {
                            context.usb.onSerialParity(2) // утсновка odd
                        } else {
                            context.usb.onSerialParity(1) // установкаа even
                        }
                    }
                } catch (e: Exception) {
                    return false
                }
                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
            }
            context.getString(R.string.commandSetDeffoltSetting) -> {

                // сброс на настроки т к модем переключился на деффолтные настроки
                context.usb.onSerialParity(0)
                context.usb.onSelectUumBit(true)
                context.usb.onSerialStopBits(0)
                context.usb.onSerialSpeed(9)
            }
        }
        return true
    }


    // поиск скорости
    private fun findSpeed(context: MainActivity): Boolean {
        // ищем нужную скорость для общения
        for (bitData in BITDATA_INDEX_MIN..BITDATA_INDEX_MAX) {
            for (stopBit in STOPBIT_INDEX_MIN..STOPBIT_INDEX_MAX) {
                for (parity in PARITY_INDEX_MIN..PARITY_INDEX_MAX) {
                    for (speed in SPEED_INDEX_MIN..SPEED_INDEX_MAX) {
                        context.usb.onSelectUumBit(bitData == 0)
                        context.usb.onSerialParity(parity)
                        context.usb.onSerialStopBits(stopBit)
                        context.usb.onSerialSpeed(speed)

                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            context.printInfoTermAndLoaging(
                                speed.toString() + parity.toString() +
                                        stopBit.toString() + bitData.toString() + "\n", 0)
                        }

                        // отправка тестовой команды
                        context.usb.writeDevice(context.getString(R.string.commandSpeedFind), false)

                        // дополнительная задержка в случае если скорость слишком мала
                        val sleepForMinSpeed = if (speed < 5) (MAX_RATIO_EXPECTATION_NEW_SPEED - speed + 1) else 1
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE_FOR_SPEED * sleepForMinSpeed)

                        // если скорость найдена то выходим
                        if (context.curentData.contains(context.getString(R.string.okSand))) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }


    // система получения ответа и ожидание полной отправки данных
    private fun expectationSand(context: MainActivity): Boolean {

        // Ожидание что устройство отправит хоть что то максимум ждет 500 мс
        var maxCntIter: Int = MAX_CNT_EXPECTATION_SAND
        while (context.curentData.isEmpty()) {
            if (maxCntIter == 0) {
                return false
            }
            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
            maxCntIter--
        }

        // ожидание что бы все данные были отправлены
        var cnt: Int = context.curentData.length
        while (context.curentData.isNotEmpty()) {
            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
            val newCnt = context.curentData.length
            if (cnt == newCnt) {
                return true
            } else {
                cnt = newCnt
            }
        }
        return false
    }

    private fun sandOkCommand(context: MainActivity, command: String = ""): Boolean {
        // проверка принялись ли данные
        if (context.curentData.isEmpty() ||
            context.curentData.contains(context.getString(R.string.error)) ||
            !context.curentData.contains(context.getString(R.string.okSand))) {

            // если команда не входит в список команд которые не должны давать ответа то ерорим все
            if (command != context.getString(R.string.commandSetResetModem) &&
                command != context.getString(R.string.commandSetFormatParity)) {
                return false
            }

        }
        return true
    }


    fun calculateFormat(bitsData: Int, stopBits: Int, parity: Int): Int {
        return when {
            bitsData == 0 && stopBits == 1 && parity == 0 -> 1 // 8 данных, 2 стопа, без четности
            bitsData == 0 && stopBits == 0 && parity == 1 -> 2 // 8 данных, 1 стоп, с четностью
            bitsData == 0 && stopBits == 0 && parity == 0 -> 3 // 8 данных, 1 стоп, без четности
            bitsData == 1 && stopBits == 1 && parity == 0 -> 4 // 7 данных, 2 стопа, без четности
            bitsData == 1 && stopBits == 0 && parity == 1 -> 5 // 7 данных, 1 стоп, с четностью
            bitsData == 1 && stopBits == 0 && parity == 0 -> 6 // 7 данных, 1 стоп, без четности
            else -> 0
        }
    }

    private fun reCalculateFormat(state: String): List<Int> {
        return when (state) {
            "1" -> listOf(0, 1, 0) // 8 данных, 2 стопа, без четности
            "2" -> listOf(0, 0, 1) // 8 данных, 1 стоп, с четностью
            "3" -> listOf(0, 0, 0) // 8 данных, 1 стоп, без четности
            "4" -> listOf(1, 1, 0) // 7 данных, 2 стопа, без четности
            "5" -> listOf(1, 0, 1) // 7 данных, 1 стоп, с четностью
            "6" -> listOf(1, 0, 0) // 7 данных, 1 стоп, без четности
            else -> throw IllegalArgumentException("Invalid reCalculateFormat ${javaClass.name}")
        }
    }

    private fun getSpeedIndax(speed: String): Int {
        try {
            return when(speed) {
                "300" -> 0
                "600" -> 1
                "1200" -> 2
                "2400" -> 3
                "4800" -> 4
                "9600" -> 5
                "19200" -> 6
                "38400" -> 7
                "57600" -> 8
                "115200" -> 9
                else -> -1
            }
        } catch (e: Exception) {
            return -1
        }
    }




    private fun formatDataCommandsNormolize(data: String): String {
        return data.substringAfter(": ").substringBefore("\n").dropLast(1)
    }

}