package com.kumir.settingupdevices.diag

interface DiagSiagnalIntarface {
    fun onErrorStopChackSignal()
    fun onPrintSignal(signal: String, errors: String)
    fun onPrintIP(ip: String)
}