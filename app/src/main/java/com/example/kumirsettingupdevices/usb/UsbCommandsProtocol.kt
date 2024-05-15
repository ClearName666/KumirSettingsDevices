package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R

class UsbCommandsProtocol {

    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 50
        const val TIMEOUT_START_DEVICE: Long = 1500
    }



    // метод для получения настроек устройства
    fun readSettingDevice(commands: List<String>, context: Context, usbFragment: UsbFragment) {
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

                // перебор всех команд и получение ответов устройства
                for (command in commands) {

                    // очищение прошлых данных
                    context.curentData = ""

                    context.usb.writeDevice(command, false)
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

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

    fun writeSettingDevice(data: Map<String, String>, context: Context, usbFragment: UsbFragment) {

        Thread {
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
                for ((key, value) in data) {

                    // очищение прошлых данных
                    context.curentData = ""

                    val dataSend: String = key + value
                    context.usb.writeDevice(dataSend, false)

                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                    // проверка принялись ли данные
                    if (context.curentData.isEmpty() ||
                        context.curentData.contains(context.getString(R.string.error)) ||
                        !context.curentData.contains(context.getString(R.string.okSand))) {
                        flagError = true

                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(key + value +
                                    context.getString(R.string.errorSendDataWrite))
                        }

                        break
                    }

                    val curentData: String = context.curentData
                    // вывод в загрузочное диалог информации
                    (context as Activity).runOnUiThread {
                        context.printInfoTermAndLoaging(key + value, progressUnit)
                        context.printInfoTermAndLoaging(curentData, progressUnit)
                    }
                    progressUnit += progressUnit

                }

                if (!flagError) {

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
        }.start()
    }


    private fun formatDataCommandsNormolize(data: String): String {
        return data.substringAfter(": ").substringBefore("\n").dropLast(1)
    }

}