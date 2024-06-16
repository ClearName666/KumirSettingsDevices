package com.example.kumirsettingupdevices.diag

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.databinding.FragmentEnfora1318DiagBinding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment

class Enfora1318DiagFragment : Fragment(), UsbFragment, DiagSiagnalIntarface {
    private val usbCommandsProtocol = UsbCommandsProtocol()

    lateinit var binding: FragmentEnfora1318DiagBinding

    var flagPermissionChackSignal: Boolean = false
    private var flagClickChackSignal: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEnfora1318DiagBinding.inflate(inflater)

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.diagTitle))
        }

        // клик по кнопки диагностики
        binding.buttonChackSignal.setOnClickListener {
            onClickChackSignal()
        }

        return binding.root
    }

    override fun onDestroyView() {
        val context: Context = requireContext()

        // выключение потока чтения
        if (flagClickChackSignal)
            onClickChackSignal()


        // возврат к деффолтным настрокам
        if (context is MainActivity) {
            context.usb.onSelectUumBit(true)
            context.usb.onSerialParity(0)
            context.usb.onSerialStopBits(0)
            context.usb.onSerialSpeed(9)
            context.usb.flagAtCommandYesNo = false
        }

        super.onDestroyView()
    }

    private fun onClickChackSignal() {
        if (flagPermissionChackSignal) {
            if (!flagClickChackSignal) {
                usbCommandsProtocol.readSignalEnfora(getString(R.string.commandGetLevelSignalAndErrors),
                    requireContext(), this)
                binding.buttonChackSignal.text = getString(R.string.ActivChackSignalTitle)

                flagClickChackSignal = true


                // загруска тип работает проверка связи
                binding.progressBarChackSignal.visibility = View.VISIBLE

            } else {
                usbCommandsProtocol.flagWorkChackSignal = false
                binding.buttonChackSignal.text = getString(R.string.chackSignalTitle)

                flagClickChackSignal = false

                // не работает проверка связи загрузка отключена
                binding.progressBarChackSignal.visibility = View.GONE
            }
        } else {
            readSettingStart()
        }
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {
        // вывод сериного номера
        val serialNumber: String = getString(R.string.serinerNumber) + "\n" +
                settingMap[getString(R.string.commandGetSerialNum)]
        binding.serinerNumber.text = serialNumber

        // вывод версионного номера
        val versionProgram: String = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionProgramEnfora)]?.
                    // peplace для того что бы убрать из ответа лишние и оставить только прошивку
                    replace("\n", "")?.
                    replace(getString(R.string.okSand), "")?.
                    replace(getString(R.string.commandGetVersionProgramEnfora), "")
        binding.textVersionFirmware.text = versionProgram

        // оператор связи
        val operationGSM: String = getString(R.string.communicationOperatorTitle) +
                settingMap[getString(R.string.commandGetOperatirGSM)]
        binding.textCommunicationOperator.text = operationGSM

        flagPermissionChackSignal = true
    }

    override fun readSettingStart() {
        // чтение тольуо тогда когда отключен проверка сигнала
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandGetVersionProgramEnfora),
            getString(R.string.commandGetOperatirGSM)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this, true)
    }

    override fun onErrorStopChackSignal() {
        flagClickChackSignal = false
        binding.buttonChackSignal.text = getString(R.string.chackSignalTitle)
        binding.progressBarChackSignal.visibility = View.GONE
    }

    override fun onPrintSignal(signal: String, errors: String) {
        binding.textLevelSignal.text = getString(R.string.LevelSignalTitle) + signal
        binding.textErrorSignal.text = getString(R.string.errorsSignalTitle) + errors

    }

    override fun writeSettingStart() {}
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}

}