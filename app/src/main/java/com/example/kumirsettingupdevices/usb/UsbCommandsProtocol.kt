package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.usbFragments.Enfora1318Fragment

class UsbCommandsProtocol {

    var flagWorkChackSignal: Boolean = false
    var flagWorkWrite: Boolean = false
    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 50
        const val WAITING_FOR_THE_TEAMS_RESPONSE_FOR_SPEED: Long = 100
        const val TIMEOUT_START_DEVICE: Long = 1500

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

                Thread.sleep(TIMEOUT_START_DEVICE)

                // прогресс на единицу то есть каждая команда сколько то процентов
                var progressUnit: Int = 100 / commands.size

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // если нужно нати скорость перед использованием то
                if (speedFind) {
                    flagsSuccess = false

                    // ищем нужную скорость для общения
                    outer@ for (bitData in BITDATA_INDEX_MIN..BITDATA_INDEX_MAX) {
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
                                            stopBit.toString() + bitData.toString() + "\n", progressUnit)
                                    }

                                    // отправка тестовой команды
                                    context.usb.writeDevice(context.getString(R.string.commandSpeedFind), false)

                                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE_FOR_SPEED)

                                    // если скорость найдена то выходим
                                    if (context.curentData.contains(context.getString(R.string.okSand))) {
                                        flagsSuccess = true
                                        break@outer
                                    }
                                }
                            }
                        }
                    }
                }

                // перебор всех команд и получение ответов устройства
                outer@ for (command in commands) {

                    // очищение прошлых данных
                    context.curentData = ""

                    context.usb.writeDevice(command, false)

                    // система получения ответа и ожидание полной отправки данных
                    var maxCntIter: Int = 10
                    while (context.curentData.isEmpty()) {
                        if (maxCntIter == 0) {
                            flagsSuccess = true
                            break@outer
                        }
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                        maxCntIter--
                    }


                    var cnt: Int = context.curentData.length
                    while (context.curentData.isNotEmpty()) {
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                        val newCnt = context.curentData.length
                        if (cnt == newCnt) {
                            break
                        } else {
                            cnt = newCnt
                        }
                    }


                    if (context.curentData.isNotEmpty()) {
                        settingData[command] = formatDataCommandsNormolize(context.curentData)

                        val curentData: String = context.curentData
                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            context.printInfoTermAndLoaging(command, progressUnit)
                            context.printInfoTermAndLoaging(curentData, progressUnit)
                        }

                        progressUnit += progressUnit

                    } else {
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.identifyDeviceFailed))
                        }
                        flagsSuccess = false
                        break
                    }
                }

                // отключение ат команд
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

                Thread.sleep(TIMEOUT_START_DEVICE)

                // прогресс на единицу то есть каждая команда сколько то процентов
                var progressUnit: Int = 100 / data.size

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // отправка всех настроек в устройство
                out@for ((key, value) in data) {

                    // очищение прошлых данных
                    context.curentData = ""

                    val dataSend: String = key + value
                    context.usb.writeDevice(dataSend, false)

                    // система получения ответа и ожидание полной отправки данных
                    var maxCntIter: Int = 10
                    while (context.curentData.isEmpty()) {
                        if (maxCntIter == 0) {
                            flagError = true
                            break@out
                        }
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                        maxCntIter--
                    }


                    var cnt: Int = context.curentData.length
                    while (context.curentData.isNotEmpty()) {
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                        val newCnt = context.curentData.length
                        if (cnt == newCnt) {
                            break
                        } else {
                            cnt = newCnt
                        }
                    }

                    // дополнительное услоие если не пришло не ERROR не OK
                    if (!context.curentData.contains(context.getString(R.string.error)) && !context.curentData.contains(context.getString(R.string.okSand))) {
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE * longSleepX * longSleepX)
                    }

                    // проверка принялись ли данные
                    if (context.curentData.isEmpty() ||
                        context.curentData.contains(context.getString(R.string.error)) ||
                        !context.curentData.contains(context.getString(R.string.okSand))) {

                        // если команда не входит в список команд которые не должны давать ответа то ерорим все
                        if (key != context.getString(R.string.commandSetResetModem)) {
                            flagError = true

                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(key + value +
                                        context.getString(R.string.errorSendDataWrite))
                            }
                            flagWorkWrite = false
                            break
                        }
                    }

                    val curentData: String = context.curentData
                    // вывод в загрузочное диалог информации
                    (context as Activity).runOnUiThread {
                        context.printInfoTermAndLoaging(key + value, progressUnit)
                        context.printInfoTermAndLoaging(curentData, progressUnit)
                    }
                    progressUnit += progressUnit

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
                    var maxCntIter: Int = 10
                    while (context.curentData.isEmpty()) {
                        if (maxCntIter == 0) {
                            flagsSuccess = true
                            break@out
                        }
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                        maxCntIter--
                    }


                    var cnt: Int = context.curentData.length
                    while (context.curentData.isNotEmpty()) {
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                        val newCnt = context.curentData.length
                        if (cnt == newCnt) {
                            break
                        } else {
                            cnt = newCnt
                        }
                    }

                    // нормаизируем данняе и разделяем
                    val data: List<String> = formatDataCommandsNormolize(context.curentData).split(",")

                    // после получения данных отправляем их на отображение
                    (context as Activity).runOnUiThread {
                        if (usbFragment is Enfora1318Fragment) {
                            try {
                                usbFragment.onPrintSignal(data[0], data[1])
                            } catch (e: Exception) {
                                flagsSuccess = true
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



    private fun formatDataCommandsNormolize(data: String): String {
        return data.substringAfter(": ").substringBefore("\n").dropLast(1)
    }

}