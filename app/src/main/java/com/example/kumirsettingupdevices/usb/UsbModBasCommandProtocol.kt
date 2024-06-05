package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity


class UsbModBasCommandProtocol {

    var flagWorkWrite: Boolean = false


    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 300
    }



    // метод для получения настроек устройства
    fun readSettingDevice(commands: List<ByteArray>, context: Context, usbFragment: UsbFragment) {
        val settingData: MutableMap<String, String> = mutableMapOf()

        Thread {
            if (context is MainActivity) {

                // открываем диалог с загрузочным меню
                (context as Activity).runOnUiThread {
                    context.openCloseLoadingView(true)
                    context.flagThreadSerialCommands = true // говорим что работает поток чтения
                }

                // прогресс на единицу то есть каждая команда сколько то процентов
                val progressUnit: Int = 100 / commands.size
                var prograss: Int = 0

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // перебор всех команд и получение ответов устройства
                for (command in commands) {
                    // прогресс продливается
                    prograss += progressUnit

                    // очищение прошлых данных
                    context.curentDataByte = byteArrayOf()

                    context.usb.writeDevice("", false, command)

                    // система получения ответа и ожидание полной отправки данных
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)


                    try {
                        settingData["0xA3"] = String(byteArrayOf(context.curentDataByte[1]), Charsets.US_ASCII)
                    } catch (e: Exception) {
                        settingData["0xA3"] = "0xA3"
                    }


                    // вывод в диалог
                    val curentData: String = context.curentDataByte.toString()
                    // вывод в загрузочное диалог информации
                    (context as Activity).runOnUiThread {
                        context.printInfoTermAndLoaging(command.toString(), prograss)
                        context.printInfoTermAndLoaging(curentData, prograss)
                    }
                }

                // включение ат команд
                context.usb.flagAtCommandYesNo = true

                context.curentData = ""
                context.curentDataByte = byteArrayOf()

                context.flagThreadSerialCommands = false // говорим что не работает поток чтения

                // окончание прогресса
                (context as Activity).runOnUiThread {
                    context.printInfoTermAndLoaging("", 100)
                    usbFragment.printSettingDevice(settingData)
                }
            }
        }.start()
    }
}