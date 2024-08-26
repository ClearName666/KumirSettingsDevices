package com.kumir.settingupdevices.diag

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentACCB030DiagBinding
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment


class ACCB030DiagFragment : Fragment(), UsbFragment, DiagSiagnalIntarface {

    private lateinit var binding: FragmentACCB030DiagBinding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    var flagPermissionChackSignal: Boolean = false
    private var flagClickChackSignal: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentACCB030DiagBinding.inflate(inflater)

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

    override fun onErrorStopChackSignal() {
        flagClickChackSignal = false
        usbCommandsProtocol.flagWorkChackSignal = false
        binding.buttonChackSignal.text = getString(R.string.chackSignalTitle)
        binding.progressBarChackSignal.visibility = View.GONE
    }

    override fun onPrintSignal(signal: String, errors: String) {
        // переводим сигнал в проценты по формуле
        var signalLevel: Double = 0.0
        try {
            signalLevel = signal.replace(" ", "").toDouble()
            if (signalLevel != 99.0) {
                signalLevel = signalLevel / 32 * 100
            }
        } catch (_: Exception) {
            return
        }

        // ули ошибка равна 99 то 0
        var errorInt: Double = 0.0
        try {
            errorInt = errors.replace(" ", "").toDouble()
            if (errorInt == 99.0) errorInt = 0.0
        } catch (_: Exception) {
            return
        }

        val printSignal = "${getString(R.string.LevelSignalTitle)}  $signalLevel%"
        val printErrprs = "${getString(R.string.errorsSignalTitle)}  $errors%"

        binding.textLevelSignal.text = printSignal
        binding.textErrorSignal.text = printErrprs
    }

    override fun onPrintIP(ip: String) {
        binding.textIp.text = getString(R.string.ip) + " " + ip
    }


    override fun printSettingDevice(settingMap: Map<String, String>) {
        // вывод сериного номера
        val serialNumber: String = getString(R.string.serinerNumber) + "\n" +
                settingMap[getString(R.string.commandGetSerialNum)]
        binding.serinerNumber.text = serialNumber

        // вывод версионного номера
        val versionProgram: String = getString(R.string.versionProgram) +
                "\n" + settingMap[getString(R.string.commandGetVersionCore)]?.
            // peplace для того что бы убрать из ответа лишние и оставить только прошивку
        replace("\n", "")?.
        replace(getString(R.string.okSand), "")?.
        replace(getString(R.string.commandGetVersionProgramEnfora), "")
        binding.textVersionFirmware.text = versionProgram

        // оператор связи
        val operationGSM: String = getString(R.string.communicationOperatorTitle) +
                settingMap[getString(R.string.commandGetOperatirGSM)]?.substringAfter("\"")?.substringBefore("\"")
        binding.textCommunicationOperator.text = operationGSM

        flagPermissionChackSignal = true
    }

    override fun readSettingStart() {
        // чтение тольуо тогда когда отключен проверка сигнала
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandGetVersionCore),
            getString(R.string.commandGetOperatirGSM)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this, false)
    }

    override fun writeSettingStart() {}
    override fun lockFromDisconnected(connect: Boolean) {

    }

    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}
}