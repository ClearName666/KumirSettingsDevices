package com.example.kumirsettingupdevices.usbFragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentACCB030CoreBinding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment

class ACCB030CoreFragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentACCB030CoreBinding



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
        binding = FragmentACCB030CoreBinding.inflate(inflater)
        return binding.root
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }
}