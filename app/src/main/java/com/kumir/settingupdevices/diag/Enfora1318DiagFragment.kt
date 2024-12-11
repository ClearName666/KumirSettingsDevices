package com.kumir.settingupdevices.diag

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentEnfora1318DiagBinding
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment

class Enfora1318DiagFragment : Fragment(), UsbFragment, DiagSiagnalIntarface {
    override val usbCommandsProtocol = UsbCommandsProtocol()

    private lateinit var binding: FragmentEnfora1318DiagBinding

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
                settingMap[getString(R.string.commandGetSerialNum)]?.replace("\"", "")
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
        if (settingMap[getString(R.string.commandGetOperatirGSM)]?.substringAfter("\"")?.substringBefore("\"")?.contains("0") == false) {
            val operationGSM: String = getString(R.string.communicationOperatorTitle) +
                    settingMap[getString(R.string.commandGetOperatirGSM)]?.substringAfter("\"")?.substringBefore("\"")
            binding.textCommunicationOperator.text = operationGSM
        }


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

        // ecли ошибка равна 99 то 0
        var errorInt: Double = 0.0
        try {
            errorInt = errors.replace(" ", "").toDouble()
            if (errorInt == 99.0) errorInt = 0.0
        } catch (_: Exception) {
            return
        }

        val printSignal = "${getString(R.string.LevelSignalTitle)}  $signalLevel%"
        val printErrprs = "${getString(R.string.errorsSignalTitle)}  $errorInt%"

        binding.textLevelSignal.text = printSignal
        binding.textErrorSignal.text = printErrprs
    }

    override fun onPrintIP(ip: String) {
        binding.textIp.text = getString(R.string.ip) + " " + ip
    }

    override fun writeSettingStart() {}
    override fun lockFromDisconnected(connect: Boolean) {

    }

    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}

}