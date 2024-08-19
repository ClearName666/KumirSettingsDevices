package com.example.kumirsettingupdevices.usbFragments

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.EditDelIntrface
import com.example.kumirsettingupdevices.LoadInterface
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemAbanentAdapter.ItemAbanentAdapter
import com.example.kumirsettingupdevices.databinding.FragmentP101Binding
import com.example.kumirsettingupdevices.formaters.FormatDataProtocol
import com.example.kumirsettingupdevices.formaters.ValidDataSettingsDevice
import com.example.kumirsettingupdevices.model.recyclerModel.ItemAbanent
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment
import com.example.kumirsettingupdevices.usb.XModemSender
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class P101Fragment : Fragment(), UsbFragment, EditDelIntrface<ItemAbanent>, LoadInterface {

    private lateinit var binding: FragmentP101Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    //private lateinit var serialPort: UsbSerialPort

    private var listKeyAbanents: MutableList<String> = mutableListOf()
    private var flagRead: Boolean = false

    private var flagReAbanents: Boolean = false
    private var flagLoadDriver: Boolean = false

    // список всех драйверов
    var itemDrivers: MutableList<String> = mutableListOf()


    // хранит всех абанентов
    val itemsAbonents: MutableList<ItemAbanent> = mutableListOf()

    // хранит текущего изменемого абанента
    var curentAbanent: ItemAbanent? = null

    var fileName: String = ""

    companion object {
        private const val DEFFAULT_NUM_DEVICE: String = "234"
        private const val DEFFAULT_PASSWORD: String = ""
        private const val DEFFAULT_ADRES: String = "0"
        private const val DEFFAULT_TIMEOUT: String = "10"

        private const val TIMEOUT_RESPONSE: Long = 100
    }
    private lateinit var file: File

    // получения файла
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

        uri?.let {
            val tempFile = createTempFileFromUri(it)
            fileName = getFileNameFromUri(it)
            if (tempFile != null && (
                        fileName.endsWith(".BIN") ||
                        fileName.endsWith(".bin") ||
                        fileName.contains(".bin") ||
                        fileName.contains(".BIN"))) {
                file = tempFile
                setModeXmodemDevice(fileName)
            } else {
                showAlertDialog("Файл не валидный")
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        val fileName = getFileNameFromUri(uri)
        val tempFile = File.createTempFile(fileName, null, requireContext().cacheDir)
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(tempFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input?.copyTo(output)
            }
        }

        return tempFile
    }


    // полуение имени файла по пути
    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = ""
        uri.let {
            val cursor = requireContext().contentResolver.query(it, null, null, null, null)
            cursor?.use {
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        }
        return fileName // что бы убрать ini в конце названия
    }

    private fun selectFile() {
        getContent.launch("*/*") // Можно указать конкретный тип файла, например, "application/octet-stream"
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentP101Binding.inflate(inflater)

        // назначение клика на меню что бы добавлять и удалять данные
        binding.fonWindowDarck.setOnClickListener {
            binding.editMenuAbanent.visibility = View.GONE
            binding.fonWindowDarck.visibility = View.GONE
            curentAbanent = null // для того что бы не удалять прсото так
        }
        // назначение кликак что бы добавлять абанента
        binding.buttonSave.setOnClickListener {
            writeSettingStart()
        }

        // нажатие на кнопку получить абанентов
        binding.buttonAddAbanent.setOnClickListener {
            getAbonents()
        }
        binding.buttonAddAbanent.visibility = View.GONE

        // если меню удаления то фон будет уберать ее
        binding.fonLoadDriver.setOnClickListener {
            if (binding.menuDelDrivers.visibility == View.VISIBLE) {
                binding.menuDelDrivers.visibility = View.GONE
                binding.fonLoadDriver.visibility = View.GONE
            }
        }

        // клик на кнопку удаления вывзов меню удаления
        binding.buttonDriversDel.setOnClickListener {
            binding.menuDelDrivers.visibility = View.VISIBLE
            binding.fonLoadDriver.visibility = View.VISIBLE
        }

        // клик на кнопку удаления драйвера
        binding.buttonDelDriver.setOnClickListener {
            try {
                delDriver(binding.spinnerDelDrivers.selectedItem.toString())
                binding.menuDelDrivers.visibility = View.GONE
                binding.fonLoadDriver.visibility = View.GONE
            } catch (e: Exception) {
                showAlertDialog(getString(R.string.nonSelectDriver))
            }

        }

        // клики для чтения и записи
        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice()
        }

        createAdapters()



        // активация чтения
        onClickReadSettingsDevice()
        binding.P101.visibility = View.GONE



        return binding.root
    }

    override fun onDestroyView() {

        val context: Context = requireContext()
        if (context is MainActivity) {
            context.mainFragmentWork(true)

            // возврат к at командам
            context.usb.setAtCommand(null)
        }

        super.onDestroyView()
    }

    private fun getAbonents() {
        val command: MutableList<String> = mutableListOf()

        for (itemAb in listKeyAbanents) {
            val key = itemAb.replace(" ", "").replace("\n", "").
            replace("\r", "")

            if (key.isNotEmpty()) {
                command.add(getString(R.string.commandSetAbonent) + key)
                command.add(getString(R.string.commandGetAdLoad))
                command.add(getString(R.string.commandGetAbView))
            }
        }

        if (command.isNotEmpty()) {
            usbCommandsProtocol.readSettingDevice(command, requireContext(), this, flagReadAbonentsP101 = true)
        } else {
            showAlertDialog(getString(R.string.notAnonents))
            // клик добавления абанента
            binding.buttonAddAbanent.text = getString(R.string.addAbanentTitle)
            binding.buttonAddAbanent.setOnClickListener {
                binding.inputKey.visibility = View.VISIBLE
                binding.fonWindowDarck.visibility = View.VISIBLE
                binding.editMenuAbanent.visibility = View.VISIBLE
            }
        }
        // кнопка загрузки драйвера
        binding.buttonLoadFile.visibility = View.VISIBLE
        binding.buttonLoadFile.setOnClickListener {
            selectFile() // выбор файла для загрузки драйвера
        }
    }

    private fun setModeXmodemDevice(name: String) {
        val context: Context = requireContext()

        if (context is MainActivity) {
            // очищение буфера перед отправкой
            context.curentDataByte = byteArrayOf()
            val dataMap: Map<String, String> = mapOf(
                getString(R.string.commandSetDriverMode) to name.substringBefore("_").uppercase()
            )

            usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false)

            // поток для отслеживания ответа от модема
            Thread {
                var cnt = 0
                while (true) {
                    cnt++
                    // если данные поступили то
                    if (context.curentDataByte.isNotEmpty()) {
                        (context as Activity).runOnUiThread {

                            // убераем кнопку уделения драйвера
                            binding.buttonDriversDel.visibility = View.GONE

                            // кнопка загрузки драйвера
                            binding.buttonLoadFile.visibility = View.VISIBLE
                            binding.buttonLoadFile.text = getString(R.string.loadDriverFin)
                            binding.buttonLoadFile.setOnClickListener {
                                if (!flagLoadDriver) {
                                    // открываем загрузочное меню
                                    binding.loadMenuProgress.visibility = View.VISIBLE
                                    binding.fonLoadDriver.visibility = View.VISIBLE

                                    loadDriver(file) // выбор файла для загрузки драйвера

                                }

                            }

                            // убераем возможность нажать на кнопку для редактирования пользователей
                            binding.buttonAddAbanent.visibility = View.GONE

                        }
                        break
                    }
                    // задержка
                    Thread.sleep(300)

                    // выход если слишком долго
                    if (cnt == 30) break
                }
            }.start()
        }
    }

    // вывод прогресса загрузки
    override fun loadingProgress(prgress: Int) {
        binding.progressBarLoad.progress = prgress
    }

    // закрытие меню прогресс бара
    override fun closeMenuProgress() {
        binding.loadMenuProgress.visibility = View.GONE
        binding.fonLoadDriver.visibility = View.GONE

        // переподключение
        Thread {
            Thread.sleep(3000)
            val context: Context = requireContext()
            if (context is MainActivity)
                context.usb.reconnect()
        }.start()
    }

    override fun errorCloseMenuProgress() {
        binding.loadMenuProgress.visibility = View.GONE
        binding.fonLoadDriver.visibility = View.GONE
    }

    override fun errorSend() {
        showAlertDialog(getString(R.string.errorSandDriver))
    }

    private fun loadDriver(file: File?) {
        val loadInterface = this
        Thread {
            val context: Context = requireContext()
            if (context is MainActivity) {

                // активируем флаг записи что бы не было возможности выйти
                usbCommandsProtocol.flagWorkWrite = true
                context.usb.flagAtCommandYesNo = false
                flagLoadDriver = true

                // пытаемся отправить
                try {
                    Log.d("XModemSender", "Создание класса XModemSender")
                    // создаем экземпляр класса для работы с xmodem
                    if (context.usb.usbSerialDevice != null) {
                        val sender = XModemSender(context.usb.usbSerialDevice, context, loadInterface)
                        Log.d("XModemSender", "Создан класс XModemSender")

                        try {
                            sender.sendFile(file)
                        } catch (e: IOException) {
                            Log.d("XModemSender", "IOException - sender.sendFile(file)")
                            (context as Activity).runOnUiThread {
                                showAlertDialog("Произошла ошибка!")
                            }
                        } finally {
                            Log.d("XModemSender", "1")
                            // драйвер появляется в названии драйвера
                            (context as Activity).runOnUiThread {
                                binding.textDriverVersion.text = getString(R.string.driverTitle) + "\n" + file?.name?.substringBefore(".")
                                Log.d("XModemSender", "2")
                                // возвраения возможности добавления абанентов
                                binding.buttonAddAbanent.visibility = View.VISIBLE
                                Log.d("XModemSender", "3")
                                // кнопка для установки дарайвера
                                binding.buttonLoadFile.text = getString(R.string.loadDriver)
                                binding.buttonLoadFile.setOnClickListener {
                                    selectFile() // выбор файла для загрузки драйвера
                                }
                                Log.d("XModemSender", "4")

                                // удаление драйвера
                                binding.buttonDriversDel.visibility = View.VISIBLE
                                Log.d("XModemSender", "5")

                                // обновление адаптеров отображения драйверов
                                itemDrivers.add(fileName.substringBefore("_").uppercase())
                                Log.d("XModemSender", "6")

                                updateDrivers()
                                Log.d("XModemSender", "7")

                            }

                            // загрузка завершина
                            usbCommandsProtocol.flagWorkWrite = false
                            context.usb.flagAtCommandYesNo = true
                            flagLoadDriver = false

                        }

                    } else {
                        Log.d("XModemSender", "serialPort = null")
                    }
                } catch (e: Exception) {
                    (context as Activity).runOnUiThread {
                        showAlertDialog("Произошла ошибка!")
                    }
                }

                // выход из режима загрузки драйверов
                /*if (!context.usb.writeDevice(context.getString(R.string.commandGetExitM32D))) {
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog("Ошибка выхода из режима загрузки драйверов перезегрузите модем!")
                    }
                }

                // загрузка завершина
                usbCommandsProtocol.flagWorkWrite = false
                context.usb.flagAtCommandYesNo = true
                flagLoadDriver = false*/
            }
        }.start()
    }


    private fun createAdapters() {

        // адаптер для выбора скорости
        val itemSelectSpeed = listOf(
            getString(R.string.speed_1200),
            getString(R.string.speed_2400),
            getString(R.string.speed_4800),
            getString(R.string.speed_9600),
            getString(R.string.speed_19200),
            getString(R.string.speed_38400),
            getString(R.string.speed_57600),
            getString(R.string.speed_115200)
        )
        // адаптер для выбора четности
        val itemSelectParity = listOf(
            getString(R.string.none),
            getString(R.string.even),
            getString(R.string.odd)
        )
        // адаптер для выбора стоп бит
        val itemSelectStopBit = listOf(
            getString(R.string.one),
            getString(R.string.two)
        )
        // адаптер дл я выбора битов данных
        val itemSelectBitData = listOf(
            getString(R.string.eight),
            getString(R.string.seven)
        )

        val adapterSelectSpeed = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed)
        val adapterSelectParity = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectParity)
        val adapterSelectStopBit = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectStopBit)
        val adapterSelectBitData = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectBitData)

        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectParity.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectStopBit.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectBitData.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerParity.adapter = adapterSelectParity
        binding.spinnerStopBit.adapter = adapterSelectStopBit
        binding.spinnerBitData.adapter = adapterSelectBitData
    }

    private fun onClickReadSettingsDevice() {
        readSettingStart()
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {

        //  если данных подоорительно мало то выходим скорее всего это
        if (settingMap[getString(R.string.commandGetSerialNum)] == null && settingMap[getString(R.string.commandGetAbView) + "1"] == null) return

        binding.P101.visibility = View.VISIBLE
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.mainFragmentWork(false)

            // сменя ат команд на другие
            context.usb.setAtCommand(getString(R.string.commandGetSerialNum))
        }



        // если чтение иначе вывод абанентов
        if (!flagRead) {
            binding.buttonAddAbanent.visibility = View.VISIBLE

            // делаем так что бы больше незя было прочитать устройство и нарушить алгаритм исполнения
            binding.imagedischarge.setOnClickListener {
                showAlertDialog(getString(R.string.readAlready))
            }
            // ------------------------------------------------------------

            // верийный номер и версия прошибки
            binding.serinerNumber.text = getString(R.string.serinerNumber) +
                    "\n" + settingMap[getString(R.string.commandGetSerialNum)]

            binding.textVersionFirmware.text = getString(R.string.versionProgram) +
                    "\n" + settingMap[getString(R.string.commandGetVersionFirmware)]


            binding.textSizeMember.text = getString(R.string.sizeMemberTitle) + " " + settingMap[getString(R.string.commandGetFspace)]

            binding.textDriverVersion.text = getString(R.string.driverTitle) +
                    "\n" + settingMap[getString(R.string.commandGetDriver)]?.
            substringAfter("DRIVER: ")?.substringBefore("\n")

            // запись абанентов в лист
            val listAbanent: List<String> = settingMap[getString(R.string.commandGetAbanents)]?.replace("OK", "")?.split("\n")!!
            listKeyAbanents = listAbanent.map { it.substringAfter("ABONENT: ") }.toMutableList()


            // выводим кнопку для удаления драйверов
            binding.buttonDriversDel.visibility = View.VISIBLE

            // добавления адептера в выборку драйвера
            val listDriverStr: List<String> = settingMap[getString(R.string.commandGetDriver)]?.
                replace("OK", "")?.split("\n")!!
                .filter { it.trim().isNotEmpty() }

            itemDrivers = listDriverStr.map { it.substringAfter("DRIVER: ").substringBefore(".") }.toMutableList()

            updateDrivers()

            // для контроля
            flagRead = true
        } else {
            // клик добавления абанента
            binding.buttonAddAbanent.text = getString(R.string.addAbanentTitle)
            binding.buttonAddAbanent.setOnClickListener {
                binding.inputKey.visibility = View.VISIBLE
                binding.fonWindowDarck.visibility = View.VISIBLE
                binding.editMenuAbanent.visibility = View.VISIBLE
            }


            // цикл перебирает количество абанентов что бы их потом прочесть и вывести
            for (i in 1..listKeyAbanents.size+1) {
                // вывод абанентов в список // только 1 абанент
                val ab: String? = settingMap[getString(R.string.commandGetAbView) + i.toString()]

                if (ab != null) {
                    itemsAbonents.add(
                        ItemAbanent(ab.substringAfter("ABONENT: ").substringBefore("\n"),
                            ab.substringAfter("ABNAME: ").substringBefore("\n"),
                            "",
                            ab.substringAfter("ABDRIVER: ").substringBefore("\n"),
                            ab.substringAfter("ABDEVID: ").substringBefore("\n"),
                            ab.substringAfter("ABPORT: ").substringBefore("\n"),
                            "",
                            "",
                            ab.substringAfter("ABPARAMS: ").substringBefore("\n"),
                            false
                        )
                    )
                }
            }

            val itemAbonentAdapter = ItemAbanentAdapter(requireContext(), itemsAbonents, this)
            binding.recyclerView.adapter = itemAbonentAdapter
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun updateDrivers() {
        val adapterDrivers = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemDrivers)
        adapterDrivers.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDriver.adapter = adapterDrivers
        binding.spinnerDelDrivers.adapter = adapterDrivers
    }


    override fun readSettingStart() {
        // если не читали до этого то читаем если нет то скорее всего жто добавление абанента и его нужно добавить
        if (!flagRead) {
            val command: List<String> = arrayListOf(
                getString(R.string.commandGetSerialNum),
                getString(R.string.commandGetVersionFirmware),
                getString(R.string.commandGetFspace),
                getString(R.string.commandGetAbanents),
                getString(R.string.commandGetAbView),
                getString(R.string.commandGetDevList)
            )

            usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
        } else {

            /*if (flagReAbanents) {
                getAbonents()
                flagReAbanents = false
            }*/

            val formatDataProtocol = FormatDataProtocol()

            var values: String = "a=t;"
            if (binding.inputPassword.text.toString().isNotEmpty()) {
                values += "p=${binding.inputPassword.text}a;"
            }
            if (binding.inputAdress.text.toString().isNotEmpty()) {
                values += "n=${binding.inputAdress.text};"
            }
            if (binding.inputValues.text.toString().isNotEmpty()) {
                values += "t=${binding.inputValues.text};"
            }
            values.dropLast(1) // убераем последний ";"

            itemsAbonents.add(
                ItemAbanent(
                    binding.inputKey.text.toString(),
                    binding.inputName.text.toString(),
                    "",
                    binding.spinnerDriver.selectedItem.toString(),
                    binding.inputNumDevice.text.toString(),
                    "${binding.spinnerSpeed.selectedItem}," +
                            "${binding.spinnerBitData.selectedItem}," +
                            "${formatDataProtocol.formatParityFromIndex(binding.spinnerParity.selectedItemPosition)}," +
                            "${binding.spinnerStopBit.selectedItem}," +
                            "${binding.inputRange.text}," +
                            "${binding.inputTimeOut.text}",
                    binding.inputPassword.text.toString(),
                    binding.inputAdress.text.toString(),
                    values,
                    false
                )
            )

            val itemAbonentAdapter = ItemAbanentAdapter(requireContext(), itemsAbonents, this)
            binding.recyclerView.adapter = itemAbonentAdapter
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

    }

    override fun writeSettingStart() {

        // если не валидно то
        if (!validAll()) return

        // если идет изменение то нужно удалит старую
        if (curentAbanent != null) {
            itemsAbonents.remove(curentAbanent)
            curentAbanent = null
        }

        // добавляем нового абанента в лист
        listKeyAbanents.add(binding.inputKey.text.toString())

        // закрфваем окно с редактированием абанента
        binding.fonWindowDarck.visibility = View.GONE
        binding.editMenuAbanent.visibility = View.GONE

        var values: String = "a=t;"
        if (binding.inputPassword.text.toString().isNotEmpty()) {
            values += "p=${binding.inputPassword.text}a;"
        }
        if (binding.inputAdress.text.toString().isNotEmpty()) {
            values += "n=${binding.inputAdress.text};"
        }
        if (binding.inputValues.text.toString().isNotEmpty()) {
            values += "t=${binding.inputValues.text};"
        }
        values.dropLast(1) // убераем последний ";"

        val formatDataProtocol = FormatDataProtocol()
        val dataMap: Map<String, String> = mapOf(
            getString(R.string.commandSetAbonent) to binding.inputKey.text.toString(),
            getString(R.string.commandGetAdLoad) to "",
            getString(R.string.commandSetAbonentName) to binding.inputName.text.toString(),
            getString(R.string.commandSetDriver) to binding.spinnerDriver.selectedItem.toString(),
            getString(R.string.commandSetDevId) to binding.inputNumDevice.text.toString(),
            getString(R.string.commandSetPortSet) to "${binding.spinnerSpeed.selectedItem}," +
                    "${binding.spinnerBitData.selectedItem}," +
                    "${formatDataProtocol.formatParityFromIndex(binding.spinnerParity.selectedItemPosition)}," +
                    "${binding.spinnerStopBit.selectedItem}," +
                    "${binding.inputRange.text}," +
                    "${binding.inputTimeOut.text}",
            getString(R.string.commandSetParams) to values,
            getString(R.string.commandAbSaveSettings) to ""
        )


        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false,
            flagRead=true)

    }

    override fun lockFromDisconnected(connect: Boolean) {
        // текстрки для кнопок
        val drawablImageDischarge = ContextCompat.getDrawable(requireContext(), R.drawable.discharge)

        if (!connect) {
            //------------------------------------------------------------------------------------------
            // покраска кнопки записи в серый
            // Обертываем наш Drawable для совместимости и изменяем цвет

            drawablImageDischarge?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GRAY)
                binding.imagedischarge.setImageDrawable(wrappedDrawable)
            }

            //--------------------------------------------------------------------------------------

            // убераем возмоэность читать и записывать
            binding.imagedischarge.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }

            binding.buttonDriversDel.visibility = View.GONE
            binding.buttonAddAbanent.visibility = View.GONE
            binding.buttonLoadFile.visibility = View.GONE

        } else {
            drawablImageDischarge?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GREEN)
                binding.imagedischarge.setImageDrawable(wrappedDrawable)
            }

            // установка клика
            binding.imagedischarge.setOnClickListener {
                onClickReadSettingsDevice()
            }

            binding.buttonDriversDel.visibility = View.VISIBLE
            binding.buttonAddAbanent.visibility = View.VISIBLE
            binding.buttonLoadFile.visibility = View.VISIBLE

            val context: Context = requireContext()
            if (context is MainActivity)
                context.usb.flagAtCommandYesNo = true
        }
    }

    private fun validAll(): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()
        if (!validDataSettingsDevice.validPasswordP101(binding.inputPassword.text.toString()) &&
            binding.inputPassword.text.toString().isNotEmpty()) {
            showAlertDialog(getString(R.string.notValidPasswordP101))
            return false
        }
        if (!validDataSettingsDevice.validRangeP101(binding.inputRange.text.toString()) ||
            binding.inputRange.text.toString().isEmpty()) {
            showAlertDialog(getString(R.string.notValidRangeP101))
            return false
        }
        if (!validDataSettingsDevice.validIdDeviceP101(binding.inputNumDevice.text.toString())) {
            showAlertDialog(getString(R.string.notIdDeviceP101))
            return false
        }
        if (!validDataSettingsDevice.validNameP101(binding.inputName.text.toString())) {
            showAlertDialog(getString(R.string.notValidNameP101))
            return false
        }
        if (binding.inputKey.text.toString().isEmpty()) {
            showAlertDialog(getString(R.string.notKeyValidP101))
            return false
        }
        if (binding.inputAdress.text.toString().isEmpty()) {
            showAlertDialog(getString(R.string.notAdresValidP101))
            return false
        }
        if (!validDataSettingsDevice.validTimeOutP101(binding.inputValues.text.toString()) &&
            binding.inputValues.text.toString().isNotEmpty()) {
            showAlertDialog(getString(R.string.notValidTimeOutP101))
            return false
        }
        if (!validDataSettingsDevice.validTimeP101(binding.inputTimeOut.text.toString())) {
            showAlertDialog(getString(R.string.notValidTimeP101))
            return false
        }

        if (itemDrivers.isEmpty()) {
            showAlertDialog(getString(R.string.notDriver))

            return false
        }


        return true
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

    override fun del(data: ItemAbanent) {
        val context: Context = requireContext()
        if (context is MainActivity && context.usb.checkConnectToDevice()) {
            val dataMap: Map<String, String> = mapOf(
                getString(R.string.commandSetDelAbonent) to data.num
            )

            usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false)

            // удаление в отобрадении
            itemsAbonents.remove(data)

            val itemAbonentAdapter = ItemAbanentAdapter(requireContext(), itemsAbonents, this)
            binding.recyclerView.adapter = itemAbonentAdapter
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

            flagReAbanents = true
        }




    }

    private fun delDriver(nameDriver: String) {
        val dataMap: Map<String, String> = mapOf(
            getString(R.string.commandSetDelDriver) to nameDriver
        )

        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false)

        // обновление адаптеров отображения драйверов
        itemDrivers.remove(nameDriver)
        updateDrivers()

        // удаление отображения наверху
        binding.textDriverVersion.text = getString(R.string.driverTitle)
    }

    override fun edit(data: ItemAbanent) {
        val context: Context = requireContext()
        if (context is MainActivity && context.usb.checkConnectToDevice()) {
            binding.fonWindowDarck.visibility = View.VISIBLE
            binding.editMenuAbanent.visibility = View.VISIBLE

            binding.inputKey.setText(data.num.trim())
            binding.inputName.setText(data.name.trim())
            binding.inputNumDevice.setText(data.numDevice.trim())

            // уубераем воможность редактировать ключ
            binding.inputKey.visibility = View.GONE

            // выводим информацию об порте
            val ports: List<String> = data.port.split(",")
            try {
                val formatDataProtocol = FormatDataProtocol()
                binding.inputRange.setText(ports[4])
                binding.inputTimeOut.setText(ports[5])

                binding.spinnerSpeed.setSelection(formatDataProtocol.getSpeedIndax(ports[0] + 2))
                binding.spinnerBitData.setSelection(formatDataProtocol.formatBitData(ports[1]))
                binding.spinnerParity.setSelection(formatDataProtocol.formatPatity(ports[2]))
                binding.spinnerStopBit.setSelection(formatDataProtocol.formatStopBit(ports[3]))

                // установка драйвера
                // пока нету
            } catch (_: Exception) {
                showAlertDialog(getString(R.string.errorUnknown))
            }

            /*
            Параметры:
                d=234 (234,236,204) - тип прибора, по умолчанию: 234
                p=222222h (в конце: h - hex, a - ascii) - пароль администратора,
                          по умолчанию: 222222h
                n=0 - сетевой адрес прибора, по умолчанию: 0
                a=t (t, f) - отображать дополнительные параметры или нет, по умолчанию: f
                t=10 - задержка (в секундах) для отображения значений,
                       по умолчанию: 10 секунд.
        */

            // выводим информацииюю об пареметрах

            if (data.values.contains("d=")) {
                binding.inputNumDevice.setText(
                    data.values.substringAfter("d=").substringBefore(";").trim()
                )
            } else {
                binding.inputNumDevice.setText(DEFFAULT_NUM_DEVICE)
            }

            if (data.values.contains("p=")) {
                if (!data.values.contains("p=a")) {
                    binding.inputPassword.setText(
                        data.values.substringAfter("p=").substringBefore(";").trim().dropLast(1)
                    )
                } else {
                    binding.inputPassword.setText(DEFFAULT_PASSWORD)
                }
            } else {
                binding.inputPassword.setText(DEFFAULT_PASSWORD)
            }

            if (data.values.contains("n=")) {
                binding.inputAdress.setText(
                    data.values.substringAfter("n=").substringBefore(";").trim()
                )
            } else {
                binding.inputAdress.setText(DEFFAULT_ADRES)
            }

            if (data.values.contains("t=")) {
                binding.inputValues.setText(
                    data.values.substringAfter("t=").substringBefore(";").trim()
                )
            } else {
                binding.inputValues.setText(DEFFAULT_TIMEOUT)
            }

            // загружаем абанента в текущие
            curentAbanent = data
        }
    }
}