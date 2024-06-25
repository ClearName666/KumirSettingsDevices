package com.example.kumirsettingupdevices.filesMenager

/*
    [Network setting RM81]
    Mode — ROUTER, CANPROXY, RS485, MONITOR
    Band — 1) 864-865МГц», 2) 866-868МГц, 3) 868.7-869.2МГц
    Power
    AccessKe
*/
data class IniFilePmModel(
    val name: String = "",
    val mode: String = "",
    val band: String = "",
    val power: String = "",
    val accessKe: String = ""
)