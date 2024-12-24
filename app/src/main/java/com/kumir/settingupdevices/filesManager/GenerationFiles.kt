package com.kumir.settingupdevices.filesManager


import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log

class GenerationFiles() {
    // генерация ini файлов для экспорта
    fun generationIniFiles(data: List<IniFileModel>, dirMain: String, context: Context): Boolean {
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

            try {
                saveFileToDownloadFolderQ(fileName, content, context, dirMain)
            } catch (e: Exception) {
                Log.d("listIniDataPreset", e.message.toString())
                return false
            }

        }

        return true
    }

    // генерация ini файлов для экспорта
    fun generationIniFilesPm(
        data: List<IniFilePmModel>,
        dirMain: String,
        context: Context
    ): Boolean {
        for (d in data) {
            val fileName = "${d.name}.ini"

            val content = buildString {
                appendLine("[Network setting RM81}]")
                appendLine("Mode=${d.mode}")
                appendLine("Band=${d.band}")
                appendLine("Power=${d.power}")
                appendLine("AccessKe=${d.accessKe}")
            }

            try {
                saveFileToDownloadFolderQ(fileName, content, context, dirMain)
            } catch (e: Exception) {
                Log.d("IniFilePmModel", e.message.toString())
                return false
            }

        }

        return true
    }

    fun createFile(content: String, fileName: String, context: Context, dirMain: String): Boolean {
        return try {
            saveFileToDownloadFolderQ(fileName, content, context, dirMain)
            true
        } catch (e: Exception) {
            Log.d("IniFilePmModel", e.message.toString())
            false
        }

    }

    private fun saveFileToDownloadFolderQ(fileName: String, fileContent: String, context: Context, dirMain: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName/*.replace(".ini", "")*/)
            put(MediaStore.MediaColumns.MIME_TYPE, "pplication/octet-strea")
            put(MediaStore.MediaColumns.RELATIVE_PATH, dirMain)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI/*MediaStore.Files.getContentUri("external")*/, contentValues)
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                outputStream?.write(fileContent.toByteArray())
            }
        }
    }
}

