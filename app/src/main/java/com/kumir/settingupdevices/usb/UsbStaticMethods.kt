package com.kumir.settingupdevices.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbStaticMethods {

    // метод для получения всех имен девайсов
    fun getAllDevices(context: Context): ArrayList<String> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager?
        val devices = usbManager?.deviceList

        val devicesString: ArrayList<String> = arrayListOf()

        devices.let { devices_ ->
            for ((_, usbDevice) in devices_!!) {
                devicesString.add(usbDevice.deviceName.toString())
            }
        }

        return devicesString
    }

    // метод для получения всех дивайсов
    fun getAllUsbDevices(context: Context): ArrayList<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager?
        val devices = usbManager?.deviceList

        val devicesRet: ArrayList<UsbDevice> = arrayListOf()
        devices?.map { devicesRet.add(it.value) }

        return devicesRet
    }

}