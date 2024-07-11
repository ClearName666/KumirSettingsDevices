package com.example.kumirsettingupdevices.diag

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemPingrecvAdapter.ItemPingrecvAdapter
import com.example.kumirsettingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.example.kumirsettingupdevices.databinding.FragmentDiagBinding
import com.example.kumirsettingupdevices.databinding.FragmentDiagM32DBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemOperator
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbDiag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DiagM32DFragment(val nameDeviace: String) : Fragment(), UsbDiag, DiagFragmentInterface {

    private lateinit var binding: FragmentDiagM32DBinding

    private val usbCommandsProtocol = UsbCommandsProtocol()

    private var flagStartDiag: Boolean = false
    private var flagViewDiag: Boolean = true


    companion object {
        const val DROP_START_FOR_DATA: Int = 2
        const val DROP_END_FOR_DATA: Int = 2

        // задержка для анимации загрузки операторов
        const val TIMEOUT_ANIM_LOADING_OPERATORS: Long = 1000

        // для градации сигнала
        private const val SIGNAL_1: Int = 90
        private const val SIGNAL_2: Int = 80
        private const val SIGNAL_3: Int = 70
        private const val SIGNAL_4: Int = 60

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagM32DBinding.inflate(inflater)

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.diagTitle))
        }

        // назначение кликов
        binding.buttonDiagStart.setOnClickListener {
            onClickStartDiag()
        }


        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // вывод предупреждения об диагностики
        showAlertDialog(getString(R.string.exitDiagRestart))
    }

    override fun onDestroyView() {

        // отключение систем проверки сигнала и выход из потока
        try {
            // отключения потока прочитки сигнала если он включен
            if (usbCommandsProtocol.flagWorkDiag) {
                //usbCommandsProtocol.threadDiag.interrupt()
                usbCommandsProtocol.flagWorkDiag = false
            }
        } catch (e: Exception) {}


        // очещение буфера данных
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.curentData = ""
        }

        super.onDestroyView()
    }

    // запсук диагностики
    private fun onClickStartDiag() {
        if (!flagStartDiag) {
            val context: Context = requireContext()

            if (context is MainActivity) {
                context.showTimerDialogDiag(this, nameDeviace)
            }
        }
    }



    override fun printAllInfo(info: String) {
        binding.progressBarData.visibility = View.GONE

        // насло с ок и заканичается на CELLSCAN
        binding.textDiag.text = info.substringAfter(getString(R.string.okSand)).
        substringBefore(getString(R.string.endDiagBeginning)).drop(DROP_START_FOR_DATA).dropLast(
            DROP_END_FOR_DATA
        )
    }

    override fun printAllOperator(allOperators: String) {

        val operator = allOperators.substringBefore("\n")

        //  разрешение показа операторов
        flagViewDiag = true

        binding.progressBarOperators.visibility = View.GONE

        if (operator.contains("SIM1: ")) {
            binding.progressBarOperators.visibility = View.GONE

            binding.Sim1Layout.visibility = View.VISIBLE

            // вывод данныех об сим карте
            binding.textOperatorSim1.text = operator.substringAfter("[").substringBefore("]")
            binding.textSignalSim1.text = operator.substringAfter("% ")

            // отображения картиночки
            if (operator.contains("MegaFon")) {
                binding.imageOperatorSim1.setBackgroundResource(R.drawable.megafon_logo_wine)
            } else if (operator.contains("MOTIV")) {
                binding.imageOperatorSim1.setBackgroundResource(R.drawable.tele2_svgrepo_com)
            } else if (operator.contains("MTS")) {
                binding.imageOperatorSim1.setBackgroundResource(R.drawable.mts__network_provider__logo_wine)
            } else if (operator.contains("Bee Line GSM")) {
                binding.imageOperatorSim1.setBackgroundResource(R.drawable.beeline_seeklogo)
            } else if (operator.contains("ROSTELECOM")) {
                binding.imageOperatorSim1.setBackgroundResource(R.drawable.rostelecom)
            } else {
                binding.imageOperatorSim1.setBackgroundResource(R.drawable.tele2_svgrepo_com)
            }

            // выводим градацию сигнала
            try {
                val signalInt: Int = operator.substringAfter("-").substringBefore("dBm").toInt()
                if (signalInt > SIGNAL_1) {
                    binding.imageSignalSim1.setBackgroundResource(R.drawable.signal_1)
                } else if (signalInt > SIGNAL_2) {
                    binding.imageSignalSim1.setBackgroundResource(R.drawable.signal_2)
                } else if (signalInt > SIGNAL_3) {
                    binding.imageSignalSim1.setBackgroundResource(R.drawable.signal_3)
                } else if (signalInt > SIGNAL_4) {
                    binding.imageSignalSim1.setBackgroundResource(R.drawable.signal_4)
                } else {
                    binding.imageSignalSim1.setBackgroundResource(R.drawable.signal_5)
                }
            } catch (_: Exception) {}
        } else if (operator.contains("SIM2: ")) {
            binding.progressBarOperators.visibility = View.GONE

            binding.Sim2Layout.visibility = View.VISIBLE

            // вывод данныех об сим карте
            binding.textOperatorSim2.text = operator.substringAfter("[").substringBefore("]")
            binding.textSignalSim2.text = operator.substringAfter("% ")

            // отображения картиночки
            if (operator.contains("MegaFon")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.megafon_logo_wine)
            } else if (operator.contains("MOTIV")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.tele2_svgrepo_com)
            } else if (operator.contains("MTS")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.mts__network_provider__logo_wine)
            } else if (operator.contains("Bee Line GSM")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.beeline_seeklogo)
            } else if (operator.contains("ROSTELECOM")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.rostelecom)
            } else {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.tele2_svgrepo_com)
            }

            // выводим градацию сигнала
            try {
                val signalInt: Int = operator.substringAfter("-").substringBefore("dBm").toInt()
                if (signalInt > SIGNAL_1) {
                    binding.imageSignalSim2.setBackgroundResource(R.drawable.signal_1)
                } else if (signalInt > SIGNAL_2) {
                    binding.imageSignalSim2.setBackgroundResource(R.drawable.signal_2)
                } else if (signalInt > SIGNAL_3) {
                    binding.imageSignalSim2.setBackgroundResource(R.drawable.signal_3)
                } else if (signalInt > SIGNAL_4) {
                    binding.imageSignalSim2.setBackgroundResource(R.drawable.signal_4)
                } else {
                    binding.imageSignalSim2.setBackgroundResource(R.drawable.signal_5)
                }
            } catch (_: Exception) {}
        }
    }

    override fun printError() {
        binding.progressBarOperators.visibility = View.GONE

        binding.Sim1Layout.visibility = View.GONE
        binding.Sim2Layout.visibility = View.GONE

        //  запрет показа операторов
        flagViewDiag = false
    }


    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

    override fun runDiag() {
        usbCommandsProtocol.readDiag(getString(R.string.commandRunDiagnostics),
            getString(R.string.endDiagBeginningM32D),
            requireContext(),
            this, this)
        flagStartDiag = true

        binding.buttonDiagStart.visibility = View.GONE

        // выводим прогресс бары
        binding.progressBarData.visibility = View.VISIBLE
        binding.progressBarOperators.visibility = View.VISIBLE

    }

    override fun printVerAndSernum(version: String, SerialNum: String) {
        // верийный номер и версия прошибки
        val serNum: String = SerialNum
        binding.serinerNumber.text = serNum

        val versionPr: String = version
        binding.textVersionFirmware.text = versionPr
    }


}