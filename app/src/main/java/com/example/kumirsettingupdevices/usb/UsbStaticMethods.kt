package com.example.kumirsettingupdevices.usb

import android.content.Context
import android.hardware.usb.UsbManager

class UsbStaticMethods {

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

}