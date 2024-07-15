package com.example.kumirsettingupdevices.usbFragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.EditDelIntrface
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemAbanentAdapter.ItemAbanentAdapter
import com.example.kumirsettingupdevices.databinding.FragmentP101Binding
import com.example.kumirsettingupdevices.formaters.FormatDataProtocol
import com.example.kumirsettingupdevices.formaters.ValidDataSettingsDevice
import com.example.kumirsettingupdevices.model.recyclerModel.ItemAbanent
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment

class P101Fragment : Fragment(), UsbFragment, EditDelIntrface<ItemAbanent> {

    private lateinit var binding: FragmentP101Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    private var listKeyAbanents: MutableList<String> = mutableListOf()
    private var flagRead: Boolean = false

    private var flagReAbanents: Boolean = false


    // хранит всех абанентов
    val itemsAbonents: MutableList<ItemAbanent> = mutableListOf()

    // хранит текущего изменемого абанента
    var curentAbanent: ItemAbanent? = null

    companion object {
        private const val DEFFAULT_NUM_DEVICE: String = "234"
        private const val DEFFAULT_PASSWORD: String = ""
        private const val DEFFAULT_ADRES: String = "0"
        private const val DEFFAULT_TIMEOUT: String = "10"
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

        binding.P101.visibility = View.VISIBLE
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.mainFragmentWork(false)
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


            binding.textSizeMember.text = getString(R.string.sizeMemberTitle) +
                    "\n" + settingMap[getString(R.string.commandGetFspace)]

            binding.textDriverVersion.text = getString(R.string.driverTitle) +
                    "\n" + settingMap[getString(R.string.commandGetDriver)]?.
            substringAfter("DRIVER: ")?.substringBefore("\n")

            // запись абанентов в лист
            val listAbanent: List<String> = settingMap[getString(R.string.commandGetAbanents)]?.replace("OK", "")?.split("\n")!!
            listKeyAbanents = listAbanent.map { it.substringAfter("ABONENT: ") }.toMutableList()



            // добавления адептера в выборку драйвера
            val listDriverStr: List<String> = settingMap[getString(R.string.commandGetDriver)]?.
                replace("OK", "")?.split("\n")!!
                .filter { it.trim().isNotEmpty() }

            val itemDrivers = listDriverStr.map { it.substringAfter("DRIVER: ").substringBefore(".") }

            val adapterDrivers = ArrayAdapter(requireContext(),
                R.layout.item_spinner, itemDrivers)
            adapterDrivers.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerDriver.adapter = adapterDrivers

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


        return true
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

    override fun del(data: ItemAbanent) {
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

    override fun edit(data: ItemAbanent) {
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

            binding.spinnerSpeed.setSelection(formatDataProtocol.getSpeedIndax(ports[0]+2))
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
            binding.inputNumDevice.setText(data.values.substringAfter("d=").substringBefore(";").trim())
        } else {
            binding.inputNumDevice.setText(DEFFAULT_NUM_DEVICE)
        }

        if (data.values.contains("p=")) {
            if (!data.values.contains("p=a")) {
                binding.inputPassword.setText(data.values.substringAfter("p=").substringBefore(";").trim().dropLast(1))
            } else {
                binding.inputPassword.setText(DEFFAULT_PASSWORD)
            }
        } else {
            binding.inputPassword.setText(DEFFAULT_PASSWORD)
        }

        if (data.values.contains("n=")) {
            binding.inputAdress.setText(data.values.substringAfter("n=").substringBefore(";").trim())
        } else {
            binding.inputAdress.setText(DEFFAULT_ADRES)
        }

        if (data.values.contains("t=")) {
            binding.inputValues.setText(data.values.substringAfter("t=").substringBefore(";").trim())
        } else {
            binding.inputValues.setText(DEFFAULT_TIMEOUT)
        }

        // загружаем абанента в текущие
        curentAbanent = data
    }
}