package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R

class UsbCommandsProtocol {

    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 30
    }


    // метод для получеия сериного номера и версии прошивки
    fun serinerNumberAndVersionFirmware(context: Context, usbFragment: UsbFragment) {
        // поток считывания сериного номера и прошивки
        Thread {

            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE*10)
            if (context is MainActivity) {

                // очищение прошлых данных
                context.curentData = ""

                // серйный номер-------------------------------------------------
                context.usb.writeDevice(context.getString(R.string.commandGetSerialNum),
                    false)

                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                // если данные поступили то выводим в серийный номер
                if (context.curentData.isNotEmpty()) {
                    (context as Activity).runOnUiThread {
                        val dataPrint: String =
                            context.getString(R.string.serinerNumber) +
                                    "\n" + formatDataCommandsNormolize(context.curentData)

                        (context as Activity).runOnUiThread {
                            usbFragment.printSerifalNumber(dataPrint)
                        }

                        context.curentData = ""
                    }
                } else {
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog(context.getString(R.string.identifyDeviceFailed))
                    }
                }

                // версия прошивки-------------------------------------------------
                context.usb.writeDevice(context.getString(R.string.commandGetVersionFirmware),
                    false)

                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                // если данные поступили то выводим в серийный номер
                if (context.curentData.isNotEmpty()) {
                    (context as Activity).runOnUiThread {
                        val dataPrint: String =
                            context.getString(R.string.versionProgram) +
                                    "\n" + formatDataCommandsNormolize(context.curentData)

                        (context as Activity).runOnUiThread {
                            usbFragment.printVersionProgram(dataPrint)
                        }

                        context.curentData = ""
                    }
                } else {
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog(context.getString(R.string.identifyDeviceFailed))
                    }
                }
            }
        }.start()
    }



    // метод для получения настроек устройства
    fun readSettingDevice(commands: List<String>, context: Context, usbFragment: UsbFragment) {
        val settingData: MutableMap<String, String> = mutableMapOf()

        Thread {
            if (context is MainActivity) {
                for (command in commands) {

                    // очищение прошлых данных
                    context.curentData = ""

                    context.usb.writeDevice(command, false)
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                    if (context.curentData.isNotEmpty()) {
                        settingData[command] = formatDataCommandsNormolize(context.curentData)
                    }
                }

                (context as Activity).runOnUiThread {
                    usbFragment.printSettingDevice(settingData)
                }
            }
        }.start()
    }

    private fun formatDataCommandsNormolize(data: String): String {
        return data.substringAfter(": ").substringBefore("\n")
    }

}