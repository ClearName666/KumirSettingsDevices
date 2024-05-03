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
import com.example.kumirsettingupdevices.databinding.FragmentM32Binding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment


class M32Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentM32Binding

    override fun onResume() {
        super.onResume()

        // вызов метода который выведет серийник и версию
        val usbCommandsProtocol = UsbCommandsProtocol()
        usbCommandsProtocol.serinerNumberAndVersionFirmware(requireContext(), this)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentM32Binding.inflate(inflater)


        // адаптер для выбора режима работы модема
        val items = listOf(
            getString(R.string.devmodeKumirNet),
            getString(R.string.devmodeClient),
            getString(R.string.devmodeTCPServer),
            getString(R.string.devmodeGSMmodem),
            getString(R.string.devmodePipeClient),
            getString(R.string.devdodePipeServer)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerServer.adapter = adapter

        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice(it)
        }

        return binding.root
    }

    private fun onClickReadSettingsDevice(view: View) {
        // вызываем метод для получения данных о девайсе

        /*val context: Context = requireContext()
        if (context is MainActivity) {
            context.showTimerDialog()
        }*/

        val command: List<String> = arrayListOf(
            getString(R.string.commandGetDeviceMode),
            getString(R.string.commandGetApn),
            getString(R.string.commandGetServer1),
            getString(R.string.commandGetTcpPort),
            getString(R.string.commandGetLogin),
            getString(R.string.commandGetPassword),
            getString(R.string.commandGetKeepAlive),
            getString(R.string.commandGetConnectionTimeout),
            getString(R.string.commandGetPort1Config),
            getString(R.string.commandGetSmsPin),
            getString(R.string.commandGetSimPin)
        )

        val usbCommandsProtocol = UsbCommandsProtocol()
        usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }


    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    // функция для вставки данных настроек устройсва
    override fun printSettingDevice(settingMap: Map<String, String>) {

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

            binding.inputPinCodeCommand.setText(settingMap[getString(R.string.commandGetSmsPin)])
        }

        if (settingMap[getString(R.string.commandGetSimPin)]?.
            contains(getString(R.string.disabled)) == true) {
            binding.switchPinCodeSmsCard.isChecked = false
        } else {
            binding.switchPinCodeSmsCard.isChecked = true

            binding.inputPinCodeSmsCard.setText(settingMap[getString(R.string.commandGetSimPin)])
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
            val context: Context = requireContext()
            if (context is MainActivity) {
                context.showAlertDialog(getString(R.string.notReadDevModeDevice))
            }
        }




        /*if (settingMap.containsValue(getString(R.string.commandGetDeviceMode))) {
            // сдесь в будещем дописать когда будет адаптер для спинера
        }
        if (settingMap.containsValue(getString(R.string.commandGetApn))) {
            binding.inputAPN.setText(settingMap[getString(R.string.commandGetApn)])
        }
        if (settingMap.containsValue(getString(R.string.commandGetServer1))) {
            binding.inputIPDNS.setText(settingMap[getString(R.string.commandGetServer1)])
        }
        if (settingMap.containsValue(getString(R.string.commandGetTcpPort))) {
            binding.inputTCP.setText(settingMap[getString(R.string.commandGetTcpPort)])
        }
        if (settingMap.containsValue(getString(R.string.commandGetLogin))) {
            binding.inputTextLoginGPRS.setText(settingMap[getString(R.string.commandGetLogin)])
        }
        if (settingMap.containsValue(getString(R.string.commandGetPassword))) {
            binding.inputPasswordGPRS.setText(settingMap[getString(R.string.commandGetPassword)])
        }
        if (settingMap.containsValue(getString(R.string.commandGetKeepAlive))) {
            binding.inputTimeOutKeeplive.setText(settingMap[getString(R.string.commandGetKeepAlive)])
        }
        if (settingMap.containsValue(getString(R.string.commandGetConnectionTimeout))) {
            binding.inputTimeoutConnection.setText(getString(R.string.commandGetConnectionTimeout))
        }
        if (settingMap.containsValue(getString(R.string.commandGetPort1Config))) {
            // сдесь в будещем дописать когда будет адаптер для спинера
        }
        if (settingMap.containsValue(getString(R.string.commandGetSmsPin))) {
            binding.inputPinCodeCommand.setText(settingMap[getString(R.string.commandGetSmsPin)])
        }
        if (settingMap.containsValue(getString(R.string.commandGetSimPin))) {
            binding.inputPinCodeSmsCard.setText(settingMap[getString(R.string.commandGetSimPin)])
        }*/

    }

}