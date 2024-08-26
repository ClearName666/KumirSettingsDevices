package com.kumir.settingupdevices

interface LoadInterface {
    fun loadingProgress(prgress: Int)
    fun closeMenuProgress()
    fun errorSend()
}