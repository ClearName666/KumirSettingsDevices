package com.kumir.settingupdevices.auto

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

        // вывод названия типа устройства
        mainContext.printDeviceTypeName(getString(R.string.autoFindDevice))


        binding.buttonStartAutoFindDevice.setOnClickListener {
            if (mainContext.usb.checkConnectToDevice())
                mainContext.showTimerDialogAutoFindDevice()
            else
                mainContext.showAlertDialog(getString(R.string.Usb_NoneConnect))
        }

        return binding.root
    }
}