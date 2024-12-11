package com.kumir.settingupdevices.diag

interface DiagFragmentInterface {
    fun runDiag()
    fun printVerAndSernum(version: String, SerialNum: String)
}