package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R

class UsbCommandsProtocol {

    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 50
    }

    fun serinerNumberAndVersionFirmware(context: Context, usbFragment: UsbFragment) {
        // поток считывания сериного номера и прошивки
        Thread {
            if (context is MainActivity) {

                // очищение прошлых данных
                context.curentData = ""

                // серйный номер-------------------------------------------------
                context.usb.writeDevice(context.getString(R.string.conmmandGetSerialNum),
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
                context.usb.writeDevice(context.getString(R.string.conmmandGetVersionFirmware),
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
    private fun formatDataCommandsNormolize(data: String): String {
        return data.substringAfter(": ").substringBefore("\n")
    }
}