package com.example.kumirsettingupdevices.filesMenager


import java.io.File

class GenerationFiles() {
    // генерация ini файлов для экспорта
    fun generationIniFiles(data: List<IniFileModel>): List<File> {
        val listFile: MutableList<File> = mutableListOf()

        for (d in data) {
            val fileName = "${d.nameFile}.ini"

            val content = buildString {
                appendLine("[${d.type}]")
                appendLine("Apn=${d.apn}")
                appendLine("TCPPort=${d.tcpPort}")
                appendLine("eServer=${d.eServer}")
                appendLine("Password=${d.password}")
                appendLine("Login=${d.login}")
                appendLine("Keepalive=${d.keepalive}")
                appendLine("Timeout=${d.timeout}")
                appendLine("InitTime=${d.inittime}")
                appendLine("Wpwrup=${d.wpwrup}")
                appendLine("Pwrdntime=${d.pwrdntime}")
                appendLine("DevMode=${d.devMode}")
                appendLine("Activeport=${d.activeport}")
                appendLine("SMS=${d.sms}")
                appendLine("SMSPin=${d.smsPin}")
                appendLine("SIM=${d.sim}")
                appendLine("SIMPin=${d.simPin}")
                appendLine("Monitor=${d.monitor}")
            }

            val file = File(fileName)
            file.writeText(content)
            listFile.add(file)
        }

        return listFile
    }
}

