package com.example.kumirsettingupdevices.usbFragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.ValidDataSettingsDevice
import com.example.kumirsettingupdevices.databinding.FragmentM32Binding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment


class M32Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentM32Binding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentM32Binding.inflate(inflater)

        // адаптер для выбора режима работы модема
        val itemsSpinnerDevMode = listOf(
            getString(R.string.devmodeKumirNet),
            getString(R.string.devmodeClient),
            getString(R.string.devmodeTCPServer),
            getString(R.string.devmodeGSMmodem),
            getString(R.string.devmodePipeClient),
            getString(R.string.devdodePipeServer)
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, itemsSpinnerDevMode)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerServer.adapter = adapter

        // адаптер для выбора порта
        val itemsSpinnerActPort = listOf(
            "1",
            "2"
        )

        val adapterActPort = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, itemsSpinnerActPort)
        adapterActPort.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSelectActivPort.adapter = adapterActPort

        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice(it)
        }

        binding.imageDownLoad.setOnClickListener {
            onClickWriteSettingsDevice(it)
        }

        return binding.root
    }

    private fun onClickReadSettingsDevice(view: View) {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.showTimerDialog(this)
        }
    }

    private fun onClickWriteSettingsDevice(view: View) {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.showTimerDialog(this, true)
        }
    }


    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    // функция для вставки данных настроек устройсва
    override fun printSettingDevice(settingMap: Map<String, String>) {

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
        if (settingMap[getString(R.string.commandGetSmsPin)]?.
            contains(getString(R.string.disabled)) == true) {
            binding.switchPinCodeSmsCommand.isChecked = false
        } else {
            binding.switchPinCodeSmsCommand.isChecked = true

            binding.inputPinCodeSmsCard.setText(settingMap[getString(R.string.commandGetSmsPin)])
        }

        if (settingMap[getString(R.string.commandGetSimPin)]?.
            contains(getString(R.string.disabled)) == true) {
            binding.switchPinCodeSmsCard.isChecked = false
        } else {
            binding.switchPinCodeSmsCard.isChecked = true

            binding.inputPinCodeCommand.setText(settingMap[getString(R.string.commandGetSimPin)])
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
            settingMap[getString(R.string.commandGetActivePort)]?.let {
                binding.spinnerSelectActivPort.setSelection(it.trim().toInt()-1)
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
            getString(R.string.commandGetActivePort)
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

        val dataMap: MutableMap<String, String> = mutableMapOf(
            getString(R.string.commandSetDeviceMode) to binding.spinnerServer.selectedItemPosition.toString(),
            getString(R.string.commandSetApn) to binding.inputAPN.text.toString(),
            getString(R.string.commandSetServer1) to binding.inputIPDNS.text.toString(),
            getString(R.string.commandSetTcpPort) to binding.inputTCP.text.toString(),
            getString(R.string.commandSetLogin) to binding.inputTextLoginGPRS.text.toString(),
            getString(R.string.commandSetPassword) to binding.inputPasswordGPRS.text.toString(),
            getString(R.string.commandSetKeepAlive) to binding.inputTimeOutKeeplive.text.toString(),
            getString(R.string.commandSetConnectionTimeout) to binding.inputTimeoutConnection.text.toString(),
            getString(R.string.commandSetActivePort) to binding.spinnerSelectActivPort.selectedItem.toString()
        )
        if (binding.switchPinCodeSmsCard.isChecked) {
            dataMap[getString(R.string.commandSetSimPin)] =
                binding.inputPinCodeCommand.text.toString()
        }
        if (binding.switchPinCodeSmsCommand.isChecked) {
            dataMap[getString(R.string.commandSetSmsPin)] =
                binding.inputPinCodeSmsCard.text.toString()
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