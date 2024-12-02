package com.kumir.settingupdevices.usbFragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.dataBasePreset.Pm
import com.kumir.settingupdevices.databinding.FragmentPM81Binding
import com.kumir.settingupdevices.modems.SettingsPm
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment

class PM81Fragment(val autoFlag: Boolean) : Fragment(), UsbFragment, PrisetFragment<Pm> {

    private lateinit var binding: FragmentPM81Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    private var NAME_TYPE_DEVICE = "KUMIR-RM81A READY"

    // сохраняем настроки для устройств  ТЕКУЩИЕ НАСТРОЙКИ
    var band: String = ""
    var netKey: String = ""
    var mode: String = ""

    // сохраненные предыжущие настройки
    val listOldPmSet: MutableList<SettingsPm> = mutableListOf(
        SettingsPm(0, "", 0, "", ""),
        SettingsPm(0, "", 0, "", "")
    )

    var cntWrite: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPM81Binding.inflate(inflater)

        createAdapters()

        controlSpinnerForGoodValue()


        // выбор присетов устройства учета порт 1
        binding.spinnerSelectPort1MeteringDevice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {

                val context: Context = requireContext()

                if (position == 0) {
                    // в случае если расширенныйе настроки то можно менять их
                    binding.DisActivPort1SetiingsPriset.visibility = View.GONE
                } else {
                    if (context is MainActivity) {
                        binding.spinnerSpeed.setSelection(context.portsDeviceSetting[position-1].speed)
                        binding.spinnerSelectParityPort1.setSelection(context.portsDeviceSetting[position-1].parity)
                        binding.spinnerSelectStopBitPort1.setSelection(context.portsDeviceSetting[position-1].stopBit)
                        binding.spinnerBitDataPort1.setSelection(context.portsDeviceSetting[position-1].bitData)

                        binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                    }

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.pm81))
        }

        // настройки кликов
        binding.DisActivPort1SetiingsPriset.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEditSorPresetSet))
        }
        binding.imageSelectPriset.setOnClickListener {
            if (context is MainActivity) {
                context.onClickPrisetPmSettingFor(this)
            }
        }


        // сохранения пресета настроек
        binding.buttonSavePreset.setOnClickListener {
            if (binding.inputNameSavePreset.text.toString().trim().isNotEmpty()) {
                if (context is MainActivity) {
                    if (validAll()) {
                        context.onClickSavePreset(
                            binding.inputNameSavePreset.text.toString(),
                            binding.spinnerServer.selectedItemPosition,
                            binding.inputNetKey.text.toString(),
                            binding.inputPowerCures.text.toString(),
                            binding.spinnerRange.selectedItemPosition
                        )
                        binding.inputNameSavePreset.setText("")
                    }
                }

            } else {
                showAlertDialog(getString(R.string.nonNamePreset))
            }
        }

        //------------------------------------------------------------------------------------------
        // покраска кнопки записи в серый
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.download)

        // Обертываем наш Drawable для совместимости и изменяем цвет
        drawable?.let {
            val wrappedDrawable = DrawableCompat.wrap(it)

            DrawableCompat.setTint(wrappedDrawable, Color.GRAY)

            binding.imageDownLoad.setImageDrawable(wrappedDrawable)
        }
        //------------------------------------------------------------------------------------------

        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice()
        }
        binding.imageDownLoad.setOnClickListener {
            showAlertDialog(getString(R.string.nonWriteSetting))
        }

        if (autoFlag) {
            readSettingStart()
        }

        return binding.root
    }

    // эксперементальный метод для устранения бага с возможностью изменить статические значения
    private fun controlSpinnerForGoodValue() {

        binding.spinnerSpeed.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort1MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerSpeed.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort1MeteringDevice.selectedItemPosition - 1].speed)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }

        binding.spinnerSelectParityPort1.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort1MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerSelectParityPort1.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort1MeteringDevice.selectedItemPosition - 1].parity)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }

        binding.spinnerSelectStopBitPort1.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort1MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerSelectStopBitPort1.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort1MeteringDevice.selectedItemPosition - 1].stopBit)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }



        binding.spinnerBitDataPort1.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort1MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerBitDataPort1.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort1MeteringDevice.selectedItemPosition - 1].bitData)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
    }

    override fun onDestroyView() {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.usb.flagAtCommandYesNo = false
        }

        super.onDestroyView()
    }

    private fun createAdapters() {
        // адаптер для выбора режима работы модема
        val itemsSpinnerDevMode = listOf(
            getString(R.string.devmodeROUTER),
            getString(R.string.devmodeCANPROXY),
            getString(R.string.devmodeRS485),
            getString(R.string.devmodeMONITOR)
        )

        // адаптер для выбора приборра учета
        val itemPortDeviceAccounting = listOf(
            getString(R.string.deviceAccountingAdvancedSettings),
            getString(R.string.deviceAccountingSPT941),
            getString(R.string.deviceAccountingSPT944),
            getString(R.string.deviceAccountingTSP025),
            getString(R.string.deviceAccountingPSCH4TMV23),
            getString(R.string.deviceAccountingTSP027),
            getString(R.string.deviceAccountingPowerCE102M),
            getString(R.string.deviceAccountingMercury206),
            getString(R.string.deviceAccountingKM5PM5),
            getString(R.string.deviceAccountingTEM104),
            getString(R.string.deviceAccountingTEM106),
            getString(R.string.deviceAccountingBKT5),
            getString(R.string.deviceAccountingBKT7),
            getString(R.string.deviceAccountingTePOCC),
            getString(R.string.deviceAccountingSPT943),
            getString(R.string.deviceAccountingSPT961),
            getString(R.string.deviceAccountingKT7Abacan),
            getString(R.string.deviceAccountingMT200DS),
            getString(R.string.deviceAccountingTCP010),
            getString(R.string.deviceAccountingTCP010M),
            getString(R.string.deviceAccountingTCP023),
            getString(R.string.deviceAccountingTCPB024),
            getString(R.string.deviceAccountingTCP026),
            getString(R.string.deviceAccountingTCPB03X),
            getString(R.string.deviceAccountingTCPB042),
            getString(R.string.deviceAccountingYCPB5XX),
            getString(R.string.deviceAccountingPCL212),
            getString(R.string.deviceAccountingSA942M),
            getString(R.string.deviceAccountingSA943),
            getString(R.string.deviceAccountingMKTC),
            getString(R.string.deviceAccountingCKM2),
            getString(R.string.deviceAccountingDymetic5102),
            getString(R.string.deviceAccountingTEPLOVACHESLITELTB7),
            getString(R.string.deviceAccountingELF),
            getString(R.string.deviceAccountingSTU1),
            getString(R.string.deviceAccountingTURBOFLOUGFGF),
            getString(R.string.deviceAccountingEK260),
            getString(R.string.deviceAccountingEK270),
            getString(R.string.deviceAccountingBKG2),
            getString(R.string.deviceAccountingCPG741),
            getString(R.string.deviceAccountingCPG742),
            getString(R.string.deviceAccountingTC2015),
            getString(R.string.deviceAccountingMERCURI230ART5),
            getString(R.string.deviceAccountingPULSAR2M),
            getString(R.string.deviceAccountingPULSAR10M),
            getString(R.string.deviceAccountingKUMIRK21K22),
            getString(R.string.deviceAccountingIM2300),
            getString(R.string.deviceAccountingENERGOMERACE303),
            getString(R.string.deviceAccountingTEM116)
        )
        // адаптер для выбора скорости
        val itemSelectSpeed = listOf(
            getString(R.string.speed_300),
            getString(R.string.speed_600),
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
        // адаптер для выбора диопазона
        val itemSelectRange = listOf(
            getString(R.string.rangeMod1),
            getString(R.string.rangeMod2),
            getString(R.string.rangeMod3)
        )

        val adapter = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemsSpinnerDevMode)
        val adapterPortDeviceAccounting = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemPortDeviceAccounting)
        val adapterSelectSpeed = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed)
        val adapterSelectParity = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectParity)
        val adapterSelectStopBit = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectStopBit)
        val adapterSelectBitData = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectBitData)
        val adapterSelectRange = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectRange)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        adapterPortDeviceAccounting.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectParity.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectStopBit.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectBitData.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectRange.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerServer.adapter = adapter
        binding.spinnerSelectPort1MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort1.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort1.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort1.adapter = adapterSelectBitData
        binding.spinnerRange.adapter = adapterSelectRange
    }

    private fun onClickReadSettingsDevice() {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.showTimerDialog(this, NAME_TYPE_DEVICE)
        }
    }

    private fun onClickWriteSettingsDevice(view: View) {
        writeSettingStart()
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {

        // сохраняем поля диопазона частот и интеренет ключа
        band = settingMap[getString(R.string.commandGetRange)]!!
        netKey = settingMap[getString(R.string.commandGetNetKey)]!!



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
        // ------------------------------------------------------------

        // сброс присетов настроек
        binding.spinnerSelectPort1MeteringDevice.setSelection(0)

        binding.DisActivPort1SetiingsPriset.visibility = View.GONE

        // верийный номер и версия прошибки
        val serNum: String = getString(R.string.serinerNumber) +
                "\n" + settingMap[getString(R.string.commandGetSerialNum)]
        binding.serinerNumber.text = serNum

        val version: String = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionFirmware)]
        binding.textVersionFirmware.text = version

        binding.inputNetKey.setText(settingMap[getString(R.string.commandGetNetKey)])
        binding.inputPowerCures.setText(settingMap[getString(R.string.commandGetPower)])


        // работа со spiner (ражим работы)
        /*Режим работы модема.
            MODE – режим работы
            модуля.
            ROUTER;
            CANPROXY;
            RS485.
            MONITOR
            Режимы записываются
            только в верхнем регистре.
        */

        settingMap[getString(R.string.commandGetMode)]?.let {
            mode = if (it.contains(getString(R.string.devmodeROUTER))) {
                binding.spinnerServer.setSelection(0)
                getString(R.string.devmodeROUTER)
            } else if (it.contains(getString(R.string.devmodeCANPROXY))) {
                binding.spinnerServer.setSelection(1)
                getString(R.string.devmodeCANPROXY)
            }  else if (it.contains(getString(R.string.devmodeRS485))) {
                binding.spinnerServer.setSelection(2)
                getString(R.string.devmodeRS485)
            } else {
                binding.spinnerServer.setSelection(3)
                getString(R.string.devmodeMONITOR)
            }
        }

        // вывод диопазона
        try {
            settingMap[getString(R.string.commandGetRange)]?.let {
                binding.spinnerRange.setSelection(it.trim().toInt()-1)
            }
        } catch (e: NumberFormatException) {
            showAlertDialog(getString(R.string.notReadRange))
        }



        try {
            // отоюражения настроек порта 1-------------------------------------------------------------
            val port1Config = settingMap[getString(R.string.commandGetPort1Config)]?.split(",")

            // скорость -----------------------------------------
            val adapterSpeed = binding.spinnerSpeed.adapter as ArrayAdapter<String>
            val indexSpeed = adapterSpeed.getPosition(port1Config?.get(0))
            if (indexSpeed != -1) {
                binding.spinnerSpeed.setSelection(indexSpeed)
            }

            // количество бит -----------------------------------------
            val adapterBitData = binding.spinnerBitDataPort1.adapter as ArrayAdapter<String>
            val indexBitData = adapterBitData.getPosition(port1Config?.get(1))
            if (indexBitData != -1) {
                binding.spinnerSelectStopBitPort1.setSelection(indexBitData)
            }

            // четность -----------------------------------------
            if (port1Config?.get(2) == "N") {
                binding.spinnerSelectParityPort1.setSelection(0)
            } else if (port1Config?.get(2) == "O") {
                binding.spinnerSelectParityPort1.setSelection(1)
            } else {
                binding.spinnerSelectParityPort1.setSelection(2)
            }

            // стоп биты---------------------------------------------------
            val adapterStopBit = binding.spinnerSelectStopBitPort1.adapter as ArrayAdapter<String>
            val indexStopBit = adapterStopBit.getPosition(port1Config?.get(3))
            if (indexBitData != -1) {
                binding.spinnerSelectStopBitPort1.setSelection(indexStopBit)
            }


            // для сохранения старых настроек и возможности из востановить
            listOldPmSet[0] = listOldPmSet[1]
            listOldPmSet[1] = SettingsPm(
                binding.spinnerRange.selectedItemPosition,
                settingMap[getString(R.string.commandGetNetKey)]!!,
                binding.spinnerServer.selectedItemPosition,
                settingMap[getString(R.string.commandGetPort1Config)]!!,
                settingMap[getString(R.string.commandGetPower)]!!
            )

        } catch (e: NumberFormatException) {
            showAlertDialog(getString(R.string.notReadActPortDevice))
        }
    }

    override fun readSettingStart() {
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandGetVersionFirmware),
            getString(R.string.commandGetMode),
            getString(R.string.commandGetNetKey),
            getString(R.string.commandGetPower),
            getString(R.string.commandGetPort1Config),
            getString(R.string.commandGetRange)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }

    override fun writeSettingStart() {

        if (!validAll()) return

        // проверка на
        /*
           3) at$mode=MONITOR
            - если поля AT$BAND, AT$NETKEY не изменялись (по умолчанию), то тогда не записывать новые значения в РМ81
            - если поля AT$BAND, AT$NETKEY изменялись, то AT$BAND=значение, AT$NETKEY=значение.
        */
        if (binding.spinnerServer.selectedItem.toString() == getString(R.string.devmodeMONITOR) &&
            binding.inputNetKey.text.toString() == netKey &&
            (binding.spinnerRange.selectedItemPosition + 1).toString() == band &&
            mode == getString(R.string.devmodeMONITOR)) {

            showAlertDialog(getString(R.string.noneValidRecordMONITOR))
            return
        }

        var parityPort1 = "N"
        when(binding.spinnerSelectParityPort1.selectedItemPosition) {
            0 -> parityPort1  = "N"
            1 -> parityPort1  = "E"
            2 -> parityPort1  = "O"
        }

        val dataMap: MutableMap<String, String> = mutableMapOf(
            getString(R.string.commandSetMode) to binding.spinnerServer.selectedItem.toString(),
            getString(R.string.commandSetNetKey) to binding.inputNetKey.text.toString(),
            getString(R.string.commandSetPower) to binding.inputPowerCures.text.toString(),
            getString(R.string.commandSetRange) to (binding.spinnerRange.selectedItemPosition + 1).toString()
        )


        dataMap[getString(R.string.commandSetPort1Config)] =
            binding.spinnerSpeed.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort1.selectedItem.toString() + "," +
                    parityPort1  + "," +
                    binding.spinnerSelectStopBitPort1.selectedItem.toString() +
                    ",200,2000"

        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this)

        // кнопка для востановления старых настроек
        if (binding.buttonOldSet.visibility == View.GONE) {
            binding.buttonOldSet.visibility = View.VISIBLE
            binding.buttonOldSet.setOnClickListener {
                binding.spinnerServer.setSelection(listOldPmSet[0].modeOld)
                binding.inputNetKey.setText(listOldPmSet[0].netKeyOld)
                binding.inputPowerCures.setText(listOldPmSet[0].powerOld)
                binding.spinnerRange.setSelection(listOldPmSet[0].bandOld)

                // отображение страрых настроек порта
                try {
                    // отоюражения настроек порта 1-------------------------------------------------------------
                    val port1Config = listOldPmSet[0].port1Old.split(",")

                    // скорость -----------------------------------------
                    val adapterSpeed = binding.spinnerSpeed.adapter as ArrayAdapter<String>
                    val indexSpeed = adapterSpeed.getPosition(port1Config.get(0))
                    if (indexSpeed != -1) {
                        binding.spinnerSpeed.setSelection(indexSpeed)
                    }

                    // количество бит -----------------------------------------
                    val adapterBitData = binding.spinnerBitDataPort1.adapter as ArrayAdapter<String>
                    val indexBitData = adapterBitData.getPosition(port1Config.get(1))
                    if (indexBitData != -1) {
                        binding.spinnerSelectStopBitPort1.setSelection(indexBitData)
                    }

                    // четность -----------------------------------------
                    if (port1Config.get(2) == "N") {
                        binding.spinnerSelectParityPort1.setSelection(0)
                    } else if (port1Config.get(2) == "O") {
                        binding.spinnerSelectParityPort1.setSelection(1)
                    } else {
                        binding.spinnerSelectParityPort1.setSelection(2)
                    }

                    // стоп биты---------------------------------------------------
                    val adapterStopBit = binding.spinnerSelectStopBitPort1.adapter as ArrayAdapter<String>
                    val indexStopBit = adapterStopBit.getPosition(port1Config.get(3))
                    if (indexBitData != -1) {
                        binding.spinnerSelectStopBitPort1.setSelection(indexStopBit)
                    }


                } catch (e: NumberFormatException) {
                    showAlertDialog(getString(R.string.notReadActPortDevice))
                }
            }
        }
    }

    override fun lockFromDisconnected(connect: Boolean) {
        // текстрки для кнопок
        val drawablImageDownLoad = ContextCompat.getDrawable(requireContext(), R.drawable.download)
        val drawablImageDischarge = ContextCompat.getDrawable(requireContext(), R.drawable.discharge)

        if (!connect) {
            //------------------------------------------------------------------------------------------
            // покраска кнопки записи в серый
            // Обертываем наш Drawable для совместимости и изменяем цвет

            drawablImageDownLoad?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GRAY)
                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }
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
            binding.imageDownLoad.setOnClickListener {
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

            binding.imageDownLoad.setOnClickListener {
                showAlertDialog(getString(R.string.notReadDevice))
            }
        }
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

    private fun validAll(): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()

        // проверка на русские символы в серверах и apn
        if (!validDataSettingsDevice.serverValid(binding.inputNetKey.text.toString())) {
            showAlertDialog(getString(R.string.errorRussionChar))
            return false
        }

        // проверки на валидность POWER
        if (!validDataSettingsDevice.powerValid(binding.inputPowerCures.text.toString()
                .replace("\\s+".toRegex(), ""))) {

            showAlertDialog(getString(R.string.errorPOWER))
            return false

        } else if (!validDataSettingsDevice.validPM81KeyNet(binding.inputNetKey.text.toString())) {
            showAlertDialog(getString(R.string.errorNETKEY))
            return false
        }

        return true
    }

    override fun printPriset(priset: Pm) {
        // закрытие меню
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.workFonDarkMenu()
        }
        // подставление данных в поля
        binding.spinnerServer.setSelection(priset.mode)
        binding.inputNetKey.setText(priset.keyNet)
        binding.inputPowerCures.setText(priset.power)
        binding.spinnerRange.setSelection(priset.diopozone)

    }

}