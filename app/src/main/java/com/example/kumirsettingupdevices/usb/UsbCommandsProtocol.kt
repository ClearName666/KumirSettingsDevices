package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R

class UsbCommandsProtocol {

    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 30
        const val TIMEOUT_START_DEVICE: Long = 1000
    }



    // метод для получения настроек устройства
    fun readSettingDevice(commands: List<String>, context: Context, usbFragment: UsbFragment) {
        val settingData: MutableMap<String, String> = mutableMapOf()
        var flagsSuccess: Boolean = true
        Thread {
            Thread.sleep(TIMEOUT_START_DEVICE)
            if (context is MainActivity) {

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

    fun writeSettingDevice(data: Map<String, String>, context: Context) {

        Thread {

            var flagError: Boolean = false

            Thread.sleep(TIMEOUT_START_DEVICE)
            if (context is MainActivity) {

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
                        context.curentData.contains(context.getString(R.string.error))) {
                        flagError = true

                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(key + value +
                                    context.getString(R.string.errorSendDataWrite))
                        }

                        break
                    }

                }

                if (!flagError) {
                    // сохранение данных AT$SAVE
                    context.usb.writeDevice(context.getString(R.string.commandSaveSettings), false)
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                }

                context.curentData = ""

                // включение ат команд
                context.usb.flagAtCommandYesNo = true
            }
        }.start()
    }


    private fun formatDataCommandsNormolize(data: String): String {
        return data.substringAfter(": ").substringBefore("\n")
    }

}