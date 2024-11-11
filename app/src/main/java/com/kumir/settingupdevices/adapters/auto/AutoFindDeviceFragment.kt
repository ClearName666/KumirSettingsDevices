package com.kumir.settingupdevices.adapters.auto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentAutoFindDeviceBinding


class AutoFindDeviceFragment(val mainContext: MainActivity) : Fragment() {

    private lateinit var binding: FragmentAutoFindDeviceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAutoFindDeviceBinding.inflate(inflater)

        binding.buttonStartAutoFindDevice.setOnClickListener {
            mainContext.showTimerDialogAutoFindDevice()
        }

        return binding.root
    }
}