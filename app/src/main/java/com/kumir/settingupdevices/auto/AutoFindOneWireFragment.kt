package com.kumir.settingupdevices.auto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentUtoFindOneWireBinding
import com.kumir.settingupdevices.usb.OneWire
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment


class AutoFindOneWireFragment(val mainContext: MainActivity) : Fragment(), UsbFragment {


    private lateinit var binding: FragmentUtoFindOneWireBinding
    override val usbCommandsProtocol = UsbCommandsProtocol()
    override fun printSerifalNumber(serialNumber: String) {}

    override fun printVersionProgram(versionProgram: String) {}

    override fun printSettingDevice(settingMap: Map<String, String>) {}

    override fun readSettingStart() {}

    override fun writeSettingStart() {}

    override fun lockFromDisconnected(connect: Boolean) {
        if (!connect) {
            binding.buttonStartAutoFindDevice.setOnClickListener {
                mainContext.showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        } else {
            binding.buttonStartAutoFindDevice.setOnClickListener {
                if (mainContext.usb.checkConnectToDevice()) {
                    startFindDeviceOneWire()
                } else {
                    mainContext.showAlertDialog(getString(R.string.Usb_NoneConnect))
                }
            }

        }
    }

    companion object {
        const val TIMEOUT_CHECK_END_ONE_WIRE_SCAN_ADDRESS: Long = 200
        const val BYTE_FIRST_DT112: String = "28"
        const val BYTE_FIRST_DKC2: String = "85"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentUtoFindOneWireBinding.inflate(inflater)

        // вывод названия типа устройства
        mainContext.printDeviceTypeName(getString(R.string.autoFindSensors))

        binding.buttonStartAutoFindDevice.setOnClickListener {
            if (mainContext.usb.checkConnectToDevice()) {
                startFindDeviceOneWire()
            } else {
                mainContext.showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        }

        return binding.root
    }


    @OptIn(ExperimentalStdlibApi::class)
    private fun startFindDeviceOneWire() {
        binding.fonLoadMenu.visibility = View.VISIBLE
        binding.loadMenuProgress.visibility = View.VISIBLE

        val oneWire: OneWire = OneWire(mainContext.usb, mainContext)

        usbCommandsProtocol.flagWorkRead = true
        usbCommandsProtocol.flagWorkOneWire = true
        oneWire.scanOneWireDevices(usbCommandsProtocol, null, mainContext, null)

        // поток ожидания результата
        Thread {
            // ожидем конец сканирования
            while (usbCommandsProtocol.flagWorkOneWire) {
                Thread.sleep(TIMEOUT_CHECK_END_ONE_WIRE_SCAN_ADDRESS)
            }

            // смотрим какой фемели код и запускаем соответствуюший фрагмент
            try {
                if (oneWire.listOneWireAddresHex[0].toHexString().startsWith(BYTE_FIRST_DKC2)) {
                    mainContext.runOnUiThread {
                        mainContext.onClickSensorPipeBlockage(binding.buttonStartAutoFindDevice)
                    }
                } else if (oneWire.listOneWireAddresHex[0].toHexString().startsWith(BYTE_FIRST_DT112)) {
                    mainContext.runOnUiThread {
                        mainContext.onClickSensorDT112(binding.buttonStartAutoFindDevice)
                    }
                } else {

                    // завершаем визуально
                    mainContext.runOnUiThread {
                        mainContext.showAlertDialog(getString(R.string.errorNoTypeDevice))
                        binding.fonLoadMenu.visibility = View.GONE
                        binding.loadMenuProgress.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {
                // завершаем визуально
                mainContext.runOnUiThread {
                    binding.fonLoadMenu.visibility = View.GONE
                    binding.loadMenuProgress.visibility = View.GONE
                    mainContext.showAlertDialog(getString(R.string.errorNoDevice))
                }
            }
        }.start()
    }

}