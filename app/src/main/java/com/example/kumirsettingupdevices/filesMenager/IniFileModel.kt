package com.example.kumirsettingupdevices.filesMenager

data class IniFileModel(
    val nameFile: String,
    val type: String,
    val apn: String,
    val tcpPort: String,
    val eServer: String,
    val password: String,
    val login: String,
    val keepalive: String,
    val timeout: String,
    val inittime: String,
    val wpwrup: String,
    val pwrdntime: String,
    val devMode: String,
    val activeport: String,
    val sms: String,
    val smsPin: String,
    val sim: String,
    val simPin: String,
    val monitor: String
)
