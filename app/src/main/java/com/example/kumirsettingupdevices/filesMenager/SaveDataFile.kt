package com.example.kumirsettingupdevices.filesMenager


import java.io.File
import java.io.IOException

// клас для сохранения файлов
class SaveDataFile {
    fun saveToExternalStorage(files: List<File>, outputDirectory: String): Boolean {
        val directory = File(outputDirectory)

        return try {
            if (!directory.exists()) { // если такой директории нет то создаем
                directory.mkdirs()
            }

            files.forEach { file ->
                val outputFile = File(directory, file.name)
                file.copyTo(outputFile, overwrite = true)
            }

            true
        } catch (e: IOException) {
            false
        }
    }
}