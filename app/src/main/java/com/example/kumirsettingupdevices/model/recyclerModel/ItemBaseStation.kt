package com.example.kumirsettingupdevices.model.recyclerModel

class ItemBaseStation(val bss: String, val snr: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemBaseStation) return false

        return bss == other.bss /*&& snr == other.snr*/
    }

    override fun hashCode(): Int {
        return bss.hashCode()
    }
}