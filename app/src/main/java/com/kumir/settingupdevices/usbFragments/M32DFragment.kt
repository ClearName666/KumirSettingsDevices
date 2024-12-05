package com.kumir.settingupdevices.usbFragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentM32DBinding
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.model.recyclerModel.Priset
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText


class M32DFragment(val autoFlag: Boolean) : Fragment(), UsbFragment, PrisetFragment<Priset> {

    private lateinit var binding: FragmentM32DBinding
    override val usbCommandsProtocol = UsbCommandsProtocol()

    private var NAME_TYPE_DEVICE = "KUMIR-M32D READY"




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentM32DBinding.inflate(inflater)

        controlSpinnerForGoodValue()

        // установка даных в tab layout
        binding.tabSims.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.layoutSim1.visibility = View.VISIBLE
                        binding.layoutSim2.visibility = View.GONE
                    }
                    1 -> {
                        binding.layoutSim1.visibility = View.GONE
                        binding.layoutSim2.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No-op
            }
        })


        // выбор присетов устройства учета порт 1
        binding.spinnerSelectPort1MeteringDevice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

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

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        // выбор присетов устройства учета порт 2
        binding.spinnerSelectPort2MeteringDevice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                val context: Context = requireContext()

                if (position == 0) {
                    // в случае если расширенныйе настроки то можно менять их
                    binding.DisActivPort2SetiingsPriset.visibility = View.GONE
                } else {
                    if (context is MainActivity) {
                        binding.spinnerSpeedPort2.setSelection(context.portsDeviceSetting[position-1].speed)
                        binding.spinnerSelectParityPort2.setSelection(context.portsDeviceSetting[position-1].parity)
                        binding.spinnerSelectStopBitPort2.setSelection(context.portsDeviceSetting[position-1].stopBit)
                        binding.spinnerBitDataPort2.setSelection(context.portsDeviceSetting[position-1].bitData)

                        binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
                    }

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.m32d))
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

        binding.DisActivPort1SetiingsPriset.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEditSorPresetSet))
        }
        binding.DisActivPort2SetiingsPriset.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEditSorPresetSet))
        }

        binding.imageSelectPriset.setOnClickListener {
            if (context is MainActivity) {
                context.onClickPrisetSettingFor(this)
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
                            binding.inputAPN.text.toString(),
                            "kumir.dv",
                            "1",
                            binding.inputTextLoginGPRS.text.toString(),
                            binding.inputPasswordGPRS.text.toString()
                        )
                        binding.inputNameSavePreset.setText("")
                    }
                }

            } else {
                showAlertDialog(getString(R.string.nonNamePreset))
            }
        }


        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice()
        }
        binding.imageDownLoad.setOnClickListener {
            showAlertDialog(getString(R.string.nonWriteSetting))
        }

        createAdapters()

        if (autoFlag) {
            readSettingStart()
        }

        setupInputValidation()

        return binding.root
    }

    private fun setupInputValidation() {
        // Карта для связи input с layout
        val inputMap = mapOf(
            binding.InputSim1TCP1 to binding.layoutInputSim1TCP1,
            binding.InputSim1TCP2 to binding.layoutInputSim1TCP2,
            binding.InputSim1TCP3 to binding.layoutInputSim1TCP3,
            binding.InputSim1TCP4 to binding.layoutInputSim1TCP4,

            binding.InputSim2TCP1 to binding.layoutInputSim2TCP1,
            binding.InputSim2TCP2 to binding.layoutInputSim2TCP2,
            binding.InputSim2TCP3 to binding.layoutInputSim2TCP3,
            binding.InputSim2TCP4 to binding.layoutInputSim2TCP4,

            binding.inputSim1Knet to binding.layoutInputTextKnet,
            binding.inputSim1Sntp to binding.layoutSim1Sntp,

            binding.inputSim2Knet to binding.layoutInputTextKnet2,
            binding.inputSim2Sntp to binding.layoutSim2Sntp,

            binding.inputAPN to binding.inputAPNLayout,
            binding.inputTextLoginGPRS to binding.inputTextLoginGPRSLayout,
            binding.inputPasswordGPRS to binding.inputPasswordGPRSLayout,

            binding.inputAPN2 to binding.inputAPNLayout2,
            binding.inputTextLoginGPRS2 to binding.inputTextLoginGPRSLayout2,
            binding.inputPasswordGPRS2 to binding.inputPasswordGPRSLayout2,

            binding.inputTimeOutKeeplive to binding.inputTimeOutKeepliveLayout,
            binding.inputTimeoutConnection to binding.inputTimeoutConnectionLayout,
        )

        // Настраиваем слушатели для каждого input
        inputMap.forEach { (editText, layout) ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val inputText = s?.toString() ?: ""
                    if (isValidInput(editText, inputText)) {
                        layout.error = null // Убираем ошибку
                    } else {
                        layout.error = "Ошибка: проверьте данные" // Устанавливаем ошибку
                    }
                }
            })
        }
    }

    // Функция для проверки валидности
    private fun isValidInput(editText: TextInputEditText, inputText: String): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()
        return when (editText.id) {
            R.id.inputAPN -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputTextLoginGPRS -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputPasswordGPRS -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)

            R.id.inputAPN2 -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputTextLoginGPRS2 -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputPasswordGPRS2 -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)

            R.id.inputTimeOutKeeplive -> validDataSettingsDevice.keepaliveValid(inputText.replace("\\s+".toRegex(), ""))
            R.id.inputTimeoutConnection -> validDataSettingsDevice.ctimeoutValid(inputText.replace("\\s+".toRegex(), ""))

            R.id.InputSim1TCP1 -> validDataSettingsDevice.validSim1tcp1(inputText)
            R.id.InputSim1TCP2 -> validDataSettingsDevice.validSim1tcp2(inputText)
            R.id.InputSim1TCP3 -> validDataSettingsDevice.validSim1tcp3(inputText)
            R.id.InputSim1TCP4 -> validDataSettingsDevice.validSim1tcp4(inputText)

            R.id.InputSim2TCP1 -> validDataSettingsDevice.validSim2tcp1(inputText)
            R.id.InputSim2TCP2 -> validDataSettingsDevice.validSim2tcp2(inputText)
            R.id.InputSim2TCP3 -> validDataSettingsDevice.validSim2tcp3(inputText)
            R.id.InputSim2TCP4 -> validDataSettingsDevice.validSim2tcp4(inputText)

            R.id.inputSim1Knet -> validDataSettingsDevice.validSim1knet(inputText)
            R.id.inputSim2Knet -> validDataSettingsDevice.validSim2knet(inputText)

            R.id.inputSim1Sntp -> validDataSettingsDevice.validSim1sntp(inputText)
            R.id.inputSim2Sntp -> validDataSettingsDevice.validSim2sntp(inputText)

            else -> true
        }
    }

    override fun onDestroyView() {
        val context: Context = requireContext()

        // выключение ат команд
        if (context is MainActivity) {
            context.usb.flagAtCommandYesNo = false
        }

        super.onDestroyView()
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



        binding.spinnerSpeedPort2.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort2MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerSpeedPort2.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort2MeteringDevice.selectedItemPosition - 1].speed)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }

        binding.spinnerSelectParityPort2.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort2MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerSelectParityPort2.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort2MeteringDevice.selectedItemPosition - 1].parity)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }

        binding.spinnerSelectStopBitPort2.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort2MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerSelectStopBitPort2.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort2MeteringDevice.selectedItemPosition - 1].stopBit)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }



        binding.spinnerBitDataPort2.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val context: Context = requireContext() as MainActivity

                    if (binding.spinnerSelectPort2MeteringDevice.selectedItemPosition != 0 && context is MainActivity) {
                        binding.spinnerBitDataPort2.setSelection(context.portsDeviceSetting[binding.spinnerSelectPort2MeteringDevice.selectedItemPosition - 1].bitData)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
    }

    private fun createAdapters() {
        // адаптер для выбора режима работы модема
        val itemsSpinnerDevMode = listOf(
            getString(R.string.devModeKNET),
            getString(R.string.devModeTCPCLIENT),
            getString(R.string.devModeTCPSERVER),
            getString(R.string.devModeMODEM)
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

        binding.spinnerServer.adapter = adapter
        binding.spinnerSelectPort1MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSelectPort2MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerSpeedPort2.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort1.adapter = adapterSelectParity
        binding.spinnerSelectParityPort2.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort1.adapter = adapterSelectStopBit
        binding.spinnerSelectStopBitPort2.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort1.adapter = adapterSelectBitData
        binding.spinnerBitDataPort2.adapter = adapterSelectBitData
    }

    override fun printSerifalNumber(serialNumber: String) {}

    override fun printVersionProgram(versionProgram: String) {}

    override fun printSettingDevice(settingMap: Map<String, String>) {

        val context: Context = requireContext()

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

        // вывод присетов настроек
        var preset1: Int = 0
        var preset2: Int = 0
        try {
            // переводим данные пресетов настроек в инт
            val profile1: Int = settingMap[getString(R.string.commandGetProfile1M32D)]?.
            replace("\n", "")?.
            replace(" ", "")?.toInt()!!
            val profile2: Int = settingMap[getString(R.string.commandGetProfile2M32D)]?.
            replace("\n", "")?.
            replace(" ", "")?.toInt()!!

            // находим среди всех присетов индекс номера присета с настройками
            if (context is MainActivity) {
                for (itemPreset in 0..<context.portsDeviceSetting.size) {
                    if (profile1 == context.portsDeviceSetting[itemPreset].priset) {
                        preset1 = itemPreset
                    }
                    if (profile2 == context.portsDeviceSetting[itemPreset].priset) {
                        preset2 = itemPreset
                    }
                }
            }
        } catch (e: Exception) {
            // не валидные данные
            showAlertDialog(getString(R.string.nonValidData) +
                    "\n${getString(R.string.commandGetProfile1M32D)} = " +
                    "${settingMap[getString(R.string.commandGetProfile1M32D)]}" +
                    "\n${getString(R.string.commandGetProfile2M32D)} = " +
                    "${settingMap[getString(R.string.commandGetProfile2M32D)]}")
        }

        binding.spinnerSelectPort1MeteringDevice.setSelection(preset1)
        binding.spinnerSelectPort2MeteringDevice.setSelection(preset2)

        // если профайл не 0 то
        if (preset1 != 0) {
            binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
        }
        if (preset2 != 0) {
            binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
        }


        // верийный номер и версия прошибки
        val serialNumberGlobal = getString(R.string.serinerNumber) +
                "\n" + settingMap[getString(R.string.commandGetMdmidM32D)]
        binding.serinerNumber.text = serialNumberGlobal

        val programVersionGlobal = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionM32D)]
        binding.textVersionFirmware.text = programVersionGlobal

        try {
            val varsionSim: Int = settingMap[getString(R.string.commandGetVersionM32D)]?.
            substringAfter("HW: ")?.substringBefore(" ")?.toInt()!!

            // проверка какое устройство сечас сканируется
            if (varsionSim < 4 || varsionSim == 16) {
                binding.tabSims.visibility = View.GONE
            } else {
                binding.tabSims.visibility = View.VISIBLE

                // симка 2----------------------------------------------------------------------------------
                binding.inputAPN2.setText(settingMap[getString(R.string.commandGetSim2ApnM32D)])
                binding.inputTextLoginGPRS2.setText(settingMap[getString(R.string.commandGetSim2LoginM32D)])
                if (settingMap[getString(R.string.commandGetSim2PasswM32D)]?.contains("****") == false) {
                    binding.inputPasswordGPRS.setText(settingMap[getString(R.string.commandGetSim2PasswM32D)])
                }

                // установка переключетелей
                if (settingMap[getString(R.string.commandGetSim2PinM32D)]?.
                    contains(getString(R.string.disabled)) == true){
                    binding.switchPinCodeSmsCard2.isChecked = false

                    binding.inputPinCodeSmsCard2.setText("")
                } else {
                    binding.switchPinCodeSmsCard2.isChecked = true
                }

                binding.inputSim2Knet.setText(settingMap[getString(R.string.commandGetSim2KnetM32D)])
                binding.inputSim2Sntp.setText(settingMap[getString(R.string.commandGetSim2SntpM32D)])

                binding.InputSim2TCP1.setText(settingMap[getString(R.string.commandGetSim2Tcp1M32D)])
                binding.InputSim2TCP2.setText(settingMap[getString(R.string.commandGetSim2Tcp2M32D)])
                binding.InputSim2TCP3.setText(settingMap[getString(R.string.commandGetSim2Tcp3M32D)])
                binding.InputSim2TCP4.setText(settingMap[getString(R.string.commandGetSim2Tcp4M32D)])
            }
        } catch (e: Exception) {
            showAlertDialog(getString(R.string.errorCodeNone))
        }



        if (settingMap[getString(R.string.commandGetSmspinM32D)]?.
            contains(getString(R.string.disabled)) == true) {
            binding.switchPinCodeSmsCommand.isChecked = false

            binding.inputPinCodeCommand.setText("")
        } else {
            binding.switchPinCodeSmsCommand.isChecked = true

        }

        // вывод режима работы
        settingMap[getString(R.string.commandGetDevmodeM32D)]?.let {
            if (it.contains(getString(R.string.devModeKNET))) {
                binding.spinnerServer.setSelection(0)
            } else if (it.contains(getString(R.string.devModeTCPCLIENT))) {
                binding.spinnerServer.setSelection(1)
            } else if (it.contains(getString(R.string.devModeTCPSERVER))) {
                binding.spinnerServer.setSelection(2)
            } else if (it.contains(getString(R.string.devModeMODEM))) {
                binding.spinnerServer.setSelection(3)
            } else {
                showAlertDialog(getString(R.string.notReadDevModeDevice))
            }
        }

        try {
            // отоюражения настроек порта 1-------------------------------------------------------------
            val port1Config = settingMap[getString(R.string.commandGetPort1M32D)]?.split(",")

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





            // отоюражения настроек порта 2-------------------------------------------------------------
            val port2Config = settingMap[getString(R.string.commandGetPort2M32D)]?.split(",")

            // скорость -----------------------------------------
            val adapterSpeed2 = binding.spinnerSpeedPort2.adapter as ArrayAdapter<String>
            val indexSpeed2 = adapterSpeed2.getPosition(port2Config?.get(0))
            if (indexSpeed2 != -1) {
                binding.spinnerSpeedPort2.setSelection(indexSpeed2)
            }

            // количество бит -----------------------------------------
            val adapterBitData2 = binding.spinnerBitDataPort2.adapter as ArrayAdapter<String>
            val indexBitData2 = adapterBitData2.getPosition(port2Config?.get(1))
            if (indexBitData2 != -1) {
                binding.spinnerSelectStopBitPort2.setSelection(indexBitData2)
            }

            // четность -----------------------------------------
            if (port2Config?.get(2) == "N") {
                binding.spinnerSelectParityPort2.setSelection(0)
            } else if (port2Config?.get(2) == "O") {
                binding.spinnerSelectParityPort2.setSelection(1)
            } else {
                binding.spinnerSelectParityPort2.setSelection(2)
            }

            // стоп биты---------------------------------------------------
            val adapterStopBit2 = binding.spinnerSelectStopBitPort2.adapter as ArrayAdapter<String>
            val indexStopBit2 = adapterStopBit2.getPosition(port2Config?.get(3))
            if (indexBitData2 != -1) {
                binding.spinnerSelectStopBitPort2.setSelection(indexStopBit2)
            }


        } catch (e: NumberFormatException) {
            showAlertDialog(getString(R.string.notReadPortDevice))
        }

        // симка 1----------------------------------------------------------------------------------
        binding.inputAPN.setText(settingMap[getString(R.string.commandGetSim1ApnM32D)])
        binding.inputTextLoginGPRS.setText(settingMap[getString(R.string.commandGetSim1LoginM32D)])

        if (settingMap[getString(R.string.commandGetSim1PasswM32D)]?.contains("****") == false) {
            binding.inputPasswordGPRS.setText(settingMap[getString(R.string.commandGetSim1PasswM32D)])
        }


        // установка переключетелей
        if (settingMap[getString(R.string.commandGetSim1PinM32D)]?.
            contains(getString(R.string.disabled)) == true){
            binding.switchPinCodeSmsCard.isChecked = false

            binding.inputPinCodeSmsCard.setText("")
        } else {
            binding.switchPinCodeSmsCard.isChecked = true
        }

        binding.inputSim1Knet.setText(settingMap[getString(R.string.commandGetSim1KnetM32D)])
        binding.inputSim1Sntp.setText(settingMap[getString(R.string.commandGetSim1SntpM32D)])

        binding.InputSim1TCP1.setText(settingMap[getString(R.string.commandGetSim1Tcp1M32D)])
        binding.InputSim1TCP2.setText(settingMap[getString(R.string.commandGetSim1Tcp2M32D)])
        binding.InputSim1TCP3.setText(settingMap[getString(R.string.commandGetSim1Tcp3M32D)])
        binding.InputSim1TCP4.setText(settingMap[getString(R.string.commandGetSim1Tcp4M32D)])




        // общие настройки
        binding.inputTimeOutKeeplive.setText(settingMap[getString(R.string.commandGetKeepaliveM32D)])
        binding.inputTimeoutConnection.setText(settingMap[getString(R.string.commandGetCtimeoutM32D)])
    }

    override fun readSettingStart() {
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetMdmidM32D),
            getString(R.string.commandGetVersionM32D),
            getString(R.string.commandGetDevmodeM32D),
            getString(R.string.commandGetKeepaliveM32D),
            getString(R.string.commandGetCtimeoutM32D),
            getString(R.string.commandGetSmspinM32D),
            getString(R.string.commandGetTzdataM32D),
            getString(R.string.commandGetPort1M32D),
            getString(R.string.commandGetPort2M32D),
            getString(R.string.commandGetProfile1M32D),
            getString(R.string.commandGetProfile2M32D),
            getString(R.string.commandGetSim1PinM32D),
            getString(R.string.commandGetSim1ApnM32D),
            getString(R.string.commandGetSim1LoginM32D),
            getString(R.string.commandGetSim1PasswM32D),
            getString(R.string.commandGetSim1KnetM32D),
            getString(R.string.commandGetSim1SntpM32D),
            getString(R.string.commandGetSim1Tcp1M32D),
            getString(R.string.commandGetSim1Tcp2M32D),
            getString(R.string.commandGetSim1Tcp3M32D),
            getString(R.string.commandGetSim1Tcp4M32D),
            getString(R.string.commandGetTcpport1M32D),
            getString(R.string.commandGetTcpport2M32D),
            getString(R.string.commandGetSim2PinM32D),
            getString(R.string.commandGetSim2ApnM32D),
            getString(R.string.commandGetSim2LoginM32D),
            getString(R.string.commandGetSim2PasswM32D),
            getString(R.string.commandGetSim2KnetM32D),
            getString(R.string.commandGetSim2SntpM32D),
            getString(R.string.commandGetSim2Tcp1M32D),
            getString(R.string.commandGetSim2Tcp2M32D),
            getString(R.string.commandGetSim2Tcp3M32D),
            getString(R.string.commandGetSim2Tcp4M32D)
        )


        usbCommandsProtocol.readSettingDevice(command, requireContext(), this, flagM32D = true)
    }

    override fun writeSettingStart() {

        if (!validAll()) return

        val context: MainActivity = requireContext() as MainActivity

        var parityPort1 = "N"
        when(binding.spinnerSelectParityPort1.selectedItemPosition) {
            0 -> parityPort1  = "N"
            1 -> parityPort1  = "E"
            2 -> parityPort1  = "O"
        }

        var parityPort2 = "N"
        when(binding.spinnerSelectParityPort2.selectedItemPosition) {
            0 -> parityPort2  = "N"
            1 -> parityPort2  = "E"
            2 -> parityPort2  = "O"
        }

        val dataMap: MutableMap<String, String> = mutableMapOf(
            getString(R.string.commandSetDevmodeM32D) to binding.spinnerServer.selectedItem.toString(),
            getString(R.string.commandSetKeepaliveM32D) to binding.inputTimeOutKeeplive.text.toString(),
            getString(R.string.commandSetCtimeoutM32D) to binding.inputTimeoutConnection.text.toString(),
            /*getString(R.string.commandSetTzdataM32D) to "",*/
            getString(R.string.commandSetPort1M32D) to binding.spinnerSpeed.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort1.selectedItem.toString() + "," +
                    parityPort1  + "," +
                    binding.spinnerSelectStopBitPort1.selectedItem.toString() +
                    ",200,2000",
            getString(R.string.commandSetPort2M32D) to binding.spinnerSpeedPort2.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort2.selectedItem.toString() + "," +
                    parityPort2  + "," +
                    binding.spinnerSelectStopBitPort2.selectedItem.toString() +
                    ",200,2000",
            getString(R.string.commandSetProfile1M32D) to context.portsDeviceSetting[binding.spinnerSelectPort1MeteringDevice.selectedItemPosition].
                priset.toString(),
            getString(R.string.commandSetProfile2M32D) to context.portsDeviceSetting[binding.spinnerSelectPort2MeteringDevice.selectedItemPosition].
                priset.toString(),
            /*getString(R.string.commandSetSim1PinM32D) to "",*/
            getString(R.string.commandSetSim1ApnM32D) to binding.inputAPN.text.toString(),
            getString(R.string.commandSetSim1LoginM32D) to binding.inputTextLoginGPRS.text.toString(),
            getString(R.string.commandSetSim1PasswM32D) to binding.inputPasswordGPRS.text.toString(),
            getString(R.string.commandSetSim1KnetM32D) to binding.inputSim1Knet.text.toString(),
            getString(R.string.commandSetSim1SntpM32D) to binding.inputSim1Sntp.text.toString(),
            getString(R.string.commandSetSim1Tcp1M32D) to binding.InputSim1TCP1.text.toString(),
            getString(R.string.commandSetSim1Tcp2M32D) to binding.InputSim1TCP2.text.toString(),
            getString(R.string.commandSetSim1Tcp3M32D) to binding.InputSim1TCP3.text.toString(),
            getString(R.string.commandSetSim1Tcp4M32D) to binding.InputSim1TCP4.text.toString()
        )

        if (binding.tabSims.visibility == View.VISIBLE) {
            dataMap[getString(R.string.commandSetSim2ApnM32D)] = binding.inputAPN2.text.toString()
            dataMap[getString(R.string.commandSetSim2LoginM32D)] = binding.inputTextLoginGPRS2.text.toString()
            dataMap[getString(R.string.commandSetSim2PasswM32D)] = binding.inputPasswordGPRS2.text.toString()
            dataMap[getString(R.string.commandSetSim2KnetM32D)] = binding.inputSim2Knet.text.toString()
            dataMap[getString(R.string.commandSetSim2SntpM32D)] = binding.inputSim2Sntp.text.toString()
            dataMap[getString(R.string.commandSetSim2Tcp1M32D)] = binding.InputSim2TCP1.text.toString()
            dataMap[getString(R.string.commandSetSim2Tcp2M32D)] = binding.InputSim2TCP2.text.toString()
            dataMap[getString(R.string.commandSetSim2Tcp3M32D)] = binding.InputSim2TCP3.text.toString()
            dataMap[getString(R.string.commandSetSim2Tcp4M32D)] = binding.InputSim2TCP4.text.toString()


            // сим 2
            if (binding.switchPinCodeSmsCard2.isChecked) {
                if (binding.inputPinCodeSmsCard2.text?.trim()?.isNotEmpty() == true) {
                    dataMap[getString(R.string.commandSetSim2PinM32D)] =
                        binding.inputPinCodeSmsCard2.text.toString()
                }
            } else {
                dataMap[getString(R.string.commandSetSim2PinM32D)] = getString(R.string.disabled)
            }
        }

        // установка пин кода на карты
        // сим 1
        if (binding.switchPinCodeSmsCard.isChecked) {
            if (binding.inputPinCodeSmsCard.text?.trim()?.isNotEmpty() == true) {
                dataMap[getString(R.string.commandSetSim1PinM32D)] =
                    binding.inputPinCodeSmsCard.text.toString()
            }
        } else {
            dataMap[getString(R.string.commandSetSim1PinM32D)] = getString(R.string.disabled)
        }



        if (binding.switchPinCodeSmsCommand.isChecked) {
            if (binding.inputPinCodeCommand.text?.isNotEmpty() == true) {
                dataMap[getString(R.string.commandSetSmsPin)] =
                    binding.inputPinCodeCommand.text.toString()
            }
        } else {
            dataMap[getString(R.string.commandSetSmsPin)] = getString(R.string.disabled)
        }


        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this)
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



    override fun printPriset(priset: Priset) {
        // закрытие меню
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.workFonDarkMenu()
        }

        if (binding.layoutSim1.visibility == View.VISIBLE) {
            // подставление данных в поля
            binding.inputAPN.setText(priset.apn)
            binding.inputSim1Knet.setText(priset.server1)
            binding.inputTextLoginGPRS.setText(priset.login)
            binding.inputPasswordGPRS.setText(priset.password)
        } else {
            // подставление данных в поля
            binding.inputAPN2.setText(priset.apn)
            binding.inputSim2Knet.setText(priset.server1)
            binding.inputTextLoginGPRS2.setText(priset.login)
            binding.inputPasswordGPRS2.setText(priset.password)
        }




        try {
            if (priset.mode < 4)
                binding.spinnerServer.setSelection(priset.mode)
        } catch (e: Exception) {
            showAlertDialog(getString(R.string.errorDataBase))
        }

    }

    private fun validAll(): Boolean {
        val validServis = ValidDataSettingsDevice()

        var parityPort1 = "N"
        when(binding.spinnerSelectParityPort1.selectedItemPosition) {
            0 -> parityPort1  = "N"
            1 -> parityPort1  = "E"
            2 -> parityPort1  = "O"
        }

        var parityPort2 = "N"
        when(binding.spinnerSelectParityPort2.selectedItemPosition) {
            0 -> parityPort2  = "N"
            1 -> parityPort2  = "E"
            2 -> parityPort2  = "O"
        }
        if (!validServis.validDevmode(binding.spinnerServer.selectedItem.toString())) {
            showAlertDialog(getString(R.string.errorValidM32DMode))
            return false
        }
        if (!validServis.validKeepalive(binding.inputTimeOutKeeplive.text.toString())) {
            showAlertDialog(getString(R.string.errorValidM32DKeeplive))

            return false
        }
        if (!validServis.validCtimeout(binding.inputTimeoutConnection.text.toString())) {
            showAlertDialog(getString(R.string.errorValidM32DCtimeOut))
            return false
        }
        if (!validServis.validSmspin(binding.inputPinCodeCommand.text.toString()) &&
            binding.inputPinCodeCommand.text?.isNotEmpty() == true) {
            showAlertDialog(getString(R.string.errorValidM32DSmsPin))
            return false
        }
        if (!validServis.validPort1(binding.spinnerSpeed.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort1.selectedItem.toString() + "," +
                    parityPort1  + "," +
                    binding.spinnerSelectStopBitPort1.selectedItem.toString() +
                    ",200,2000")) {
            showAlertDialog(getString(R.string.errorValidPortSim))
            return false
        }
        if (!validServis.validPort2(binding.spinnerSpeedPort2.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort2.selectedItem.toString() + "," +
                    parityPort2  + "," +
                    binding.spinnerSelectStopBitPort2.selectedItem.toString() +
                    ",200,2000")) {
            showAlertDialog(getString(R.string.errorValidPortSim))

            return false
        }
        if (!validServis.validSim1pin(binding.inputPinCodeSmsCard.text.toString()) &&
            binding.inputPinCodeSmsCard.text?.isNotEmpty() == true) {

            showAlertDialog(getString(R.string.errorValidM32DPinCode))
            return false
        }


        if (!validServis.validSim1apn(binding.inputAPN.text.toString())) {
            showAlertDialog(getString(R.string.errorValidM32DAPN))
            return false
        }


        if (!validServis.validSim1knet(binding.inputSim1Knet.text.toString())) {
            showAlertDialog(getString(R.string.errorValidM32DKnet))
            return false
        }


        if (!validServis.validSim1sntp(binding.inputSim1Sntp.text.toString())) {
            showAlertDialog(getString(R.string.errorValidM32DSntp))

            return false
        }


        if (!validServis.validSim1tcp1(binding.InputSim1TCP1.text.toString()) &&
            binding.InputSim1TCP1.text.toString().isNotEmpty()) {
            showAlertDialog(getString(R.string.errorValidM32DTCP))
            return false
        }
        if (!validServis.validSim1tcp2(binding.InputSim1TCP2.text.toString()) &&
            binding.InputSim1TCP2.text.toString().isNotEmpty()) {
            showAlertDialog(getString(R.string.errorValidM32DTCP))

            return false
        }
        if (!validServis.validSim1tcp3(binding.InputSim1TCP3.text.toString()) &&
            binding.InputSim1TCP3.text.toString().isNotEmpty()) {
            showAlertDialog(getString(R.string.errorValidM32DTCP))

            return false
        }
        if (!validServis.validSim1tcp4(binding.InputSim1TCP4.text.toString()) &&
            binding.InputSim1TCP4.text.toString().isNotEmpty()) {
            showAlertDialog(getString(R.string.errorValidM32DTCP))

            return false
        }


        if (binding.tabSims.visibility == View.VISIBLE) {
            if (!validServis.validSim2tcp1(binding.InputSim2TCP1.text.toString()) &&
                binding.InputSim2TCP1.text.toString().isNotEmpty()) {
                showAlertDialog(getString(R.string.errorValidM32DTCP))

                return false
            }
            if (!validServis.validSim2tcp2(binding.InputSim2TCP2.text.toString()) &&
                binding.InputSim2TCP2.text.toString().isNotEmpty() ) {
                showAlertDialog(getString(R.string.errorValidM32DTCP))

                return false
            }
            if (!validServis.validSim2tcp3(binding.InputSim2TCP3.text.toString()) &&
                binding.InputSim2TCP3.text.toString().isNotEmpty()) {
                showAlertDialog(getString(R.string.errorValidM32DTCP))

                return false
            }
            if (!validServis.validSim2tcp4(binding.InputSim2TCP4.text.toString()) &&
                binding.InputSim2TCP4.text.toString().isNotEmpty()) {
                showAlertDialog(getString(R.string.errorValidM32DTCP))

                return false
            }

            if (!validServis.validSim2sntp(binding.inputSim2Sntp.text.toString())) {
                showAlertDialog(getString(R.string.errorValidM32DSntp))

                return false
            }

            if (!validServis.validSim2knet(binding.inputSim2Knet.text.toString())) {
                showAlertDialog(getString(R.string.errorValidM32DKnet))

                return false
            }

            if (!validServis.validSim2pin(binding.inputPinCodeSmsCard2.text.toString()) &&
                binding.inputPinCodeSmsCard2.text?.isNotEmpty() == true) {
                showAlertDialog(getString(R.string.errorValidM32DPinCode))

                return false
            }
            if (!validServis.validSim2apn(binding.inputAPN2.text.toString())) {
                showAlertDialog(getString(R.string.errorValidM32DAPN))

                return false
            }
        }

        return true
    }
}