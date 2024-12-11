package com.kumir.settingupdevices.usbFragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.common.base.Charsets
import com.kumir.settingupdevices.LoadInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentFirmwareSTMBinding
import com.kumir.settingupdevices.usb.StmLoader
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.use


class FirmwareSTMFragment(private val contextMain: MainActivity): Fragment(), UsbFragment,
    LoadInterface {

    override val usbCommandsProtocol: UsbCommandsProtocol = UsbCommandsProtocol()

    private lateinit var stmLoader: StmLoader
    private lateinit var binding: FragmentFirmwareSTMBinding


    val listDeviceModel: List<String> = listOf(
        "m32_c",
        "m32_lite",
        "m32_d"
    )

    companion object {
        const val URL_SERVER: String = "http://192.168.0.29"

    }

    // коды ошибок
    enum class ErrorUpdate {SERVER, FILES, VALID, SERVER_ERROR}

    var flagCancellation: Boolean = false

    override fun onDestroyView() {
        contextMain.usb.onSerialSpeed(9) // 115200
        contextMain.usb.onSerialParity(0) // none

        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contextMain.usb.onSerialParity(1) // even
        contextMain.usb.onSelectUumBit(true) // 8 bit
        contextMain.usb.onSerialStopBits(0) // 1 bit

        binding = FragmentFirmwareSTMBinding.inflate(inflater)

        // клик на запись
        binding.imageDownLoad.setOnClickListener {
            onClickWriteSettingsDevice(binding.imageDownLoad)
        }

        // клик на то что перезкгрузил
        binding.buttonReboot.setOnClickListener {
            onClickResetButton()
        }

        // клик на кнопку отмены
        binding.buttonCancellation.setOnClickListener {
            onClickCancellation()
        }

        stmLoader = StmLoader(usbCommandsProtocol, requireContext() as MainActivity)


        // проверка обновлений на сервере кумир
        binding.buttonCheckUpdate.setOnClickListener {
            updateProgramsStm()
        }

        createAdapters()

        return binding.root
    }

    //------------------------------------НОВЫЙ ФУНКЦИОНАЛ------------------------------------------

    // функция для запуска потока обновления
    @SuppressLint("CommitPrefEdits")
    private fun updateProgramsStm() {
        binding.loadingMenuUpdate.visibility = View.VISIBLE
        Thread {
            // получаем список со всем файломи которые у нас есть
            val listFilesInInternalStorage = listFilesInInternalStorage(contextMain)
            val listUpdateFile: MutableSet<String> = mutableSetOf()
            val listDelFiles: MutableList<String> = mutableListOf()
            val mapVersionFile: MutableMap<String, String> = mutableMapOf()

            // проверка обновления для прошивки m32
            for (device in listDeviceModel) {
                val version = sendPostRequest("get_version", device)
                if (version != null) {
                    try {
                        // прверка все ли хорошо
                        if (version[0] == 0x00.toByte()) {

                            var flagPresence = false
                            var flagPresenceDeviceVersion = false
                            // поулчаем из байт данных версию в строке за сиключением 1 байта ошибки
                            val versionNew = version.drop(1).toByteArray().toString(Charsets.UTF_8)

                            for (fileName in listFilesInInternalStorage) {

                                if (fileName.contains(device.replace("_", ""))) {
                                    // проверка на соответствие версии
                                    if (!fileName.contains(versionNew)) {
                                        listDelFiles.add(fileName)
                                        flagPresence = true
                                    }
                                    flagPresenceDeviceVersion = true
                                }
                            }

                            // файл не найден или его надо обновить (обновляем)
                            if (flagPresence || !flagPresenceDeviceVersion) {
                                listUpdateFile.add(device)
                                mapVersionFile[device] = versionNew
                            }
                        } else {
                            contextMain.runOnUiThread {
                                binding.loadingMenuUpdate.visibility = View.GONE
                                errorPost(ErrorUpdate.VALID)
                            }
                            continue
                        }
                    } catch (_: Exception) {
                        contextMain.runOnUiThread {
                            binding.loadingMenuUpdate.visibility = View.GONE
                            errorPost(ErrorUpdate.SERVER)
                        }
                        continue
                    }
                } else {
                    contextMain.runOnUiThread {
                        binding.loadingMenuUpdate.visibility = View.GONE
                        errorPost(ErrorUpdate.SERVER)
                    }
                    continue
                }
            }

            // обновление всего что нужно
            for (device in listUpdateFile) {
                val programBootFileByte = sendPostRequest("get_boot", device)
                val programFlashFileByte = sendPostRequest("get_flash", device)

                // прверяем пришли ли даннные
                if (programBootFileByte != null && programFlashFileByte != null){
                    try {
                        // проверяем нету ли ошибок при отпрвки
                        if (programBootFileByte[0] == 0x00.toByte() && programFlashFileByte[0] == 0x00.toByte()) {

                            val fileVersionName = mapVersionFile[device]
                            if (fileVersionName != null) {

                                // сохраняем данные о адресе
                                try {
                                    val sharedPreferences = contextMain.getSharedPreferences("addressPrefs", Context.MODE_PRIVATE)
                                    val editor = sharedPreferences.edit()

                                    val addressBoot = bytesToIntLittleEndian(programBootFileByte, 1, 4)
                                    val addressFlash = bytesToIntLittleEndian(programFlashFileByte, 1, 4)

                                    editor.putInt("${device}_boot", addressBoot) // Сохраняем значение boot
                                    editor.putInt(device, addressFlash) // Сохраняем значение flash
                                } catch (_: Exception) {
                                    Log.e("errorMemory", "По какой то причине данне об адесе не смогли получиться из за чего было выбрано значение по умолчанию")
                                }

                                // сгенерированное название про версии для флеш и для бут
                                val fileNameFlash = "kumir_${device.replace("_", "")}_$fileVersionName.bin"
                                val fileNameBoot = "kumir_${device.replace("_", "")}_${fileVersionName}_boot.bin"

                                // записываем новые прошивки во внутренную память
                                // только если они не пустые
                                var flagSuc = false
                                if (programFlashFileByte.size > 15) {
                                    flagSuc = writeFileToInternalStorage(
                                        contextMain,
                                        fileNameFlash,
                                        programFlashFileByte.drop(6).toByteArray()
                                    )

                                }
                                if (programBootFileByte.size > 15) {
                                    flagSuc = writeFileToInternalStorage(
                                        contextMain,
                                        fileNameBoot,
                                        programBootFileByte.drop(6).toByteArray()
                                    )
                                }

                                // удаляем все старые файлы
                                if (flagSuc)
                                    for (fileDel in listDelFiles)
                                        deleteFileFromInternalStorage(contextMain, fileDel)

                            } else {
                                contextMain.runOnUiThread {
                                    binding.loadingMenuUpdate.visibility = View.GONE
                                    errorPost(ErrorUpdate.SERVER)
                                }
                                continue
                            }

                        } else {
                            contextMain.runOnUiThread {
                                binding.loadingMenuUpdate.visibility = View.GONE
                                errorPost(ErrorUpdate.SERVER_ERROR)
                            }
                            continue
                        }

                    } catch (_: Exception) {
                        contextMain.runOnUiThread {
                            binding.loadingMenuUpdate.visibility = View.GONE
                            errorPost(ErrorUpdate.SERVER)
                        }
                        continue
                    }
                }
            }
            contextMain.runOnUiThread {
                binding.loadingMenuUpdate.visibility = View.GONE
            }
        }.start()
    }

    // функция для преборазования адресса в инт
    fun bytesToIntLittleEndian(bytes: ByteArray, startIndex: Int, endIndex: Int): Int {
        var result = 0
        var shift = 0

        for (i in startIndex..endIndex) {
            result = result or (bytes[i].toInt() and 0xFF shl shift)
            shift += 8
        }
        return result
    }

    // функция для обработки ошибок при обнавлении
    private fun errorPost(codeError: ErrorUpdate) {
        when (codeError) {
            ErrorUpdate.SERVER -> showAlertDialog(getString(R.string.errorUpdateStmServer))
            ErrorUpdate.FILES -> showAlertDialog(getString(R.string.errorUpdateStmFile))
            ErrorUpdate.VALID -> showAlertDialog(getString(R.string.errorUpdateStmDataValid))
            ErrorUpdate.SERVER_ERROR -> showAlertDialog(getString(R.string.errorUpdateStmDataServerError))
        }
    }

    // чтение из файла
    private fun readFileFromInternalStorage(context: Context, fileName: String): File {
        val file = File(context.filesDir, fileName)
        return file
    }

    // получение названия файла для прошивки
    private fun parseStringFromBytes(programFileByte: ByteArray): String? {
        if (programFileByte.size < 7) {
            return null // Недостаточно данных в массиве
        }

        val length = programFileByte[5].toInt() and 0xFF // Беззнаковое преобразование
        val startIndex = 6

        if (programFileByte.size < startIndex + length) {
            return null // Недостаточно байтов для строки
        }

        val byteArrayForString = programFileByte.copyOfRange(startIndex, startIndex + length)
        return byteArrayForString.toString(Charsets.UTF_8) // Преобразование в строку
    }

    // функция для получения списка файлов с внутреннего хранилищя (прошивки)
    private fun listFilesInInternalStorage(context: Context): List<String> {
        val directory = context.filesDir // Директория внутреннего хранилища
        val files = directory.listFiles() // Получаем список файлов в директории

        return files?.map { it.name } ?: emptyList() // Возвращаем список названий файлов
    }


    // функция для записи файлы
    private fun writeFileToInternalStorage(context: Context, fileName: String, content: ByteArray): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            file.writeBytes(content) // Записываем данные в файл
            true // Успешная запись
        } catch (e: Exception) {
            e.printStackTrace()
            false // Ошибка записи
        }
    }

    // функция для удаления файла
    private fun deleteFileFromInternalStorage(context: Context, fileName: String?): Boolean {
        return try {
            if (fileName == null) false

            val file = File(context.filesDir, fileName!!)
            if (file.exists()) {
                file.delete() // Удаляем файл
            } else {
                false // Файл не найден
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false // Ошибка удаления
        }
    }

    // функция для запроса
    private fun sendPostRequest(operation: String, devName: String): ByteArray? {
        val url = URL(URL_SERVER)
        var connection: HttpURLConnection? = null
        return try {
            // Открываем соединение
            connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "*/*")
            }

            // Формируем тело запроса
            val postData = "operation=$operation&dev_name=$devName"
            connection.outputStream.use { output ->
                output.write(postData.toByteArray(Charsets.UTF_8))
                output.flush()
            }

            // Читаем ответ
            val responseStream = connection.inputStream
            responseStream.readBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }


    //----------------------------------------------------------------------------------------------



    // создание адаптеров для выбора устройства
    private fun createAdapters() {

        // адаптер для выбора скорости
        val itemSelectSpeed = listOf(
            getString(R.string.speed_115200),
            getString(R.string.speed_230400)
        )
        // адаптер для выбора четности
        val itemSelectDevice = listOf(
            getString(R.string.m32Version3),
            getString(R.string.m32Version4),
            getString(R.string.m32lite)
        )

        val adapterSelectDevice = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectDevice)
        val adapterSelectSpeed= ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed)


        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectDevice.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerDevice.adapter = adapterSelectDevice
    }

    private fun onClickWriteSettingsDevice(view: View) {
        writeSettingStart()
    }

    override fun writeSettingStart() {

        // установка выбраной скоости
        contextMain.usb.onSerialSpeed(binding.spinnerSpeed.selectedItemPosition + 9)

        // открываем загрузочное меню
        binding.fonLoading.visibility = View.VISIBLE
        binding.loading.visibility = View.VISIBLE
        binding.infoLoading.visibility = View.VISIBLE


        // поток для записи
        Thread {
            Log.d("loadFileStm", "Начало загрузки")

            try {
                val bootloaderFile = getTempFileFromAssets(requireContext(), binding.spinnerDevice.selectedItemPosition, true)
                val programFile = getTempFileFromAssets(requireContext(), binding.spinnerDevice.selectedItemPosition, false)

                // вывод версии прошивки
                contextMain.runOnUiThread {
                    showVersionProgram(if (binding.spinnerDevice.selectedItemPosition == 1) "7_2_6_6409"
                    else if (binding.spinnerDevice.selectedItemPosition == 2) "7_1_1_5260" else
                    "7_1_1_6291")
                }


                Log.d("loadFileStm", "Файлы получены: $bootloaderFile, $programFile \n Начало установки файлов")


                // в зависемости от остоятельств запускаем в том или ином режиме
                if (!stmLoader.loadFile(
                        bootloaderFile, programFile,
                        getAddressProgram(true), getAddressProgram(false),
                        (bootloaderFile.length().toInt() / 1024), (programFile.length().toInt() / 1024),
                        this,
                        binding.spinnerDevice.selectedItemPosition == 1,
                        this
                    )
                ) {
                    Log.d("loadFileStm", "Не успешно")
                }

            } catch (e: Exception) {
                closeMenuProgress()
                Log.e("loadFileStm", e.message.toString())
            }



            Log.d("loadFileStm", "Завершино")

        }.start()
    }


    private fun getCurrentDevice(): String? {
        val positionDevice =  binding.spinnerDevice.selectedItemPosition

        return when (positionDevice) {
            0 -> "m32c"
            1 -> "m32d"
            2 -> "m32_lite"
            else -> null
        }
    }

    private fun getAddressProgram(boot: Boolean): Int {

        try {
            val device = getCurrentDevice()
            if (device != null) {
                val sharedPreferences = contextMain.getSharedPreferences("addressPrefs", Context.MODE_PRIVATE)
                val address = sharedPreferences.getInt(if (boot) device + "_boot" else device, 0)
                if (address != 0) return address
            }
        } catch (_: Exception) {
            Log.e("errorMemory", "По какой то причине данне об адесе не смогли получиться из за чего было выбрано значение по умолчанию")
        }



        return if (binding.spinnerDevice.selectedItemPosition == 0) {
            if (boot) 0x08000000
            else 0x08080000
        } else {
            if (boot) 0x08000000
            else 0x08008000
        }
    }


    private fun getTempFileFromAssets(context: Context, numberDev: Int, bootLoader: Boolean): File {


        // первым делом изщем во внутренних файлах
        val listFilesInInternalStorage = listFilesInInternalStorage(contextMain)
        if (listFilesInInternalStorage.isNotEmpty()) {
            when (numberDev) {
                0 -> {
                    for (fileName in listFilesInInternalStorage) {
                        if (!bootLoader && fileName.contains("m32c") && !fileName.contains("_boot")) {
                            val tempFile = File.createTempFile("file", null, context.cacheDir)

                            // Записываем данные из InputStream во временный файл
                            FileOutputStream(tempFile).use { output ->
                                val readFileFromInternalStorage = readFileFromInternalStorage(contextMain, fileName)
                                readFileFromInternalStorage.inputStream().copyTo(output)
                            }
                            return tempFile
                        }
                        if (bootLoader && fileName.contains("m32c") && fileName.contains("_boot")) {
                            val tempFile = File.createTempFile("file", null, context.cacheDir)

                            // Записываем данные из InputStream во временный файл
                            FileOutputStream(tempFile).use { output ->
                                val readFileFromInternalStorage = readFileFromInternalStorage(contextMain, fileName)
                                readFileFromInternalStorage.inputStream().copyTo(output)
                            }
                            return tempFile
                        }
                    }
                }
                1 -> {
                    for (fileName in listFilesInInternalStorage) {
                        if (!bootLoader && fileName.contains("m32d") && !fileName.contains("_boot")) {
                            val tempFile = File.createTempFile("file", null, context.cacheDir)

                            // Записываем данные из InputStream во временный файл
                            FileOutputStream(tempFile).use { output ->
                                val readFileFromInternalStorage = readFileFromInternalStorage(contextMain, fileName)
                                readFileFromInternalStorage.inputStream().copyTo(output)
                            }
                            return tempFile
                        }
                        if (bootLoader && fileName.contains("m32d") && fileName.contains("_boot")) {
                            val tempFile = File.createTempFile("file", null, context.cacheDir)

                            // Записываем данные из InputStream во временный файл
                            FileOutputStream(tempFile).use { output ->
                                val readFileFromInternalStorage = readFileFromInternalStorage(contextMain, fileName)
                                readFileFromInternalStorage.inputStream().copyTo(output)
                            }
                            return tempFile
                        }
                    }
                }
                2 -> {
                    for (fileName in listFilesInInternalStorage) {
                        if (!bootLoader && fileName.contains("m32_lite") && !fileName.contains("_boot")) {
                            val tempFile = File.createTempFile("file", null, context.cacheDir)

                            // Записываем данные из InputStream во временный файл
                            FileOutputStream(tempFile).use { output ->
                                val readFileFromInternalStorage = readFileFromInternalStorage(contextMain, fileName)
                                readFileFromInternalStorage.inputStream().copyTo(output)
                            }
                            return tempFile
                        }
                        if (bootLoader && fileName.contains("m32_lite") && fileName.contains("_boot")) {
                            val tempFile = File.createTempFile("file", null, context.cacheDir)

                            // Записываем данные из InputStream во временный файл
                            FileOutputStream(tempFile).use { output ->
                                val readFileFromInternalStorage = readFileFromInternalStorage(contextMain, fileName)
                                readFileFromInternalStorage.inputStream().copyTo(output)
                            }
                            return tempFile
                        }
                    }
                }
                else -> {}
            }
        }


        // Открываем файл из assets
        val inputStream: InputStream? =
            if (numberDev == 2 && bootLoader) resources.openRawResource(R.raw.kumir_m32_lite_boot)
            else if (numberDev == 2 && !bootLoader) resources.openRawResource(R.raw.kumir_m32_lite_7_1_1_5260)
            else if (numberDev == 0 && bootLoader) resources.openRawResource(R.raw.kumir_m32_7_1_1_6291_boot)
            else if (numberDev == 0 && !bootLoader) resources.openRawResource(R.raw.kumir_m32_7_1_1_6291)
            else if (numberDev == 1 && bootLoader) resources.openRawResource(R.raw.kumir_m32d_7_2_6_6409_boot)
            else if (numberDev == 1 && !bootLoader) resources.openRawResource(R.raw.kumir_m32d_7_2_6_6409)
            else null

        // Создаем временный файл
        val tempFile = File.createTempFile("file", null, context.cacheDir)

        // Записываем данные из InputStream во временный файл
        FileOutputStream(tempFile).use { output ->
            inputStream?.copyTo(output)
        }

        return tempFile
    }


    // контроль отсоединения кабеля
    override fun lockFromDisconnected(connect: Boolean) {
        // текстрки для кнопок
        val drawablImageDownLoad = ContextCompat.getDrawable(requireContext(), R.drawable.download)

        if (!connect) {
            //------------------------------------------------------------------------------------------
            // покраска кнопки записи в серый
            // Обертываем наш Drawable для совместимости и изменяем цвет

            drawablImageDownLoad?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GRAY)
                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }

            binding.imageDownLoad.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        } else {
            // -------------активайия кнопки после прочтения-------------
            // перекраска в красный цвет кнопки загрузки
            val drawablImageDownLoad = ContextCompat.getDrawable(requireContext(), R.drawable.download)
            drawablImageDownLoad?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.RED)
                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }

            // только после чтения
            binding.imageDownLoad.setOnClickListener {
                onClickWriteSettingsDevice(it)
            }
        }
    }

    private fun showAlertDialog(text: String) {
        contextMain.showAlertDialog(text)
    }

    // методы для управления отображением статуса прошивки
    override fun loadingProgress(prgress: Int) {
        binding.progressBarLoading.progress = prgress
    }

    // закрытие меню загрузки прошивки
    override fun closeMenuProgress() {
        binding.fonLoading.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.infoLoading.visibility = View.GONE

        // возвращяем текст обратно
        binding.textCurrentTask.text = getString(R.string.currentTask)
    }

    override fun errorSend() {
        closeMenuProgress()
        loadingProgress(0)
        binding.buttonReboot.visibility = View.GONE

        if (!flagCancellation)
            showAlertDialog(getString(R.string.errorLoadStm))
        else
            flagCancellation = false

    }

    // требование перезагрузить устройство
    fun needResetPlease() {
        showAlertDialog(getString(R.string.pleaseResetStm203))
        binding.buttonReboot.visibility = View.VISIBLE
    }

    // нажатие на кнопку что бы уведомить о перезагрузки
    private fun onClickResetButton()  {
        stmLoader.flagResetOk = true
        binding.buttonReboot.visibility = View.GONE
    }

    // метод для отмены операции зашивания
    private fun onClickCancellation() {
        flagCancellation = true
    }

    fun currentTaskFlash(text: String) {
        binding.textCurrentTask.text = text
    }

    // вывод версии bootloader
    fun showBootLoaderVersion(versionBootLoader: String) {
        binding.infoLayoutStm.visibility = View.VISIBLE
        binding.textVersionBootLoader.text = getString(R.string.versionBootloader) + " \n" +
                versionBootLoader
    }

    fun showVersionProgram(versionProgram: String) {
        binding.infoLayoutStm.visibility = View.VISIBLE
        binding.textThisVersionProgram.text = getString(R.string.loadProgramVersion) + "\n" +
                versionProgram
    }


    // неиспользуемые методы
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}
    override fun printSettingDevice(settingMap: Map<String, String>) {}
    override fun readSettingStart() {}

}