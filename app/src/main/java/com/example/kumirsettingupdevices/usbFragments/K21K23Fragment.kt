package com.example.kumirsettingupdevices.usbFragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentK21k23Binding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment


class K21K23Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentK21k23Binding

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
        binding = FragmentK21k23Binding.inflate(inflater)
        return binding.root
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {
        TODO("Not yet implemented")
    }

}