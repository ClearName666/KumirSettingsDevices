package com.example.kumirsettingupdevices.usbFragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentEnforma1318Binding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment


class Enforma1318Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentEnforma1318Binding



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
        binding = FragmentEnforma1318Binding.inflate(inflater)
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