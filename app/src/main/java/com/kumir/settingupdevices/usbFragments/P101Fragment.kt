package com.kumir.settingupdevices.usbFragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
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
import com.kumir.settingupdevices.EditDelIntrface
import com.kumir.settingupdevices.LoadInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.adapters.ItemAbanentAdapter.ItemAbanentAdapter
import com.kumir.settingupdevices.databinding.FragmentP101Binding
import com.kumir.settingupdevices.formaters.FormatDataProtocol
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.model.recyclerModel.ItemAbanent
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment
import com.kumir.settingupdevices.usb.XModemSender
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset

class P101Fragment : Fragment(), UsbFragment, EditDelIntrface<ItemAbanent>, LoadInterface {

    private lateinit var binding: FragmentP101Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    //private lateinit var serialPort: UsbSerialPort

    private var listKeyAbanents: MutableSet<String> = mutableSetOf()
    private var flagRead: Boolean = false

    private var flagReAbanents: Boolean = false
    private var flagLoadDriver: Boolean = false

    private var flagReconnect: Boolean = false

    // список всех драйверов
    var itemDrivers: MutableSet<String> = mutableSetOf()


    // хранит всех абанентов
    val itemsAbonents: MutableList<ItemAbanent> = mutableListOf()

    // хранит текущего изменемого абанента
    var currentAbanent: ItemAbanent? = null

    var fileName: String = ""
    var nameDriver: String = "driver"

    // хранит текущее наименование драйвера которое хочет записать пользователь
    var nameCurrentLoadDriver = ""



    companion object {
        private const val DEFFAULT_NUM_DEVICE: String = "234"
        private const val DEFFAULT_PASSWORD: String = ""
        private const val DEFFAULT_ADRES: String = "0"
        private const val DEFFAULT_TIMEOUT: String = "10"

        // private const val TIMEOUT_RESPONSE: Long = 100
    }
    private lateinit var file: File

    // получения файла
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

        uri?.let {
            fileName = getFileNameFromUri(it)
            val tempFile = createTempFileFromUri(it)
             if (tempFile != null && (
                        fileName.endsWith(".BIN") ||
                        fileName.endsWith(".bin") ||
                        fileName.contains(".bin") ||
                        fileName.contains(".BIN"))) {
                file = tempFile

                 // выбор названия для загрузки драйвера
                 setNameDriver()

                //setModeXmodemDevice(fileName)
            } else {
                if (!(requireContext() as MainActivity).usb.checkConnectToDevice())
                    showAlertDialog(getString(R.string.noConnected))

                showAlertDialog(getString(R.string.notFormatFile))
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        val context: Context = requireContext()
        try {
            val tempFile = File.createTempFile(fileName, null, context.cacheDir)
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input?.copyTo(output)
                }
            }

            return tempFile

        } catch (e: Exception) {
            Log.d("myErrorFile", e.message.toString())
            return null
        }
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


    override fun onDestroyView() {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.usb.onSerialSpeed(9)
            context.usb.flagCode1251 = false
        }

        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentP101Binding.inflate(inflater)



        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.controller_p_101))

            context.usb.flagCode1251 = true
        }


        // назначения кнопки для завершения диагностики
        binding.buttonEndDiag.setOnClickListener {
            endDiag()
            endViewDiag()
        }

        // назначения клика на меню выбора названия имени
        val validDataSettingsDevice = ValidDataSettingsDevice()
        binding.buttonSetName.setOnClickListener {
            if (binding.inputSetName.text.toString().isNotEmpty() && validDataSettingsDevice.isAscii(
                binding.inputSetName.text.toString()
            )) {
                nameDriver = binding.inputSetName.text.toString().uppercase()
                setModeXmodemDevice(fileName)
            } else {
                showAlertDialog(getString(R.string.nameNotValid))
            }
        }

        binding.buttonLoadFile.setOnClickListener {
            selectFile() // выбор файла для загрузки драйвера
        }

        binding.buttonCancellationDriverWrite.setOnClickListener {
            binding.mainLayoutSetName.visibility = View.GONE
        }

        binding.mainLayoutSetName.setOnClickListener {
            binding.mainLayoutSetName.visibility = View.GONE
        }


        // назначение клика на меню что бы добавлять и удалять данные
        binding.fonWindowDarck.setOnClickListener {
            binding.editMenuAbanent.visibility = View.GONE
            binding.fonWindowDarck.visibility = View.GONE
            currentAbanent = null // для того что бы не удалять прсото так
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

        return binding.root
    }


    // хавершение диагностики
    private fun endDiag() {
        listKeyAbanents = mutableSetOf()
        flagRead = false
        flagReAbanents = false
        flagLoadDriver = false
        flagReconnect = false
        itemDrivers = mutableSetOf()
        itemsAbonents.clear()
        currentAbanent = null
        fileName = ""
        nameDriver = "driver"
        nameCurrentLoadDriver = ""


        // нажатие на кнопку получить абанентов
        binding.buttonAddAbanent.setOnClickListener {
            getAbonents()
        }

        binding.buttonLoadFile.setOnClickListener {
            selectFile() // выбор файла для загрузки драйвера
        }
    }

    // завершаем диагностику (отображение)
    private fun endViewDiag() {
        binding.buttonAddAbanent.text = getString(R.string.readAbanents)
        binding.buttonAddAbanent.visibility = View.GONE

        val itemAbonnentAdapter = ItemAbanentAdapter(requireContext(), mutableListOf(), this)
        binding.recyclerView.adapter = itemAbonnentAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())


        binding.serinerNumber.text = getString(R.string.serinerNumber)
        binding.textDriverVersion.text = getString(R.string.driverTitle)
        binding.textVersionFirmware.text = getString(R.string.versionProgram)
        binding.textSizeMember.text = getString(R.string.sizeMemberTitle)

        binding.textRead.visibility = View.VISIBLE
        binding.imagedischarge.visibility = View.VISIBLE
        binding.buttonEndDiag.visibility = View.GONE


        binding.buttonLoadFile.visibility = View.GONE
        binding.buttonLoadFile.text = getString(R.string.loadDriver)

        binding.buttonDriversDel.visibility = View.GONE

        binding.inputPassword.setText("")
        binding.inputAdress.setText("")
        binding.inputValues.setText("")
        binding.inputName.setText("")
        binding.inputRange.setText("")
        binding.inputKey.setText("")
        binding.inputNumDevice.setText("")
        binding.inputSetName.setText("")
        binding.inputTimeOut.setText("")

        binding.checkBoxHex.isChecked = false

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
            (requireContext() as MainActivity).usb.reconnectCDC()
            usbCommandsProtocol.readSettingDevice(command, requireContext(), this, flagReadAbonentsP101 = true)
        } else {
            showAlertDialog(getString(R.string.notAnonents))
            // клик добавления абанента
            binding.buttonAddAbanent.text = getString(R.string.addAbanentTitle)
            binding.buttonAddAbanent.setOnClickListener {
                binding.layoutInputKey.visibility = View.VISIBLE
                binding.fonWindowDarck.visibility = View.VISIBLE
                binding.editMenuAbanent.visibility = View.VISIBLE
            }
        }
    }

    private fun setNameDriver() {
        binding.mainLayoutSetName.visibility = View.VISIBLE
    }

    private fun setModeXmodemDevice(name: String) {

        // закрываем меню выбора имени
        binding.mainLayoutSetName.visibility = View.GONE

        val context: Context = requireContext()

        if (!(context as MainActivity).usb.checkConnectToDevice()) {
            showAlertDialog(getString(R.string.nonConnectAdapter))
            return
        }

        if (context is MainActivity) {
            // очищение буфера перед отправкой
            context.currentDataByteAll = byteArrayOf()
            val dataMap: Map<String, String> = mapOf(
                getString(R.string.commandSetDriverMode) to nameDriver/*name.substringBefore("_").uppercase()*/
            )

            // переподключения перед отправкой команды
            context.usb.reconnectCDC()
            usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false)

            // поток для отслеживания ответа от модема
            Thread {
                var cnt = 0
                while (true) {
                    cnt++
                    // если данные поступили то
                    if (context.currentDataByteAll.isNotEmpty()) {
                        (context as Activity).runOnUiThread {
                            // начать установку драйвера
                            if (!flagLoadDriver) {
                                (requireContext() as MainActivity).openCloseLoadingView(false)
                                binding.loadMenuProgress.visibility = View.VISIBLE
                                binding.fonLoadDriver.visibility = View.VISIBLE
                                loadDriver(file)
                            }
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
    }

    override fun errorSend() {
        showAlertDialog(getString(R.string.errorSandDriver))

        // закрытие окна и переполдключение
        loadingProgress(100)
        closeMenuProgress()

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
                val sender = XModemSender(context.usb.usbSerialDevice, context, loadInterface, nameCurrentLoadDriver)


                // попытка записать драйвер
                if (sender.sendFile(file)) {
                    // драйвер появляется в названии драйвера
                    (context as Activity).runOnUiThread {
                        binding.textDriverVersion.text = getString(R.string.driverTitle) + "\n" + file?.name?.substringBefore(".")

                        // обновление адаптеров отображения драйверов если запись была успешно
                        itemDrivers.add(nameDriver.uppercase())

                        updateDriversShowView()
                    }
                }

                // загрузка завершина
                usbCommandsProtocol.flagWorkWrite = false
                flagLoadDriver = false
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


        // если чтение иначе вывод абанентов
        if (!flagRead) {
            binding.buttonAddAbanent.visibility = View.VISIBLE

            // кнопка загрузки драйвера
            binding.buttonLoadFile.visibility = View.VISIBLE

            binding.textRead.visibility = View.GONE
            binding.imagedischarge.visibility = View.GONE
            binding.buttonEndDiag.visibility = View.VISIBLE
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
            listKeyAbanents = listAbanent.map { it.substringAfter("ABONENT: ") }.toMutableSet()


            // выводим кнопку для удаления драйверов
            binding.buttonDriversDel.visibility = View.VISIBLE

            // добавления адептера в выборку драйвера
            val listDriverStr: List<String> = settingMap[getString(R.string.commandGetDriver)]?.
                replace("OK", "")?.split("\n")!!
                .filter { it.trim().isNotEmpty() }

            itemDrivers = listDriverStr.map { it.substringAfter("DRIVER: ").substringBefore(".") }.toMutableSet()

            updateDriversShowView()

            // для контроля
            flagRead = true
        } else {
            // клик добавления абанента
            binding.buttonAddAbanent.text = getString(R.string.addAbanentTitle)
            binding.buttonAddAbanent.setOnClickListener {
                binding.layoutInputKey.visibility = View.VISIBLE
                binding.fonWindowDarck.visibility = View.VISIBLE
                binding.editMenuAbanent.visibility = View.VISIBLE
            }


            // цикл перебирает количество абанентов что бы их потом прочесть и вывести
            for (i in 1..listKeyAbanents.size+1) {
                // вывод абанентов в список // только 1 абанент
                val ab: String? = settingMap[getString(R.string.commandGetAbView) + i.toString()]

                if (ab != null) {
                    itemsAbonents.add(
                        ItemAbanent(ab.substringAfter("ABONENT: ").substringBefore("\n").trim(),
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

            // устанавливаем все которые возможны ключи для проверок
            addAllInListKeys()

            val itemAbonnentAdapter = ItemAbanentAdapter(requireContext(), itemsAbonents, this)
            binding.recyclerView.adapter = itemAbonnentAdapter
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun addAllInListKeys() {
        for (ab in itemsAbonents) {
            listKeyAbanents.add(ab.num)
        }
    }


    private fun updateDriversShowView() {



        // обновляем все спинеры где есть вывод драйверов
        val adapterDrivers = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemDrivers.toList())
        adapterDrivers.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDriver.adapter = adapterDrivers
        binding.spinnerDelDrivers.adapter = adapterDrivers
    }


    override fun readSettingStart() {

        // проверка на подлючение
        val context: MainActivity = requireContext() as MainActivity
        if (!context.usb.checkConnectToDevice()) return

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

            // переподлючаемся перед отправкой
            context.usb.reconnectCDC()
            usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
        } else {

            /*if (flagReAbanents) {
                getAbonents()
                flagReAbanents = false
            }*/

            val formatDataProtocol = FormatDataProtocol()

            var values: String = "a=t;"
            if (binding.inputPassword.text.toString().isNotEmpty()) {
                // тип пароля
                val passwordType = if (binding.checkBoxHex.isChecked) "h" else "a";
                values += "p=${binding.inputPassword.text}$passwordType;"
            }
            if (binding.inputAdress.text.toString().isNotEmpty()) {
                values += "n=${binding.inputAdress.text};"
            }
            if (binding.inputValues.text.toString().isNotEmpty()) {
                values += "t=${binding.inputValues.text};"
            }
            values.dropLast(1) // убераем последний ";"


            // проверка есть ли данные абонент
            for (obj in itemsAbonents) {
                if (obj.num == binding.inputKey.text.toString())  {
                    // удаляем старую версию перед тем как запихнуть новую
                    itemsAbonents.remove(obj)
                    break
                }
            }

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

            // устанавливаем все которые возможны ключи для проверок
            addAllInListKeys()

            val itemAbonentAdapter = ItemAbanentAdapter(requireContext(), itemsAbonents, this)
            binding.recyclerView.adapter = itemAbonentAdapter
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

    }

    override fun writeSettingStart() {

        // если не валидно то
        if (!validAll()) return

        // если идет изменение то нужно удалит старую
        if (currentAbanent != null) {
            itemsAbonents.remove(currentAbanent)
            currentAbanent = null
        }

        // добавляем нового абанента в лист
        if (binding.inputKey.text.toString() !in listKeyAbanents)
            listKeyAbanents.add(binding.inputKey.text.toString())

        // закрфваем окно с редактированием абанента
        binding.fonWindowDarck.visibility = View.GONE
        binding.editMenuAbanent.visibility = View.GONE

        var values = "a=f;"
        if (binding.switchAddParams.isChecked) values = "a=t;"
        if (binding.inputPassword.text.toString().isNotEmpty()) {
            // тип пароля
            val passwordType = if (binding.checkBoxHex.isChecked) "h" else "a";
            values += "p=${if (binding.checkBoxHex.isChecked) binding.inputPassword.text.toString().lowercase() else binding.inputPassword.text}$passwordType;"
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



        // переподключения перед отправкой команды
        (requireContext() as MainActivity).usb.reconnectCDC()
        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false,
            flagRead=true, flagCode1251=true)

    }


    private fun utf8ToWin1251(input: String): ByteArray {
        // Преобразуем строку в байты UTF-8
        val utf8Bytes = input.toByteArray(Charset.forName("Windows-1251"))

        return utf8Bytes
    }

    override fun lockFromDisconnected(connect: Boolean) {
        // текстрки для кнопок
        val drawablImageDischarge = ContextCompat.getDrawable(requireContext(), R.drawable.discharge)

        if (!connect && !flagReconnect) {
            //------------------------------------------------------------------------------------------
            // покраска кнопки записи в серый
            // Обертываем наш Drawable для совместимости и изменяем цвет

            drawablImageDischarge?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GRAY)
                binding.imagedischarge.setImageDrawable(wrappedDrawable)
            }

            //--------------------------------------------------------------------------------------

            // далеем возможность завершить диагностику
            binding.textRead.visibility = View.GONE
            binding.imagedischarge.visibility = View.GONE
            binding.buttonEndDiag.visibility = View.VISIBLE

            binding.buttonDriversDel.visibility = View.GONE
            binding.buttonAddAbanent.visibility = View.GONE
            binding.buttonLoadFile.visibility = View.GONE

            // кнопка для установки дарайвера
            binding.buttonLoadFile.text = getString(R.string.loadDriver)
            binding.buttonLoadFile.setOnClickListener {
                selectFile() // выбор файла для загрузки драйвера
            }

            // нажатие на кнопку получить абанентов
            binding.buttonAddAbanent.setOnClickListener {
                getAbonents()
            }

            flagRead = false

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
        }
    }

    private fun validAll(): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()
        if (!validDataSettingsDevice.validPasswordP101(binding.inputPassword.text.toString()) &&
            binding.inputPassword.text.toString().isNotEmpty() && !binding.checkBoxHex.isChecked) {
            showAlertDialog(getString(R.string.notValidPasswordP101))
            return false
        } else if (binding.checkBoxHex.isChecked && !validDataSettingsDevice.isValidHex(binding.inputPassword.text.toString()) && binding.inputPassword.text.toString().length < 12) {
            showAlertDialog(getString(R.string.notValidPasswordP101Hex))
            return false
        }
        if (!validDataSettingsDevice.validRangeP101(binding.inputRange.text.toString()) ||
            binding.inputRange.text.toString().isEmpty()) {
            showAlertDialog(getString(R.string.notValidRangeP101))
            return false
        }
        /*if (!validDataSettingsDevice.validIdDeviceP101(binding.inputNumDevice.text.toString())) {
            showAlertDialog(getString(R.string.notIdDeviceP101))
            return false
        }*/
        if (!validDataSettingsDevice.validNameP101(binding.inputName.text.toString())) {
            showAlertDialog(getString(R.string.notValidNameP101))
            return false
        }
        if (binding.inputKey.text.toString().isEmpty()) {
            showAlertDialog(getString(R.string.notKeyValidP101))
            return false
        }
        if (binding.inputAdress.text.toString().isEmpty()) {
            showAlertDialog(getString(R.string.notAddressValidP101))
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

        if (currentAbanent == null && binding.inputKey.text.toString() in listKeyAbanents) {
            showAlertDialog(getString(R.string.abonnentPresent))

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

    // перед удалением у полузователя буте справшиваться ч\хочет ли он удалить или нет
    override fun del(data: ItemAbanent) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(getString(R.string.delAbonent) + data.name)

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            delAbonent(data)
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

    }

    private fun delAbonent(data: ItemAbanent) {
        val context: Context = requireContext()
        if (context is MainActivity && context.usb.checkConnectToDevice()) {
            val dataMap: Map<String, String> = mapOf(
                getString(R.string.commandSetDelAbonent) to data.num
            )

            // переподлючаемся перед отправкой
            context.usb.reconnectCDC()
            usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false)

            // удаление в отобрадении и в листе
            listKeyAbanents.remove(data.num)
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

        val context: MainActivity = requireContext() as MainActivity

        // переподлючаемся перед отправкой
        context.usb.reconnectCDC()
        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this, false)

        // поток ожидающий пока завершится операция удаления и
        // потом смотрит успешна она или нет и в
        // зависимости от этого удаляет или не удаляет информацию в UI
        Thread {
            while (!usbCommandsProtocol.flagWorkWrite) { Thread.sleep(1)}
            while (usbCommandsProtocol.flagWorkWrite) {
                Thread.sleep(1)
                if (!usbCommandsProtocol.flagWorkWrite) {
                    break
                }
            }

            if (usbCommandsProtocol.flagSuccessfullyWrite) {
                (requireContext() as Activity).runOnUiThread {
                    // обновление адаптеров отображения драйверов
                    itemDrivers.remove(nameDriver)
                    updateDriversShowView()

                    // удаление отображения наверху
                    binding.textDriverVersion.text = getString(R.string.driverTitle)
                }
            }
        }.start()
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
            binding.layoutInputKey.visibility = View.GONE

            // выводим информацию об порте
            val ports: List<String> = data.port.split(",")
            try {
                val formatDataProtocol = FormatDataProtocol()
                binding.inputRange.setText(ports[4].trim())
                binding.inputTimeOut.setText(ports[5].trim())

                // установка флага о том что установлен hex пароль
                binding.checkBoxHex.isChecked = data.values.substringAfter("p=").substringBefore(";").length > 7 // 7 парог после которого идет хекс пароль



                // установка данных в спинер
                binding.spinnerSpeed.setSelection(formatDataProtocol.getSpeedIndax(ports[0]) - 2)
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
                binding.inputNumDevice.setText(data.numDevice)
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
            currentAbanent = data
        }
    }
}