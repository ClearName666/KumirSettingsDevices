package com.kumir.settingupdevices.diag

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayout
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.kumir.settingupdevices.databinding.FragmentDiagM32DBinding
import com.kumir.settingupdevices.diag.DiagFragment.Companion
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




    // флаг который говорит о том что устройство не исправно
    private var flagErrorCurrentDev: Boolean = false

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


        // Установим обработчик касаний
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val maxHeight = screenHeight / 2  // Максимум до половины экрана

        binding.touchListenerDiagForDifarmate.setOnTouchListener(object : View.OnTouchListener {
            var initialY = 0f
            var initialHeight = 0

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    // анимация для того что бы увеличивать и уменьшать то за что мы зацепляемся
                    /*MotionEvent.ACTION_DOWN -> {
                        // Сохраним начальные значения и запустим анимацию увеличения
                        initialY = event.rawY
                        initialHeight = binding.touchListenerDiagForDifarmate.height
                        animateLayout(binding.touchListenerDiagForDifarmate, 1.0f, 1.2f) // Увеличим до 120% от исходного размера
                        return true
                    }*/
                    MotionEvent.ACTION_DOWN -> {
                        // Сохраним начальное положение пальца и высоту лэйаута
                        initialY = event.rawY
                        initialHeight = binding.mainLayoutTabData.height
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Рассчитаем разницу перемещения
                        val deltaY = event.rawY - initialY

                        // Установим новую высоту лэйаута
                        val newHeight = initialHeight + deltaY.toInt()
                        if (newHeight > 200 && newHeight < maxHeight) { // Минимальная высота (можно задать свое значение)
                            val layoutParams = binding.mainLayoutTabData.layoutParams
                            layoutParams.height = newHeight
                            binding.mainLayoutTabData.layoutParams = layoutParams
                        }
                        return true
                    }
                }
                return false
            }
        })

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.diagTitleM32D))
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

        // изменения цвета текста у графиков
        binding.lineChartSim1.xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)
        binding.lineChartSim1.axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)
        binding.lineChartSim1.axisRight.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)
        binding.lineChartSim1.legend.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)


        binding.lineChartSim2.xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)
        binding.lineChartSim2.axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)
        binding.lineChartSim2.axisRight.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)
        binding.lineChartSim2.legend.textColor = ContextCompat.getColor(requireContext(), R.color.textColor)



        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // вывод предупреждения об диагностики
        showAlertDialog(getString(R.string.exitDiagRestart))
    }

    override fun onDestroyView() {
        endDiag()

        super.onDestroyView()
    }

    // запсук диагностики
    private fun onClickStartDiag() {
        if (!flagStartDiag) {
            val context: Context = requireContext()

            if (context is MainActivity) {
                context.showTimerDialogDiag(this, nameDeviace)
            }
        } else {
            endDiag()
            endViewDiag()
        }
    }

    // программное завершение диагностики
    private fun endDiag() {

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

        flagStartDiag = false
    }

    // визуальное завершение диагностики
    private fun endViewDiag() {

        // возврат на исходную видемость
        binding.progressBarData.visibility = View.GONE
        binding.progressBarOperators.visibility = View.GONE
        binding.textStateMalfunction.visibility = View.GONE
        binding.textStateNotMalfunction.visibility = View.GONE


        // меням все текста на исходные
        binding.serinerNumber.text = getString(R.string.serinerNumber)
        binding.textVersionFirmware.text = getString(R.string.versionProgram)
        binding.buttonDiagStart.text = getString(R.string.startDiagTitle)
        binding.textDiag.text = getString(R.string.dataLoading)

        // убераем все cсим карты
        binding.Sim1Layout.visibility = View.GONE
        binding.Sim2Layout.visibility = View.GONE

        // завершаем графики
        entriesSim1.clear()
        entriesSim2.clear()
        lineChartSim1("0", "0")
        lineChartSim2("0", "0")
    }



    override fun printAllInfo(info: String) {

        // исправления бага что график идет с 0
        entriesSim1.clear()
        entriesSim2.clear()

        binding.progressBarData.visibility = View.GONE
        flagErrorCurrentDev = false

        // выводим все логи о том что неисправно и что все окей
        val masDataValue = info.split("\n")
        val masErrorValue: MutableList<String> = mutableListOf()
        var infoInFlagsGreenRed = ""
        for (value in masDataValue) {
            if (value.length > 1) {  // минимум 2 символа для того что бы отсеивать мусор
                infoInFlagsGreenRed += if (value.contains("ERROR")) {
                    flagErrorCurrentDev = true
                    masErrorValue.add("\uD83D\uDFE5 $value \n")
                    ""
                } else {
                    try {
                        if (value.contains("Battery") && value.substringAfter("V ").substringBefore("%").toInt() < 90) {
                            flagErrorCurrentDev = true
                            masErrorValue.add("\uD83D\uDFE5 $value \n")
                            ""
                        } else {
                            "\uD83D\uDFE9 $value \n"
                        }
                    } catch (e: Exception) {
                        "\uD83D\uDFE9 $value \n"
                    }
                }
            }
        }

        // добавляем в начало ошибки
        for (value in masErrorValue) {
            infoInFlagsGreenRed = value + infoInFlagsGreenRed
        }

        // насло с ок и заканичается на CELLSCAN
        binding.textDiag.text = infoInFlagsGreenRed./*substringAfter(getString(R.string.okSand)).*/
        substringBefore(getString(R.string.endDiagBeginning))/*.drop(DROP_START_FOR_DATA)*/.dropLast(
            DiagFragment.DROP_END_FOR_DATA
        )

        // если есть ошибка то выводим диалог
        /*if (flagErrorCurrentDev) {
            binding.textStateMalfunction.visibility = View.VISIBLE
            binding.textStateNotMalfunction.visibility = View.GONE
        } else {
            binding.textStateMalfunction.visibility = View.GONE
            binding.textStateNotMalfunction.visibility = View.VISIBLE
        }*/
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
            } else if (operator.contains("MOTIV") || operator.contains("Tele2") ) {
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
            } else if (operator.contains("MOTIV") || operator.contains("Tele2") ) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.tele2_svgrepo_com)
            } else if (operator.contains("MTS")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.mts__network_provider__logo_wine)
            } else if (operator.contains("Bee Line GSM")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.beeline_seeklogo)
            } else if (operator.contains("ROSTELECOM")) {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.rostelecom)
            } else {
                binding.imageOperatorSim2.setBackgroundResource(R.drawable.error_svgrepo_com)
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
        endDiag()
        endViewDiag()
    }

    override fun noConnect() {
        endDiag()
        endViewDiag()

        showAlertDialog(getString(R.string.Disconnected))
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

        // binding.buttonDiagStart.visibility = View.GONE

        // выводим прогресс бары
        binding.progressBarData.visibility = View.VISIBLE
        binding.progressBarOperators.visibility = View.VISIBLE
        binding.buttonDiagStart.text = getString(R.string.endDiagTitle)

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

        val dataSet = LineDataSet(entriesSim1, "y - Сигнал, x - Итерация опроса") // создаем набор данных с меткой
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

        val dataSet = LineDataSet(entriesSim2, "y - Сигнал, x - Итерация опроса") // создаем набор данных с меткой
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