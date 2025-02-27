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
import com.google.android.material.textfield.TextInputEditText
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.databinding.FragmentM32Binding
import com.kumir.settingupdevices.model.recyclerModel.Priset
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment


class M32Fragment(val autoFlag: Boolean) : Fragment(), UsbFragment, PrisetFragment<Priset> {

    override val usbCommandsProtocol = UsbCommandsProtocol()

    private lateinit var binding: FragmentM32Binding

    private var serialNumberGlobal: String? = null
    private var programVersionGlobal: String? = null


    private var NAME_TYPE_DEVICE = "KUMIR-M32 READY"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentM32Binding.inflate(inflater)

        controlSpinnerForGoodValue()

        createAdapters()

        // мониторинг изменений режима работы
        binding.spinnerServer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                setPresetSpinnerServer(position)
                updateViewPort(binding.spinnerSelectActivPort.selectedItemPosition)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        // изменения блакировки изменения настроек портов
        binding.spinnerSelectActivPort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                updateViewPort(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

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
            context.printDeviceTypeName(getString(R.string.m32))
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

        // назначение кликов
        binding.port1DisActivFon.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEdit))
        }
        binding.port2DisActivFon.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEdit))
        }

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
        /*binding.imageDiag.setOnClickListener {
            // запускаем окно с дигностикой

            if (context is MainActivity && programVersionGlobal != null) {
                context.onClickDiag(serialNumberGlobal!!, programVersionGlobal!!)
            } else {
                showAlertDialog(getString(R.string.nonWriteSetting))
            }
        }*/

        // сохранения пресета настроек
        binding.buttonSavePreset.setOnClickListener {
            if (binding.inputNameSavePreset.text.toString().trim().isNotEmpty()) {
                if (context is MainActivity) {
                    if (validAll()) {
                        context.onClickSavePreset(
                            binding.inputNameSavePreset.text.toString(),
                            binding.spinnerServer.selectedItemPosition,
                            binding.inputAPN.text.toString(),
                            binding.inputIPDNS.text.toString(),
                            binding.inputTCP.text.toString(),
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

        if (autoFlag) {
            readSettingStart()
        }

        setupInputValidation()

        return binding.root
    }


    private fun setupInputValidation() {
        // Карта для связи input с layout
        val inputMap = mapOf(
            binding.inputIPDNS to binding.inputIPDNSLayout,
            binding.inputAPN to binding.inputAPNLayout,
            binding.inputTextLoginGPRS to binding.inputTextLoginGPRSLayout,
            binding.inputPasswordGPRS to binding.inputPasswordGPRSLayout,
            binding.inputTimeOutKeeplive to binding.inputTimeOutKeepliveLayout,
            binding.inputTimeoutConnection to binding.inputTimeoutConnectionLayout,
            binding.inputTCP to binding.inputTCPLayout,
            binding.inputPinCodeCommand to binding.inputPinCodeCommandLayout,
            binding.inputPinCodeSmsCard to binding.inputPinCodeSmsCardLayout
        )

        // Карта для связи input с текстом ошибки
        val inputMapText = mapOf(
            binding.inputIPDNS to getString(R.string.errorRussionChar),
            binding.inputAPN to getString(R.string.errorValidAPNFormat),
            binding.inputTextLoginGPRS to getString(R.string.errorRussionChar),
            binding.inputPasswordGPRS to getString(R.string.errorRussionChar),
            binding.inputTimeOutKeeplive to getString(R.string.errorKEEPALIVE),
            binding.inputTimeoutConnection to getString(R.string.errorCTIMEOUT),
            binding.inputTCP to getString(R.string.errorTCPPORT),
            binding.inputPinCodeCommand to "",
            binding.inputPinCodeSmsCard to ""
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
                        layout.error = inputMapText[editText] // Устанавливаем ошибку
                    }
                }
            })
        }
    }

    // Функция для проверки валидности
    private fun isValidInput(editText: TextInputEditText, inputText: String): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()
        return when (editText.id) {
            R.id.inputIPDNS -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputAPN -> validDataSettingsDevice.apnValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputTextLoginGPRS -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputPasswordGPRS -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.charPROV_CHAR_MAXValid(inputText)
            R.id.inputTimeOutKeeplive -> validDataSettingsDevice.keepaliveValid(inputText.replace("\\s+".toRegex(), ""))
            R.id.inputTimeoutConnection -> validDataSettingsDevice.ctimeoutValid(inputText.replace("\\s+".toRegex(), ""))
            R.id.inputTCP -> validDataSettingsDevice.tcpPortValid(inputText.replace("\\s+".toRegex(), ""))
            R.id.inputPinCodeCommand, R.id.inputPinCodeSmsCard -> {
                if (binding.switchPinCodeSmsCommand.isChecked || binding.switchPinCodeSmsCard.isChecked) {
                    validDataSettingsDevice.simPasswordValid(inputText)
                } else true
            }
            else -> true
        }
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



    private fun updateViewPort(position: Int) {
        // если режим кумир нет
        if (binding.spinnerServer.selectedItemPosition != 0) {
            when(position) {
                0 -> {
                    binding.port1DisActivFon.visibility = View.GONE
                    binding.port2DisActivFon.visibility = View.VISIBLE
                }
                1 -> {
                    binding.port1DisActivFon.visibility = View.VISIBLE
                    binding.port2DisActivFon.visibility = View.GONE
                }
            }
        } else {
            binding.port1DisActivFon.visibility = View.GONE
            binding.port2DisActivFon.visibility = View.GONE
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

    // устанока конкретного режима приведет к ...
    private fun setPresetSpinnerServer(position: Int) {
        // если режим кумир нет
        if (binding.spinnerServer.selectedItemPosition != 0) {
            when(position) {
                0 -> {
                    binding.port1DisActivFon.visibility = View.GONE
                    binding.port2DisActivFon.visibility = View.VISIBLE
                }
                1 -> {
                    binding.port1DisActivFon.visibility = View.VISIBLE
                    binding.port2DisActivFon.visibility = View.GONE
                }
            }
        } else {
            binding.port1DisActivFon.visibility = View.GONE
            binding.port2DisActivFon.visibility = View.GONE
        }
    }

    private fun createAdapters() {
        // адаптер для выбора режима работы модема
        val itemsSpinnerDevMode = listOf(
            getString(R.string.devmodeKumirNet),
            getString(R.string.devmodeClient),
            getString(R.string.devmodeTCPServer),
            getString(R.string.devmodeGSMmodem),
            getString(R.string.devmodePipeClient),
            getString(R.string.devdodePipeServer)
        )
        // адаптер для выбора порта
        val itemsSpinnerActPort = listOf(
            "1",
            "2"
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
        val adapterActPort = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemsSpinnerActPort)
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

        adapterActPort.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

        binding.spinnerSelectActivPort.adapter = adapterActPort
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

    // функция для вставки данных настроек устройсва
    override fun printSettingDevice(settingMap: Map<String, String>) {


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
            val profile1: Int = settingMap[getString(R.string.commandGetProfile1)]?.
                replace("\n", "")?.
                replace(" ", "")?.toInt()!!
            val profile2: Int = settingMap[getString(R.string.commandGetProfile2)]?.
                replace("\n", "")?.
                replace(" ", "")?.toInt()!!

            // находим среди всех присетов индекс номера присета с настройками
            val context: Context = requireContext()
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
                    "\n${getString(R.string.commandGetProfile1)} = " +
                    "${settingMap[getString(R.string.commandGetProfile1)]}" +
                    "\n${getString(R.string.commandGetProfile2)} = " +
                    "${settingMap[getString(R.string.commandGetProfile2)]}")
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
        serialNumberGlobal = getString(R.string.serinerNumber) +
                "\n" + settingMap[getString(R.string.commandGetSerialNum)]
        binding.serinerNumber.text = serialNumberGlobal

        programVersionGlobal = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionFirmware)]
        binding.textVersionFirmware.text = programVersionGlobal

        binding.inputAPN.setText(settingMap[getString(R.string.commandGetApn)])
        binding.inputIPDNS.setText(settingMap[getString(R.string.commandGetServer1)])
        binding.inputTCP.setText(settingMap[getString(R.string.commandGetTcpPort)])
        binding.inputTextLoginGPRS.setText(settingMap[getString(R.string.commandGetLogin)])
        binding.inputPasswordGPRS.setText(settingMap[getString(R.string.commandGetPassword)])
        binding.inputTimeOutKeeplive.setText(settingMap[getString(R.string.commandGetKeepAlive)])
        binding.inputTimeoutConnection.setText(settingMap[getString(R.string.commandGetConnectionTimeout)])

        // установка переключетелей
        if (settingMap[getString(R.string.commandGetSimPin)]?.
            contains(getString(R.string.disabled)) == true){
            binding.switchPinCodeSmsCard.isChecked = false

            binding.inputPinCodeSmsCard.setText("")
        } else {
            binding.switchPinCodeSmsCard.isChecked = true

        }

        if (settingMap[getString(R.string.commandGetSmsPin)]?.
            contains(getString(R.string.disabled)) == true) {
            binding.switchPinCodeSmsCommand.isChecked = false

            binding.inputPinCodeCommand.setText("")
        } else {
            binding.switchPinCodeSmsCommand.isChecked = true

        }

        // работа со spiner (ражим работы)
        /*Режим работы модема.
            0 – kumirNet — подключение к ИИС КУМИР-Ресурс;
            1 – TCPClient — подключение модема к серверу опроса (пакетный режим);
            2 – TCPServer — подключение сервера опроса к модему (пакетный режим);
            3 – GSMmodem — прямое подключение GSM-модуля к порту 1;
            4 – PipeClient — подключение модема к серверу опроса (прозрачный режим);
            5 – PipeServer — подключение сервера опроса к модему (прозрачный режим);
        */
        try {
            settingMap[getString(R.string.commandGetDeviceMode)]?.let {
                binding.spinnerServer.setSelection(it.trim().toInt())
            }
        } catch (e: NumberFormatException) {
            showAlertDialog(getString(R.string.notReadDevModeDevice))
        }


        try {
            var activPort: Int = 0
            settingMap[getString(R.string.commandGetActivePort)]?.let {
                activPort = it.trim().toInt()-1
                binding.spinnerSelectActivPort.setSelection(activPort)
            }

            binding.port1DisActivFon.visibility = View.VISIBLE
            binding.port2DisActivFon.visibility = View.VISIBLE


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

            if (binding.spinnerServer.selectedItemPosition == 0 || activPort == 0) {
                binding.port1DisActivFon.visibility = View.GONE
            }





            // отоюражения настроек порта 2-------------------------------------------------------------
            val port2Config = settingMap[getString(R.string.commandGetPort2Config)]?.split(",")

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

            if (binding.spinnerServer.selectedItemPosition == 0 || activPort == 1) {
                binding.port2DisActivFon.visibility = View.GONE
            }

        } catch (e: NumberFormatException) {
            showAlertDialog(getString(R.string.notReadActPortDevice))
        }
    }

    override fun readSettingStart() {
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandGetVersionFirmware),
            getString(R.string.commandGetDeviceMode),
            getString(R.string.commandGetApn),
            getString(R.string.commandGetServer1),
            getString(R.string.commandGetTcpPort),
            getString(R.string.commandGetLogin),
            getString(R.string.commandGetPassword),
            getString(R.string.commandGetKeepAlive),
            getString(R.string.commandGetConnectionTimeout),
            getString(R.string.commandGetSmsPin),
            getString(R.string.commandGetSimPin),
            getString(R.string.commandGetActivePort),
            getString(R.string.commandGetPort1Config),
            getString(R.string.commandGetPort2Config),
            getString(R.string.commandGetProfile1),
            getString(R.string.commandGetProfile2)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }

    // запись данных в устройство
    override fun writeSettingStart() {

        // проверка на валидность всех полей
        if (!validAll()) return

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
            getString(R.string.commandSetDeviceMode) to binding.spinnerServer.selectedItemPosition.toString(),
            getString(R.string.commandSetApn) to binding.inputAPN.text.toString(),
            getString(R.string.commandSetServer1) to binding.inputIPDNS.text.toString(),
            getString(R.string.commandSetTcpPort) to binding.inputTCP.text.toString(),
            getString(R.string.commandSetLogin) to binding.inputTextLoginGPRS.text.toString(),
            getString(R.string.commandSetPassword) to binding.inputPasswordGPRS.text.toString(),
            getString(R.string.commandSetKeepAlive) to binding.inputTimeOutKeeplive.text.toString(),
            getString(R.string.commandSetConnectionTimeout) to binding.inputTimeoutConnection.text.toString(),
            getString(R.string.commandSetActivePort) to binding.spinnerSelectActivPort.selectedItem.toString(),
        )

        // загрузкак профайл
        val context: Context = requireContext()
        if (context is MainActivity) {
            try {
                dataMap[getString(R.string.commandSetProfile1)] = if (binding.spinnerSelectPort1MeteringDevice.selectedItemPosition != 0)
                    context.portsDeviceSetting[binding.spinnerSelectPort1MeteringDevice.selectedItemPosition].priset.toString()
                else "99"

                dataMap[getString(R.string.commandSetProfile2)] = if (binding.spinnerSelectPort2MeteringDevice.selectedItemPosition != 0)
                    context.portsDeviceSetting[binding.spinnerSelectPort2MeteringDevice.selectedItemPosition].priset.toString()
                else "99"
            } catch (e: Exception) {
                showAlertDialog(getString(R.string.nonValidData))
                return
            }
        }


        // смотря какой активный порт такие данные и будут аписываться
        if (binding.spinnerSelectActivPort.selectedItem.toString() == "1" ||
            binding.spinnerServer.selectedItemPosition == 0) {
            dataMap[getString(R.string.commandSetPort1Config)] =
                    binding.spinnerSpeed.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort1.selectedItem.toString() + "," +
                    parityPort1  + "," +
                    binding.spinnerSelectStopBitPort1.selectedItem.toString() +
                    ",200,2000"
        }
        if (binding.spinnerSelectActivPort.selectedItem.toString() == "2" ||
            binding.spinnerServer.selectedItemPosition == 0) {
            dataMap[getString(R.string.commandSetPort2Config)] =
                    binding.spinnerSpeedPort2.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort2.selectedItem.toString() + "," +
                    parityPort2  + "," +
                    binding.spinnerSelectStopBitPort2.selectedItem.toString() +
                    ",200,2000"
        }

        if (binding.switchPinCodeSmsCommand.isChecked) {
            if (binding.inputPinCodeCommand.text?.isNotEmpty() != false) {
                dataMap[getString(R.string.commandSetSmsPin)] =
                    binding.inputPinCodeCommand.text.toString()
            }
        } else {
            dataMap[getString(R.string.commandSetSmsPin)] = "0000"
        }

        if (binding.switchPinCodeSmsCard.isChecked) {
            if (binding.inputPinCodeSmsCard.text?.isNotEmpty() != false) {
                dataMap[getString(R.string.commandSetSimPin)] =
                    binding.inputPinCodeSmsCard.text.toString()
            }
        } else {
            dataMap[getString(R.string.commandSetSimPin)] = "0000"
        }

        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this)

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
        if (!validDataSettingsDevice.serverValid(binding.inputIPDNS.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputAPN.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputTextLoginGPRS.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputPasswordGPRS.text.toString())) {
            showAlertDialog(getString(R.string.errorRussionChar))
            return false
        }

        // проверки на валидность keepalive ctimeout tcpPort
        if (!validDataSettingsDevice.keepaliveValid(
                binding.inputTimeOutKeeplive.text.toString().replace("\\s+".toRegex(), ""))) {

            showAlertDialog(getString(R.string.errorKEEPALIVE))
            return false

        } else if (!validDataSettingsDevice.ctimeoutValid(
                binding.inputTimeoutConnection.text.toString().replace("\\s+".toRegex(), ""))) {
            showAlertDialog(getString(R.string.errorCTIMEOUT))
            return false

        } else if (!validDataSettingsDevice.tcpPortValid(
                binding.inputTCP.text.toString().replace("\\s+".toRegex(), ""))) {
            showAlertDialog(getString(R.string.errorTCPPORT))
            return false
        }

        // проверки на вaлидность 63 символа
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputAPN.text.toString()) || !validDataSettingsDevice.apnValid(binding.inputAPN.text.toString())) {
            showAlertDialog(getString(R.string.errorValidAPNFormat))
            return false
        }
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputIPDNS.text.toString())) {
            showAlertDialog(getString(R.string.errorValidIPDNS))
            return false
        }
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputTextLoginGPRS.text.toString())) {
            showAlertDialog(getString(R.string.errorValidLogin))
            return false
        }
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputPasswordGPRS.text.toString())) {
            showAlertDialog(getString(R.string.errorValidPassword))
            return false
        }
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputPasswordGPRS.text.toString())) {
            showAlertDialog(getString(R.string.errorValidPassword))
            return false
        }


        // проверка на 4 символа пароля сим и смс кода
        if (binding.switchPinCodeSmsCommand.isChecked) {
            if (binding.inputPinCodeCommand.text?.isNotEmpty() != false &&
                !validDataSettingsDevice.simPasswordValid(binding.inputPinCodeCommand.text.toString())) {
                showAlertDialog(getString(R.string.errorValidSim))
                return false
            }
        }

        if (binding.switchPinCodeSmsCard.isChecked) {
            if (binding.inputPinCodeSmsCard.text?.isNotEmpty() != false &&
                !validDataSettingsDevice.simPasswordValid(binding.inputPinCodeSmsCard.text.toString())) {
                showAlertDialog(getString(R.string.errorValidSim))
                return false
            }
        }

        return true
    }

    override fun printPriset(priset: Priset) {

        // закрытие меню
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.workFonDarkMenu()
        }
        // подставление данных в поля
        binding.inputAPN.setText(priset.apn)
        binding.inputTCP.setText(priset.tcpPort)
        binding.inputIPDNS.setText(priset.server1)
        binding.inputTextLoginGPRS.setText(priset.login)
        binding.inputPasswordGPRS.setText(priset.password)

        binding.spinnerServer.setSelection(priset.mode)
        setPresetSpinnerServer(priset.mode)

    }

}