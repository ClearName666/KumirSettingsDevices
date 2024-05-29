package com.example.kumirsettingupdevices

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentDiagBinding

class DiagFragment(val serialNumber: String, val programVersion: String) : Fragment() {

    private lateinit var binding: FragmentDiagBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagBinding.inflate(inflater)

        // верийный номер и версия прошибки
        val serNum: String = serialNumber
        binding.serinerNumber.text = serNum

        val version: String = programVersion
        binding.textVersionFirmware.text = version

        return binding.root
    }

}