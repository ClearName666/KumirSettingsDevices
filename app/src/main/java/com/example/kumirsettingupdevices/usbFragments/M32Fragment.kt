package com.example.kumirsettingupdevices.usbFragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        return binding.root
    }

    fun onClickReadSettings(view: View) {
        // вызываем метод для получения данных о девайсе
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
        if (settingMap.containsValue(getString(R.string.commandGetDeviceMode))) {
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
        }

    }
}