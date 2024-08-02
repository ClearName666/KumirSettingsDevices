package com.example.kumirsettingupdevices.model.recyclerModel

class StSearchOneWire(var iLastDeviceFlag: Byte, var iLastDiscrepancy: Byte, var iLastFamilyDiscrepancy: Byte,
    val ROM: ByteArray) {

    fun onClearSearch() {
        iLastDeviceFlag = 0
        iLastDeviceFlag = 0
        iLastFamilyDiscrepancy = 0
    }
}