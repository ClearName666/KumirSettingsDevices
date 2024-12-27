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
import com.kumir.settingupdevices.dataBasePreset.Enfora
import com.kumir.settingupdevices.databinding.FragmentM31Binding
import com.kumir.settingupdevices.modems.ModemDataW
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment
import com.kumir.testappusb.settings.ConstUsbSettings


class M31Fragment : Fragment(), UsbFragment, PrisetFragment<Enfora> {

    private lateinit var binding: FragmentM31Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()
    private var flagClickChackSignal: Boolean = false

    private var readOk: Boolean = false

    private var curentDataModem: Map<String, String> = mapOf()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentM31Binding.inflate(inflater)

        controlSpinnerForGoodValue()

        createAdapters()

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
            context.printDeviceTypeName(getString(R.string.m31))
        }

        // настройки кликов
        binding.DisActivPort1SetiingsPriset.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEditSorPresetSet))
        }

        binding.imageSelectPriset.setOnClickListener {
            if (context is MainActivity) {
                context.onClickPrisetEnforaSettingFor(this)
            }
        }

        // сохранения пресета настроек
        binding.buttonSavePreset.setOnClickListener {
            if (binding.inputNameSavePreset.text.toString().trim().isNotEmpty() ) {
                if (context is MainActivity) {
                    if (validAll()) {
                        context.onClickSavePreset(
                            binding.inputNameSavePreset.text.toString(),
                            binding.inputAPN.text.toString(),
                            binding.inputServer1.text.toString(),
                            binding.inputServer2.text.toString(),
                            binding.inputLogin.text.toString(),
                            binding.inputPassword.text.toString(),
                            binding.inputTimeOut.text.toString(),
                            binding.inputSizeBuffer.text.toString())
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

            if (!flagClickChackSignal) {
                onClickReadSettingsDevice(it)
            } else {
                showAlertDialog(getString(R.string.notUseSerialPort))
            }

        }
        binding.imageDownLoad.setOnClickListener {
            showAlertDialog(getString(R.string.nonWriteSetting))
        }

        // Установка обработчика нажатия на switch
        binding.switchCastomSet.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.fonCastomingSettings.visibility = View.GONE
                binding.switchCastomSet.background = null
            } else {
                binding.fonCastomingSettings.visibility = View.VISIBLE
            }
        }

        binding.fonCastomingSettings.setOnClickListener {
            binding.ScrollSetting.scrollTo(0, 200)
            binding.switchCastomSet.setBackgroundResource(R.color.dangerous)
        }

        setupInputValidation()

        return binding.root
    }


    private fun setupInputValidation() {
        // Карта для связи input с layout
        val inputMap = mapOf(
            binding.inputSizeBuffer to binding.inputSizeBufferLayout,
            binding.inputTimeOut to binding.inputTimeOutLayout,
            binding.inputServer1 to binding.inputServer1Layout,
            binding.inputServer2 to binding.inputServer2Layout,
            binding.inputAPN to binding.inputAPNLayout,
        )

        // Карта для связи input с текстом ошибки
        val inputMapText = mapOf(
            binding.inputSizeBuffer to getString(R.string.errorSizeBuffer),
            binding.inputTimeOut to getString(R.string.errorTimeOutEnfora),
            binding.inputServer1 to getString(R.string.errorValidM32DRS232RS485),
            binding.inputServer2 to getString(R.string.errorValidM32DRS232RS485),
            binding.inputAPN to getString(R.string.errorValidAPNFormat),
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
            R.id.inputSizeBuffer -> validDataSettingsDevice.padtoValid(inputText)
            R.id.inputTimeOut -> validDataSettingsDevice.padblkValid(inputText)
            R.id.inputServer1 -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.validServer(inputText)
            R.id.inputServer2 -> validDataSettingsDevice.serverValid(inputText) && validDataSettingsDevice.validServer(inputText)
            R.id.inputAPN -> validDataSettingsDevice.apnValid(inputText)
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
    }

    // при уничтожении он будет возвращять нормальные настроки порта
    override fun onDestroyView() {
        val context: Context = requireContext()

        try {
            // отключения потока прочитки сигнала если он включен
            if (usbCommandsProtocol.flagWorkChackSignal) {
                usbCommandsProtocol.flagWorkChackSignal = false
                usbCommandsProtocol.threadChackSignalEnfora.interrupt()
            }
        } catch (_: Exception) {}



        if (context is MainActivity) {
            context.usb.onSelectUumBit(true)
            context.usb.onSerialParity(0)
            context.usb.onSerialStopBits(0)
            context.usb.onSerialSpeed(9)
            context.usb.flagAtCommandYesNo = false
        }

        super.onDestroyView()
    }

    private fun createAdapters() {

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

        binding.spinnerSelectPort1MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort1.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort1.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort1.adapter = adapterSelectBitData
    }


    private fun onClickReadSettingsDevice(view: View) {
        readSettingStart()
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

        // прочтение прошло успешно
        readOk = true

        // текущие настройки сохраняются для сравнения в будещем
        curentDataModem = settingMap


        // верийный номер и версия прошибки
        val serNum: String = getString(R.string.serinerNumber) +
                "\n" + settingMap[getString(R.string.commandGetSerialNum)]?.replace("\"", "")
        binding.serinerNumber.text = serNum

        val version: String = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionProgramEnfora)]?.
            // peplace для того что бы убрать из ответа лишние и оставить только прошивку
        replace("\n", "")?.
        replace(getString(R.string.okSand), "")?.
        replace(getString(R.string.commandGetVersionProgramEnfora), "")
        binding.textVersionFirmware.text = version

        // вывод настроек

        val loginPassword: List<String>? = settingMap[getString(R.string.commandGetUsernamePassword)]?.
        substringAfter("\"")?.substringBefore("\"")?.split(",")


        // вывод настроек в inputы
        binding.inputAPN.setText(settingMap[getString(R.string.commandGetApnEnforaM31)]?.replace(" ", "")?.
        substringAfter("\",\"")?.substringBefore("\""))

        if (settingMap[getString(R.string.commandGetLoginPasswordEnforaM31)]?.contains("0") == false) {
            try {
                binding.inputLogin.setText(loginPassword?.get(0) ?: "")
                binding.inputPassword.setText(loginPassword?.get(1) ?: "")
            } catch (_: Exception) {
                binding.inputLogin.setText("")
                binding.inputPassword.setText("")
            }
        }


        // замена сервера с PADDST на FRIEND
        /* binding.inputServer1.setText(settingMap[getString(R.string.commandServer1EnforaOrM31)]?.replace(" ", "")?.
         substringAfter("\"")?.substringBefore("\""))*/

        binding.inputServer1.setText(settingMap[getString(R.string.commandServer2EnforaOrM31)]?.substringAfter("\"")
            ?.substringBefore("\"")
        )


        val pattern = """02,\s*(\d+),""".toRegex()
        val extractedValue = settingMap[getString(R.string.commandServer2EnforaOrM31)]?.let { input ->
            pattern.find(input)?.groupValues?.get(1)?.let { number ->
                input.substringAfter("02, $number,").substringBefore("\n")
                    .replace(" ", "").substringAfter("\"").substringBefore("\"")
            }
        }
        binding.inputServer2.setText(extractedValue)

        binding.inputTimeOut.setText(settingMap[getString(R.string.commandGetPadTimeout)]?.replace(" ", ""))
        binding.inputSizeBuffer.setText(settingMap[getString(R.string.commandGetPadBlockSize)]?.replace(" ", ""))



        // оеператор связи
        /*val operationGSM: String = getString(R.string.communicationOperatorTitle) +
                settingMap[getString(R.string.commandGetOperatirGSM)]
        binding.textCommunicationOperator.text = operationGSM*/


        // отоюражения настроек интерфейса----------------------------------------------------------
        binding.spinnerSpeed.setSelection(ConstUsbSettings.speedIndex)
        binding.spinnerBitDataPort1.setSelection(if (ConstUsbSettings.numBit) 0 else 1)
        binding.spinnerSelectParityPort1.setSelection(ConstUsbSettings.parityIndex)
        binding.spinnerSelectStopBitPort1.setSelection(ConstUsbSettings.stopBit)

        // проверка что данные настроек соответсвуют нужным
        /*
            * apn - kumir.dv
            * server1 - 172.27.0.15
            * server2 - 172.27.0.14
            * login пусто
            * password - пусто
            * tcpport - 6502
        */


        if (settingMap[getString(R.string.commandGetApnEnforaM31)]?.lowerCase()?.contains(getString(R.string.defaultAPN)) == false ||
            /*settingMap[getString(R.string.commandServer1EnforaOrM31)]?.contains(getString(R.string.defaultHelpCheckSERVER1)) == false ||*/
            settingMap[getString(R.string.commandServer2EnforaOrM31)]?.contains(getString(R.string.defaultHelpCheckSERVER2copySERVER1)) == false ||
            settingMap[getString(R.string.commandServer2EnforaOrM31)]?.contains(getString(R.string.defaultHelpCheckSERVER2)) == false ||
            settingMap[getString(R.string.commandGetLoginPasswordEnforaM31)]?.contains("0") == false ||
            settingMap[getString(R.string.commandGetTcpPortEnforaM31)]?.contains(getString(R.string.defaultTCPPORT)) == false) {

            showAlertDialog(getString(R.string.nonSettingDeviceNeedsFlashed) +
                    "\n\napn: ${binding.inputAPN.text.toString()}" +
                    "\nserver 1: ${binding.inputServer1.text.toString()}" +
                    "\nserver 2: ${binding.inputServer2.text.toString()}" +
                    "\ntcp port: ${settingMap[getString(R.string.commandGetTcpPortEnforaM31)]}" +
                    "\nтак же пароль и логин возможно не пусты "
            )

            return
        }
    }

    // функция для понижения регистра
    private fun String.lowerCase(): String = map {
        it.lowercaseChar()
    }.joinToString("")

    override fun readSettingStart() {
        // чтение тольуо тогда когда отключен проверка сигнала
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandGetVersionProgramEnfora),
            getString(R.string.commandServer1EnforaOrM31),
            getString(R.string.commandServer2EnforaOrM31),
            getString(R.string.commandGetApnEnforaM31),
            getString(R.string.commandGetTcpPortEnforaM31),
            getString(R.string.commandGetLoginPasswordEnforaM31),
            getString(R.string.commandGetOperatirGSM),


            getString(R.string.commandGetDisableAutoAttach),
            getString(R.string.commandGetAutoRegistration),
            getString(R.string.commandGetConfigureHostInterface),
            getString(R.string.commandGetPadBlockSize),
            getString(R.string.commandGetPadTimeout),
            getString(R.string.commandGetConfigureWakeup),
            getString(R.string.commandGetConfigureAck),
            getString(R.string.commandGetExecutePadCommand),
            getString(R.string.commandGetActivatePadConnection),
            getString(R.string.commandGetConnectionTimeoutEnfora),
            getString(R.string.commandGetIdleTimeout),
            getString(R.string.commandGetNetworkMonitor),
            getString(R.string.commandGetStoreAtEvents),
            getString(R.string.commandGetEventTimer),
            getString(R.string.commandGetEvent),
            getString(R.string.commandGetConfigureGPIO),
            getString(R.string.commandGetGPIOValue),
            getString(R.string.commandGetUsernamePassword)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this, true)
    }

    override fun writeSettingStart() {
        // для начала читаем настроки порта
        /*
            <format>
            1 = 8 data, 2 stop, no parity
            2 = 8 data, 1 stop,1 parity
            3 = 8 data, 1 stop,  no parity
            4 = 7 data, 2 stop, no parity
            5 = 7 data, 1 stop, 1 parity
            6 = 7 data, 1 stop, no parity

            <parity>
            0  = odd
            1 =  even
            2 = mark
        */

        // формула для вычисления нужного формата
        val format: Int = usbCommandsProtocol.calculateFormat(binding.spinnerBitDataPort1.selectedItemPosition,
            binding.spinnerSelectStopBitPort1.selectedItemPosition,
            if (binding.spinnerSelectParityPort1.selectedItemPosition == 0) 0 else 1)

        // если введены не валидные настроки порта
        if (format == 0) {
            showAlertDialog(getString(R.string.errorSettingPort))
            return
        }

        val parity: Int = if (binding.spinnerSelectParityPort1.selectedItemPosition == 2) 0 else 1

        val dataWrite: MutableMap<String, String>

        // если включены кастомные настроки то другая функция
        val modemDataW = ModemDataW(requireContext())

        dataWrite = if (binding.switchCastomSet.isChecked) {

            // проверка на валидность
            if(!validAll()) return

            val loginPassword: String = binding.inputLogin.text.toString().replace(" ", "")  +
                    "," + binding.inputPassword.text.toString().replace(" ", "")

            modemDataW.getEnfora1318DataWrite(curentDataModem, mapOf(
                getString(R.string.commandServer1EnforaOrM31) to binding.inputServer1.text.toString(),
                getString(R.string.commandServer2EnforaOrM31) to binding.inputServer2.text.toString(),
                getString(R.string.commandGetApnEnforaM31) to binding.inputAPN.text.toString(),
                getString(R.string.commandGetPadTimeout) to binding.inputTimeOut.text.toString(),
                getString(R.string.commandGetPadBlockSize) to binding.inputSizeBuffer.text.toString(),
                getString(R.string.commandGetUsernamePassword) to loginPassword
            ))
        } else {
            modemDataW.getEnfora1318DataWrite(curentDataModem, mapOf())
        }

        dataWrite[getString(R.string.commandSetSpeed)] = binding.spinnerSpeed.selectedItem.toString()
        dataWrite[getString(R.string.commandSetFormatParity)] = "$format,$parity"
        dataWrite[getString(R.string.commandSetSaveSettings)] = ""
        dataWrite[getString(R.string.commandSetResetModem)] = ""

        usbCommandsProtocol.writeSettingDevice(dataWrite, requireContext(), this, false, 5)

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
                onClickReadSettingsDevice(it)

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
        // проверка валидности введенных данных
        val validDataSettingsDevice = ValidDataSettingsDevice()
        if (!validDataSettingsDevice.padtoValid(binding.inputSizeBuffer.text.toString())) {
            showAlertDialog(getString(R.string.errorSizeBuffer))
            return false
        }
        if (!validDataSettingsDevice.padblkValid(binding.inputTimeOut.text.toString())) {
            showAlertDialog(getString(R.string.errorTimeOutEnfora))
            return false
        }

        // проверка на русские символы в серверах и apn
        if (!validDataSettingsDevice.serverValid(binding.inputServer1.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputServer2.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputAPN.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputLogin.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputPassword.text.toString())) {
            showAlertDialog(getString(R.string.errorRussionChar))
            return false
        }


        // проверка логина и пароля
        val loginPassword: String = binding.inputLogin.text.toString().replace(" ", "")  +
                "," + binding.inputPassword.text.toString().replace(" ", "")
        if (!validDataSettingsDevice.loginPasswordValid(loginPassword)) {
            showAlertDialog(getString(R.string.errorLoginPassworsd))
            return false
        }

        // проверкка на валидность сервера 1 и сервера 2
        if (!validDataSettingsDevice.validServer(binding.inputServer1.text.toString()) ||
            !validDataSettingsDevice.validServer(binding.inputServer2.text.toString())) {
            showAlertDialog(getString(R.string.errorValidServer))
            return false
        }

        if (!validDataSettingsDevice.apnValid(binding.inputAPN.text.toString())) {
            showAlertDialog(getString(R.string.errorValidAPNFormat))
            return false
        }

        return true
    }

    override fun printPriset(priset: Enfora) {
        // закрытие меню
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.workFonDarkMenu()
        }
        // подставление данных в поля
        binding.inputAPN.setText(priset.apn)
        binding.inputLogin.setText(priset.login)
        binding.inputPassword.setText(priset.password)
        binding.inputServer1.setText(priset.server1)
        binding.inputServer2.setText(priset.server2)
        binding.inputTimeOut.setText(priset.timeout)
        binding.inputSizeBuffer.setText(priset.sizeBuffer)
    }


}