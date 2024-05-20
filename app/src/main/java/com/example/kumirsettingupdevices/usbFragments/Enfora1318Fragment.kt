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
import com.example.kumirsettingupdevices.databinding.FragmentEnforma1318Binding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment
import com.example.testappusb.settings.ConstUsbSettings


class Enfora1318Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentEnforma1318Binding

    private val usbCommandsProtocol = UsbCommandsProtocol()
    private var flagClickChackSignal: Boolean = false

    private var NAME_TYPE_DEVICE = "Enfora1318"




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEnforma1318Binding.inflate(inflater)

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
            context.printDeviceTypeName(getString(R.string.enforma1318))
        }

        // настройки кликов
        binding.DisActivPort1SetiingsPriset.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEditSorPrisetSet))
        }

        binding.buttonChackSignal.setOnClickListener {
            onClickChackSignal()
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


    // при уничтожении он будет возвращять нормальные настроки порта
    override fun onDestroyView() {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.usb.onSelectUumBit(true)
            context.usb.onSerialParity(0)
            context.usb.onSerialStopBits(0)
            context.usb.onSerialSpeed(9)
        }

        usbCommandsProtocol.flagWorkChackSignal = false

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
            android.R.layout.simple_spinner_item, itemPortDeviceAccounting)
        val adapterSelectSpeed = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectSpeed)
        val adapterSelectParity = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectParity)
        val adapterSelectStopBit = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectStopBit)
        val adapterSelectBitData = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, itemSelectBitData)

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
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.curentData = NAME_TYPE_DEVICE // обход проверки индитификатора
            context.showTimerDialog(this, NAME_TYPE_DEVICE, false,false)
        }
    }

    private fun onClickWriteSettingsDevice(view: View) {
        writeSettingStart()
    }

    private fun onClickChackSignal() {
        if (!flagClickChackSignal) {
            usbCommandsProtocol.readSignalEnfora(getString(R.string.commandGetLevelSignalAndErrors),
                requireContext(), this)
            binding.buttonChackSignal.text = getString(R.string.ActivChackSignalTitle)

            flagClickChackSignal = true
        } else {
            usbCommandsProtocol.flagWorkChackSignal = false
            binding.buttonChackSignal.text = getString(R.string.chackSignalTitle)

            flagClickChackSignal = false
        }

    }
    fun onErrorStopChackSignal() {
        binding.buttonChackSignal.text = getString(R.string.ActivChackSignalTitle)
    }

    fun onPrintSignal(signal: String, errors: String) {
        binding.textLevelSignal.text = getString(R.string.LevelSignalTitle) + signal
        binding.textErrorSignal.text = getString(R.string.errorsSignalTitle) + errors

    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {

        if (settingMap[getString(R.string.commandGetSerialNumberEnforaM31)]?.contains("01152600") == true) {
            showAlertDialog(getString(R.string.notDeviceType) + getString(R.string.m31))
            return
        }

        // верийный номер и версия прошибки
        val serNum: String = getString(R.string.serinerNumber) +
                "\n" + settingMap[getString(R.string.commandGetSerialNum)]
        binding.serinerNumber.text = serNum

        // оеператор связи
        val operationGSM: String = getString(R.string.communicationOperatorTitle) +
                settingMap[getString(R.string.commandGetOperatirGSM)]
        binding.textCommunicationOperator.text = operationGSM


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

        if (settingMap[getString(R.string.commandGetApnEnforaM31)]?.contains(getString(R.string.deffoltAPN)) == false ||
            settingMap[getString(R.string.commandServer1EnforaOrM31)]?.contains(getString(R.string.deffoltSERVER1)) == false ||
            settingMap[getString(R.string.commandServer2EnforaOrM31)]?.contains(getString(R.string.deffoltSERVER2)) == false ||
            settingMap[getString(R.string.commandGetLoginPasswordEnforaM31)] == "0" ||
            settingMap[getString(R.string.commandGetTcpPortEnforaM31)] == getString(R.string.deffoltTCPPORT)) {

            showAlertDialog(getString(R.string.nonSettingDeviceNeedsFlashed) +
                    getString(R.string.commandGetApnEnforaM31) + "-" +
                    settingMap[getString(R.string.commandGetApnEnforaM31)] + "\n" +

                    getString(R.string.commandServer1EnforaOrM31) + "-" +
                    settingMap[getString(R.string.commandServer1EnforaOrM31)] + "\n" +

                    getString(R.string.commandServer2EnforaOrM31) + "-" +
                    settingMap[getString(R.string.commandServer2EnforaOrM31)] + "\n" +

                    getString(R.string.commandGetLoginPasswordEnforaM31) + "-" +
                    settingMap[getString(R.string.commandGetLoginPasswordEnforaM31)] + "\n" +

                    getString(R.string.commandGetTcpPortEnforaM31) + "-" +
                    settingMap[getString(R.string.commandGetTcpPortEnforaM31)]
            )
            return
        }

    }

    override fun readSettingStart() {
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandServer1EnforaOrM31),
            getString(R.string.commandServer2EnforaOrM31),
            getString(R.string.commandGetApnEnforaM31),
            getString(R.string.commandGetTcpPortEnforaM31),
            getString(R.string.commandGetLoginPasswordEnforaM31),
            getString(R.string.commandGetOperatirGSM)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this, true)
    }

    override fun writeSettingStart() {

    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }




}