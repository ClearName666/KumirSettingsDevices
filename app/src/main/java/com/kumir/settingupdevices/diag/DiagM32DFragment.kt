package com.kumir.settingupdevices.diag

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayout
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentDiagM32DBinding
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbDiag

class DiagM32DFragment(val nameDeviace: String) : Fragment(), UsbDiag, DiagFragmentInterface {

    private lateinit var binding: FragmentDiagM32DBinding

    private val usbCommandsProtocol = UsbCommandsProtocol()

    private var flagStartDiag: Boolean = false
    private var flagViewDiag: Boolean = true


    // флаг прошивкаи 2 или 1 сим карт
    private var flagSim2: Boolean = true


    // для графика данных
    val entriesSim1 = ArrayList<Entry>()
    val entriesSim2 = ArrayList<Entry>()

    // текущий уровень сигнала у бащвой станции
    var curentPkgNumbersim1: Int = 0
    var curentPkgNumbersim2: Int = 0


    companion object {
        const val DROP_START_FOR_DATA: Int = 2
        const val DROP_END_FOR_DATA: Int = 2


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


        // установка даных в tab layout
        binding.tabPresets.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.scrollDiag.visibility = View.VISIBLE
                        binding.lineChartSim1.visibility = View.GONE
                        binding.lineChartSim2.visibility = View.GONE
                    }
                    1 -> {
                        binding.scrollDiag.visibility = View.GONE
                        binding.lineChartSim1.visibility = View.VISIBLE
                        binding.lineChartSim2.visibility = View.GONE
                    }
                    2 -> {
                        binding.scrollDiag.visibility = View.GONE
                        binding.lineChartSim1.visibility = View.GONE
                        binding.lineChartSim2.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No-op
            }
        })

        // установка в 0 графиков
        lineChartSim1("0", "0")

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
        } catch (_: Exception) {}


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
                binding.imageOperatorSim1.setBackgroundResource(R.drawable.error_svgrepo_com)
            }

            // выводим градацию сигнала
            try {
                val signalInt: Int = operator.substringAfter("-").substringBefore("dBm").toInt()

                // вывод информации в график
                curentPkgNumbersim1++

                lineChartSim1(curentPkgNumbersim1.toString(), (-signalInt).toString())

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
        } else if (operator.contains("SIM2: ") && flagSim2) {
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

                // вывод информации в график
                curentPkgNumbersim2++

                lineChartSim2(curentPkgNumbersim2.toString(), (-signalInt).toString())

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

        try {
            val varsionSim: Int = version.substringAfter("HW: ").substringBefore(" ").toInt()
            if (varsionSim < 4 || varsionSim == 16) flagSim2 = false
        } catch (e: Exception) {
            showAlertDialog(getString(R.string.errorCodeNone))
        }
    }



    private fun extractFirstIntFromString(input: String): Int? {
        val regex = Regex("-?\\d+")
        val match = regex.find(input)
        return match?.value?.toInt()
    }

    private fun lineChartSim1(x: String, y: String) {

        val xInt = extractFirstIntFromString(x)
        val yInt = extractFirstIntFromString(y)

        // Создание данных для графика
        entriesSim1.add(Entry(xInt?.toFloat()!!, yInt?.toFloat()!!))

        if (entriesSim1.size > 20) {
            entriesSim1.removeAt(0)
        }

        val dataSet = LineDataSet(entriesSim1, "Сигнал") // создаем набор данных с меткой
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.lineColor) // устанавливаем цвет линии
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.valueTextColor) // устанавливаем цвет текста значений

        // Настройка стиля точек
        dataSet.setDrawCircles(true)
        dataSet.setDrawCircleHole(false)
        dataSet.circleRadius = 2f
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.lineColor))

        val lineData = LineData(dataSet)
        binding.lineChartSim1.data = lineData

        // Настройка описания
        val description = Description()
        description.text = "График сигнала!"
        binding.lineChartSim1.description = description

        // Обновление графика
        binding.lineChartSim1.invalidate() // перерисовать график
    }

    private fun lineChartSim2(x: String, y: String) {

        val xInt = extractFirstIntFromString(x)
        val yInt = extractFirstIntFromString(y)

        // Создание данных для графика
        entriesSim2.add(Entry(xInt?.toFloat()!!, yInt?.toFloat()!!))

        if (entriesSim2.size > 20) {
            entriesSim2.removeAt(0)
        }

        val dataSet = LineDataSet(entriesSim2, "Сигнал") // создаем набор данных с меткой
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.lineColor) // устанавливаем цвет линии
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.valueTextColor) // устанавливаем цвет текста значений

        // Настройка стиля точек
        dataSet.setDrawCircles(true)
        dataSet.setDrawCircleHole(false)
        dataSet.circleRadius = 2f
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.lineColor))

        val lineData = LineData(dataSet)
        binding.lineChartSim2.data = lineData

        // Настройка описания
        val description = Description()
        description.text = "График сигнала!"
        binding.lineChartSim2.description = description

        // Обновление графика
        binding.lineChartSim2.invalidate() // перерисовать график
    }



}