package com.example.kumirsettingupdevices.usbFragments

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
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.ValidDataSettingsDevice
import com.example.kumirsettingupdevices.databinding.FragmentM32Binding
import com.example.kumirsettingupdevices.settings.DeviceAccountingPrisets
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment


class M32Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentM32Binding

    private var NAME_TYPE_DEVICE = "KUMIR-M32 READY"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentM32Binding.inflate(inflater)

        createAdapters()

        // мониторинг изменений режима работы
        binding.spinnerServer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                // если режим кумир нэт
                if (position == 0) {
                    when(binding.spinnerSelectActivPort.selectedItemPosition) {
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

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        // изменения блакировки изменения настроек портов
        binding.spinnerSelectActivPort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                // если режим кумир нет
                if (binding.spinnerServer.selectedItemPosition == 0) {
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

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        // выбор присетов устройства учета порт 1
        binding.spinnerSelectPort1MeteringDevice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                when(position) {
                    0 -> {
                        // в случае если расширенныйе настроки то можно менять их
                        binding.DisActivPort1SetiingsPriset.visibility = View.GONE
                    }
                    1 -> {
                        binding.spinnerSpeed.setSelection(DeviceAccountingPrisets.SPT941Speed)
                        binding.spinnerSelectParityPort1.setSelection(DeviceAccountingPrisets.SPT941Parity)
                        binding.spinnerSelectStopBitPort1.setSelection(DeviceAccountingPrisets.SPT941StopBit)
                        binding.spinnerBitDataPort1.setSelection(DeviceAccountingPrisets.SPT941BitData)

                        binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                    }
                    2 -> {
                        binding.spinnerSpeed.setSelection(DeviceAccountingPrisets.SPT944Speed)
                        binding.spinnerSelectParityPort1.setSelection(DeviceAccountingPrisets.SPT944Parity)
                        binding.spinnerSelectStopBitPort1.setSelection(DeviceAccountingPrisets.SPT944StopBit)
                        binding.spinnerBitDataPort1.setSelection(DeviceAccountingPrisets.SPT944BitData)

                        binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                    }
                    3 -> {
                        binding.spinnerSpeed.setSelection(DeviceAccountingPrisets.TSP025Speed)
                        binding.spinnerSelectParityPort1.setSelection(DeviceAccountingPrisets.TSP025Parity)
                        binding.spinnerSelectStopBitPort1.setSelection(DeviceAccountingPrisets.TSP025StopBit)
                        binding.spinnerBitDataPort1.setSelection(DeviceAccountingPrisets.TSP025BitData)

                        binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                    }
                    4 -> {
                        binding.spinnerSpeed.setSelection(DeviceAccountingPrisets.PSCH4TMV23Speed)
                        binding.spinnerSelectParityPort1.setSelection(DeviceAccountingPrisets.PSCH4TMV23Parity)
                        binding.spinnerSelectStopBitPort1.setSelection(DeviceAccountingPrisets.PSCH4TMV23StopBit)
                        binding.spinnerBitDataPort1.setSelection(DeviceAccountingPrisets.PSCH4TMV23BitData)

                        binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                    }
                    5 -> {
                        binding.spinnerSpeed.setSelection(DeviceAccountingPrisets.TSP027Speed)
                        binding.spinnerSelectParityPort1.setSelection(DeviceAccountingPrisets.TSP027Parity)
                        binding.spinnerSelectStopBitPort1.setSelection(DeviceAccountingPrisets.TSP027StopBit)
                        binding.spinnerBitDataPort1.setSelection(DeviceAccountingPrisets.TSP027BitData)

                        binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                    }
                    6 -> {
                        binding.spinnerSpeed.setSelection(DeviceAccountingPrisets.PowerCE102MSpeed)
                        binding.spinnerSelectParityPort1.setSelection(DeviceAccountingPrisets.PowerCE102MParity)
                        binding.spinnerSelectStopBitPort1.setSelection(DeviceAccountingPrisets.PowerCE102MStopBit)
                        binding.spinnerBitDataPort1.setSelection(DeviceAccountingPrisets.PowerCE102MBitData)

                        binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                    }
                    7 -> {
                        binding.spinnerSpeed.setSelection(DeviceAccountingPrisets.Mercury206Speed)
                        binding.spinnerSelectParityPort1.setSelection(DeviceAccountingPrisets.Mercury206Parity)
                        binding.spinnerSelectStopBitPort1.setSelection(DeviceAccountingPrisets.Mercury206StopBit)
                        binding.spinnerBitDataPort1.setSelection(DeviceAccountingPrisets.Mercury206BitData)

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

                when(position) {
                    0 -> {
                        // в случае если расширенныйе настроки то можно менять их
                        binding.DisActivPort2SetiingsPriset.visibility = View.GONE
                    }
                    1 -> {
                        binding.spinnerSpeedPort2.setSelection(DeviceAccountingPrisets.SPT941Speed)
                        binding.spinnerSelectParityPort2.setSelection(DeviceAccountingPrisets.SPT941Parity)
                        binding.spinnerSelectStopBitPort2.setSelection(DeviceAccountingPrisets.SPT941StopBit)
                        binding.spinnerBitDataPort2.setSelection(DeviceAccountingPrisets.SPT941BitData)

                        binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
                    }
                    2 -> {
                        binding.spinnerSpeedPort2.setSelection(DeviceAccountingPrisets.SPT944Speed)
                        binding.spinnerSelectParityPort2.setSelection(DeviceAccountingPrisets.SPT944Parity)
                        binding.spinnerSelectStopBitPort2.setSelection(DeviceAccountingPrisets.SPT944StopBit)
                        binding.spinnerBitDataPort2.setSelection(DeviceAccountingPrisets.SPT944BitData)

                        binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
                    }
                    3 -> {
                        binding.spinnerSpeedPort2.setSelection(DeviceAccountingPrisets.TSP025Speed)
                        binding.spinnerSelectParityPort2.setSelection(DeviceAccountingPrisets.TSP025Parity)
                        binding.spinnerSelectStopBitPort2.setSelection(DeviceAccountingPrisets.TSP025StopBit)
                        binding.spinnerBitDataPort2.setSelection(DeviceAccountingPrisets.TSP025BitData)

                        binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
                    }
                    4 -> {
                        binding.spinnerSpeedPort2.setSelection(DeviceAccountingPrisets.PSCH4TMV23Speed)
                        binding.spinnerSelectParityPort2.setSelection(DeviceAccountingPrisets.PSCH4TMV23Parity)
                        binding.spinnerSelectStopBitPort2.setSelection(DeviceAccountingPrisets.PSCH4TMV23StopBit)
                        binding.spinnerBitDataPort2.setSelection(DeviceAccountingPrisets.PSCH4TMV23BitData)

                        binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
                    }
                    5 -> {
                        binding.spinnerSpeedPort2.setSelection(DeviceAccountingPrisets.TSP027Speed)
                        binding.spinnerSelectParityPort2.setSelection(DeviceAccountingPrisets.TSP027Parity)
                        binding.spinnerSelectStopBitPort2.setSelection(DeviceAccountingPrisets.TSP027StopBit)
                        binding.spinnerBitDataPort2.setSelection(DeviceAccountingPrisets.TSP027BitData)

                        binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
                    }
                    6 -> {
                        binding.spinnerSpeedPort2.setSelection(DeviceAccountingPrisets.PowerCE102MSpeed)
                        binding.spinnerSelectParityPort2.setSelection(DeviceAccountingPrisets.PowerCE102MParity)
                        binding.spinnerSelectStopBitPort2.setSelection(DeviceAccountingPrisets.PowerCE102MStopBit)
                        binding.spinnerBitDataPort2.setSelection(DeviceAccountingPrisets.PowerCE102MBitData)

                        binding.DisActivPort2SetiingsPriset.visibility = View.VISIBLE
                    }
                    7 -> {
                        binding.spinnerSpeedPort2.setSelection(DeviceAccountingPrisets.Mercury206Speed)
                        binding.spinnerSelectParityPort2.setSelection(DeviceAccountingPrisets.Mercury206Parity)
                        binding.spinnerSelectStopBitPort2.setSelection(DeviceAccountingPrisets.Mercury206StopBit)
                        binding.spinnerBitDataPort2.setSelection(DeviceAccountingPrisets.Mercury206BitData)

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
            showAlertDialog(getString(R.string.nonPortEditSorPrisetSet))
        }
        binding.DisActivPort2SetiingsPriset.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEditSorPrisetSet))
        }


        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice(it)

            // Обертываем наш Drawable для совместимости и изменяем цвет
            drawable?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)

                DrawableCompat.setTint(wrappedDrawable, Color.RED)

                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }

            // только после чтения
            binding.imageDownLoad.setOnClickListener {
                onClickWriteSettingsDevice(it)
            }
        }
        binding.imageDownLoad.setOnClickListener {
            showAlertDialog(getString(R.string.nonWriteSetting))
        }
        return binding.root
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
            getString(R.string.deviceAccountingMercury206)
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
            android.R.layout.simple_spinner_item, itemsSpinnerDevMode)
        val adapterActPort = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemsSpinnerActPort)
        val adapterPortDeviceAccounting = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemPortDeviceAccounting)
        val adapterSelectSpeed = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectSpeed)
        val adapterSelectParity = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectParity)
        val adapterSelectStopBit = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectStopBit)
        val adapterSelectBitData = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectBitData)

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

    private fun onClickReadSettingsDevice(view: View) {
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

        // сброс присетов настроек
        binding.spinnerSelectPort1MeteringDevice.setSelection(0)
        binding.spinnerSelectPort2MeteringDevice.setSelection(0)

        binding.DisActivPort1SetiingsPriset.visibility = View.GONE
        binding.DisActivPort2SetiingsPriset.visibility = View.GONE

        // верийный номер и версия прошибки
        val serNum: String = getString(R.string.serinerNumber) +
                "\n" + settingMap[getString(R.string.commandGetSerialNum)]
        binding.serinerNumber.text = serNum

        val version: String = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionFirmware)]
        binding.textVersionFirmware.text = version

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

            if (activPort == 0 || binding.spinnerServer.selectedItemPosition != 0) {

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

                binding.port1DisActivFon.visibility = View.GONE

            }
            if (activPort == 1  || binding.spinnerServer.selectedItemPosition != 0){


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
            getString(R.string.commandGetPort2Config)
        )

        val usbCommandsProtocol = UsbCommandsProtocol()
        usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }

    // запись данных в устройство
    override fun writeSettingStart() {

        val validDataSettingsDevice = ValidDataSettingsDevice()

        // проверки на валидность keepalive ctimeout tcpPort
        if (!validDataSettingsDevice.keepaliveValid(
                binding.inputTimeOutKeeplive.text.toString().replace("\\s+".toRegex(), ""))) {

            showAlertDialog(getString(R.string.errorKEEPALIVE))
            return

        } else if (!validDataSettingsDevice.ctimeoutValid(
                binding.inputTimeoutConnection.text.toString().replace("\\s+".toRegex(), ""))) {
            showAlertDialog(getString(R.string.errorCTIMEOUT))
            return

        } else if (!validDataSettingsDevice.tcpPortValid(
                binding.inputTCP.text.toString().replace("\\s+".toRegex(), ""))) {
            showAlertDialog(getString(R.string.errorTCPPORT))
            return
        }

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

        // смотря какой активный порт такие данные и будут аписываться
        if (binding.spinnerSelectActivPort.selectedItem.toString() == "1" ||
            binding.spinnerServer.selectedItemPosition != 0) {
            dataMap[getString(R.string.commandSetPort1Config)] =
                    binding.spinnerSpeed.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort1.selectedItem.toString() + "," +
                    parityPort1  + "," +
                    binding.spinnerSelectStopBitPort1.selectedItem.toString() +
                    ",200,2000"
        }
        if (binding.spinnerSelectActivPort.selectedItem.toString() == "2" ||
            binding.spinnerServer.selectedItemPosition != 0) {
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

        val usbCommandsProtocol = UsbCommandsProtocol()
        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this)

    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

}