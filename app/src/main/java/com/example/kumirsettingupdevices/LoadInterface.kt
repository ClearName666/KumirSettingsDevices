package com.example.kumirsettingupdevices

interface LoadInterface {
    fun loadingProgress(prgress: Int)
    fun closeMenuProgress()
    fun errorCloseMenuProgress()
    fun errorSend()
}