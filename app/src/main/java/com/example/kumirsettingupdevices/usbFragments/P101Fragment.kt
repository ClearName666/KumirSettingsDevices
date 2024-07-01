package com.example.kumirsettingupdevices.usbFragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentP101Binding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment

class P101Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentP101Binding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentP101Binding.inflate(inflater)

        // назначение клика на меню что бы добавлять и удалять данные
        binding.fonWindowDarck.setOnClickListener {
            binding.editMenuAbanent.visibility = View.GONE
            binding.fonWindowDarck.visibility = View.GONE
        }
        // клик добавления абанента
        binding.buttonAddAbanent.setOnClickListener {
            binding.fonWindowDarck.visibility = View.VISIBLE
            binding.editMenuAbanent.visibility = View.VISIBLE
        }

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

    override fun readSettingStart() {
        TODO("Not yet implemented")
    }

    override fun writeSettingStart() {
        TODO("Not yet implemented")
    }

    override fun lockFromDisconnected(connect: Boolean) {

    }
}