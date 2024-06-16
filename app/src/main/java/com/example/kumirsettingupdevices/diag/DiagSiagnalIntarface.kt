package com.example.kumirsettingupdevices.diag

interface DiagSiagnalIntarface {
    fun onErrorStopChackSignal()
    fun onPrintSignal(signal: String, errors: String)
}