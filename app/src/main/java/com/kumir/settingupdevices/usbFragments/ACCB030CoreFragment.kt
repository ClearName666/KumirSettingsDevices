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
import com.kumir.settingupdevices.databinding.FragmentACCB030CoreBinding
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.model.recyclerModel.Priset
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment

class ACCB030CoreFragment : Fragment(), UsbFragment, PrisetFragment<Priset> {

    private lateinit var binding: FragmentACCB030CoreBinding

    override val usbCommandsProtocol = UsbCommandsProtocol()
    private var flagClickChackSignal: Boolean = false

    private var readOk: Boolean = false

    private var NAME_TYPE_DEVICE = "KUMIR-VZLET_ASSV030 READY"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentACCB030CoreBinding.inflate(inflater)
        createAdapters()

        // выбор присетов устройства учета порт 1
        binding.spinnerSelectPort1MeteringDevice.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
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
                            binding.spinnerSpeed.setSelection(context.portsDeviceSetting[position - 1].speed)
                            binding.spinnerSelectParityPort1.setSelection(context.portsDeviceSetting[position - 1].parity)
                            binding.spinnerSelectStopBitPort1.setSelection(context.portsDeviceSetting[position - 1].stopBit)
                            binding.spinnerBitDataPort1.setSelection(context.portsDeviceSetting[position - 1].bitData)

                            binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
                        }

                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.accb030Core))
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

        // настройки кликов
        binding.DisActivPort1SetiingsPriset.setOnClickListener {
            showAlertDialog(getString(R.string.nonPortEditSorPresetSet))
        }


        // сохранения пресета настроек
        binding.buttonSavePreset.setOnClickListener {
            if (binding.inputNameSavePreset.text.toString().isNotEmpty()) {
                if (context is MainActivity) {
                    if (validAll() ) {
                        context.onClickSavePreset(
                            binding.inputNameSavePreset.text.toString(),
                            0,
                            binding.inputAPN.text.toString(),
                            binding.inputIPDNS.text.toString(),
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
                onClickReadSettingsDevice()
            } else {
                showAlertDialog(getString(R.string.notUseSerialPort))
            }
        }

        binding.imageDownLoad.setOnClickListener {
            showAlertDialog(getString(R.string.notReadDevice))
        }


        if (context is MainActivity && !context.usb.checkConnectToDevice()) {
            lockFromDisconnected(false)
        }

        return binding.root
    }



    override fun onDestroyView() {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.usb.flagAtCommandYesNo = false
        }

        super.onDestroyView()
    }


    private fun onClickReadSettingsDevice() {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.showTimerDialog(this, NAME_TYPE_DEVICE)
        }
    }

    private fun onClickWriteSettingsDevice(view: View) {
        val validDataSettingsDevice = ValidDataSettingsDevice()
        // проверка на русские символы в серверах и apn
        if (!validDataSettingsDevice.serverValid(binding.inputIPDNS.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputAPN.text.toString())) {
            showAlertDialog(getString(R.string.errorRussionChar))
            return
        }

        writeSettingStart()
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


        val adapterPortDeviceAccounting = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner, itemPortDeviceAccounting
        )
        val adapterSelectSpeed = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner, itemSelectSpeed
        )
        val adapterSelectParity = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner, itemSelectParity
        )
        val adapterSelectStopBit = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner, itemSelectStopBit
        )
        val adapterSelectBitData = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner, itemSelectBitData
        )

        adapterPortDeviceAccounting.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        adapterSelectParity.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        adapterSelectStopBit.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        adapterSelectBitData.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerSelectPort1MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort1.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort1.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort1.adapter = adapterSelectBitData
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

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


        // прочтение прошло успешно
        readOk = true

        // верийный номер и версия прошибки
        val serialNumber: String = getString(R.string.serinerNumber) +
                "\n" + settingMap[getString(R.string.commandGetSerialNum)]
        binding.serinerNumber.text = serialNumber

        val programVersion: String = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionFirmware)]
        binding.textVersionFirmware.text = programVersion

        binding.inputAPN.setText(settingMap[getString(R.string.commandGetApn)])
        binding.inputIPDNS.setText(settingMap[getString(R.string.commandGetServer1)])
        binding.inputTextLoginGPRS.setText(settingMap[getString(R.string.commandGetLogin)])
        binding.inputPasswordGPRS.setText(settingMap[getString(R.string.commandGetPassword)])
        binding.inputTimeOutKeeplive.setText(settingMap[getString(R.string.commandGetKeepAlive)])
        binding.inputTimeoutConnection.setText(settingMap[getString(R.string.commandGetConnectionTimeout)])

        // вывод присетов настроек
        var preset1: Int = 0
        try {
            // переводим данные пресетов настроек в инт
            val profile1: Int = settingMap[getString(R.string.commandGetProfile1)]?.
            replace("\n", "")?.
            replace(" ", "")?.toInt()!!

            // находим среди всех присетов индекс номера присета с настройками
            if (context is MainActivity) {
                for (itemPreset in 0..<context.portsDeviceSetting.size) {
                    if (profile1 == context.portsDeviceSetting[itemPreset].priset) {
                        preset1 = itemPreset
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // не валидные данные
            showAlertDialog(getString(R.string.nonValidData) +
                    "\n${getString(R.string.commandGetProfile1)} = " +
                    "${settingMap[getString(R.string.commandGetProfile1)]}")
        }

        binding.spinnerSelectPort1MeteringDevice.setSelection(preset1)

        // если профайл не 0 то
        if (preset1 != 0) {
            binding.DisActivPort1SetiingsPriset.visibility = View.VISIBLE
        }

        // установка переключетелей
        if (settingMap[getString(R.string.commandGetSimPin)]?.
            contains(getString(R.string.disabled)) == true){
            binding.switchPinCodeSmsCard.isChecked = false

            binding.inputPinCodeSmsCard.setText("")
        } else {
            binding.switchPinCodeSmsCard.isChecked = true

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

        } catch (e: NumberFormatException) {
            showAlertDialog(getString(R.string.notReadActPortDevice))
        }

        // выввод номера телефона диспетчиера
        binding.inputNumberPhoneDis.setText(settingMap[getString(R.string.commandGetAbonent)])

        // вывод ip адреса сервера
        binding.inputGSMOper.setText(settingMap[getString(R.string.commandGetAbonServer)])

        // вывод времени входящего звонка
        binding.inputMaxTimeCall.setText(settingMap[getString(R.string.commandGetTimeAbonent)])
    }

    override fun readSettingStart() {
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandGetVersionFirmware),
            getString(R.string.commandGetProfile1),
            getString(R.string.commandGetPort1Config),
            getString(R.string.commandGetApn),
            getString(R.string.commandGetKeepAlive),
            getString(R.string.commandGetServer1),
            getString(R.string.commandGetConnectionTimeout),
            getString(R.string.commandGetSimPin),
            getString(R.string.commandGetLogin),
            getString(R.string.commandGetPassword),
            getString(R.string.commandGetAbonent),
            getString(R.string.commandGetAbonServer),
            getString(R.string.commandGetTimeAbonent)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }

    override fun writeSettingStart() {

        // проверка на валидность
        if (!validAll()) return

        var parityPort1 = "N"
        when(binding.spinnerSelectParityPort1.selectedItemPosition) {
            0 -> parityPort1  = "N"
            1 -> parityPort1  = "E"
            2 -> parityPort1  = "O"
        }

        val dataMap: MutableMap<String, String> = mutableMapOf(
            getString(R.string.commandSetApn) to binding.inputAPN.text.toString(),
            getString(R.string.commandSetServer1) to binding.inputIPDNS.text.toString(),
            getString(R.string.commandSetLogin) to binding.inputTextLoginGPRS.text.toString(),
            getString(R.string.commandSetPassword) to binding.inputPasswordGPRS.text.toString(),
            getString(R.string.commandSetKeepAlive) to binding.inputTimeOutKeeplive.text.toString(),
            getString(R.string.commandSetConnectionTimeout) to binding.inputTimeoutConnection.text.toString()
        )

        // загрузкак профайл
        val context: Context = requireContext()
        if (context is MainActivity) {
            try {
                dataMap[getString(R.string.commandSetProfile1)] =
                    context.portsDeviceSetting[binding.spinnerSelectPort1MeteringDevice.selectedItemPosition].
                    priset.toString()
            } catch (e: Exception) {
                showAlertDialog(getString(R.string.nonValidData))
                return
            }
        }

        dataMap[getString(R.string.commandSetPort1Config)] =
            binding.spinnerSpeed.selectedItem.toString() + "," +
                    binding.spinnerBitDataPort1.selectedItem.toString() + "," +
                    parityPort1  + "," +
                    binding.spinnerSelectStopBitPort1.selectedItem.toString() +
                    ",200,2000"

        if (binding.switchPinCodeSmsCard.isChecked) {
            if (binding.inputPinCodeSmsCard.text?.isNotEmpty() != false) {
                dataMap[getString(R.string.commandSetSimPin)] =
                    binding.inputPinCodeSmsCard.text.toString()
            }
        } else {
            dataMap[getString(R.string.commandSetSimPin)] = "0000"
        }

        dataMap[getString(R.string.commandSetAbonentCore)] = binding.inputNumberPhoneDis.text.toString()
        dataMap[getString(R.string.commandSetAbonServer)] = binding.inputGSMOper.text.toString()
        dataMap[getString(R.string.commandSetTimeAbonent)] = binding.inputMaxTimeCall.text.toString()

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


    // проверка валидности
    private fun validAll(): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()

        if (!validDataSettingsDevice.validTimeAbonent(binding.inputMaxTimeCall.text.toString())) {
            showAlertDialog(getString(R.string.errorTimeCall))
            return false
        }

        if (!validDataSettingsDevice.validServer(binding.inputGSMOper.text.toString())) {
            showAlertDialog(getString(R.string.errorValidServerCore))
            return false
        }

        if (!validDataSettingsDevice.isValidPhoneNumber(binding.inputNumberPhoneDis.text.toString())) {
            showAlertDialog(getString(R.string.errorValidNumberPhone))
            return false
        }

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

        }

        // проверки на вaлидность 63 символа
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputAPN.text.toString())) {
            showAlertDialog(getString(R.string.errorValidAPN))
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
        binding.inputIPDNS.setText(priset.server1)
        binding.inputTextLoginGPRS.setText(priset.login)
        binding.inputPasswordGPRS.setText(priset.password)


    }
}