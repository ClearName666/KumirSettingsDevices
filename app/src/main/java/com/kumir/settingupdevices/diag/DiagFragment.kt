package com.kumir.settingupdevices.diag

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.kumir.settingupdevices.databinding.FragmentDiagBinding
import com.kumir.settingupdevices.model.recyclerModel.ItemOperator
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbDiag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DiagFragment(val nameDeviace: String) : Fragment(), UsbDiag, DiagFragmentInterface {

    private lateinit var binding: FragmentDiagBinding

    private val usbCommandsProtocol = UsbCommandsProtocol()

    private var flagStartDiag: Boolean = false
    private var flagViewDiag: Boolean = true


    // данные операторов
    private var listOperators: MutableList<ItemOperator> = mutableListOf()

    // флаг для работы анимации
    private var flagWorkAnimLoadingOperators: Boolean = true
    private var flagTimer: Boolean = false
    // поток для работы анимации
    private var animJob: Job? = null

    // флаг который говорит о том что устройство не исправно
    private var flagErrorCurrentDev: Boolean = false


    companion object {
        // const val DROP_START_FOR_DATA: Int = 2
        const val DROP_END_FOR_DATA: Int = 3

        // задержка для анимации загрузки операторов
        const val TIMEOUT_ANIM_LOADING_OPERATORS: Long = 1000
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagBinding.inflate(inflater)

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
                        initialHeight = binding.mainScrollLayoutDiag.height
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Рассчитаем разницу перемещения
                        val deltaY = event.rawY - initialY

                        // Установим новую высоту лэйаута
                        val newHeight = initialHeight + deltaY.toInt()
                        if (newHeight > 200 && newHeight < maxHeight) { // Минимальная высота (можно задать свое значение)
                            val layoutParams = binding.mainScrollLayoutDiag.layoutParams
                            layoutParams.height = newHeight
                            binding.mainScrollLayoutDiag.layoutParams = layoutParams
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
            context.printDeviceTypeName(getString(R.string.diagTitle))
        }

        // назначение кликов
        binding.buttonDiagStart.setOnClickListener {
            onClickStartDiag()
        }

        binding.switchAdvancedOperators.setOnCheckedChangeListener { _, isChecked ->
            if (flagViewDiag) {
                if (isChecked) { // рсширеный ражим включен
                    val itemOperatorAdapter = ItemOperatorAdapter(requireContext(), listOperators)
                    binding.recyclerItemOperators.adapter = itemOperatorAdapter
                    binding.recyclerItemOperators.layoutManager = LinearLayoutManager(requireContext())
                } else { // краткий режим
                    val itemBriefly: List<ItemOperator> = listOperators.map { ItemOperator(
                        it.operator,
                        "",
                        "",
                        it.rxlev,
                        "",
                        it.arfnc,
                        "",
                        ""
                    ) }

                    val itemOperatorAdapter = ItemOperatorAdapter(requireContext(), itemBriefly)
                    binding.recyclerItemOperators.adapter = itemOperatorAdapter
                    binding.recyclerItemOperators.layoutManager = LinearLayoutManager(requireContext())
                }
            }
        }

        // свич для вывода лога
        binding.switchActivityLogData.setOnCheckedChangeListener { _, isChecked ->
            binding.mainScrollLayoutDiag.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        return binding.root
    }

    // Анимация изменения размера
    /*private fun animateLayout(view: View, fromScale: Float, toScale: Float) {
        val scaleAnimation = ScaleAnimation(
            fromScale, toScale,  // X от-до
            fromScale, toScale,  // Y от-до
            Animation.RELATIVE_TO_SELF, 0.5f,  // Точка поворота по X - центр
            Animation.RELATIVE_TO_SELF, 0.5f   // Точка поворота по Y - центр
        )
        scaleAnimation.duration = 200  // Продолжительность анимации 200ms
        scaleAnimation.fillAfter = true  // Сохранение конечного состояния после завершения анимации
        view.startAnimation(scaleAnimation)
    }*/

    /*override fun onStart() {
        super.onStart()

        // вывод предупреждения об диагностики
        showAlertDialog(getString(R.string.exitDiagRestart))
    }*/

    override fun onDestroyView() {
        endDiag()
        super.onDestroyView()
    }

    // визуальное завершение диагностики
    private fun endViewDiag() {

        // возврат на исходную видемость 
        binding.mainScrollLayoutDiag.visibility = View.GONE
        binding.progressBarData.visibility = View.GONE
        binding.progressBarOperators.visibility = View.GONE
        binding.textNonFindOperators.visibility = View.GONE
        binding.textStateMalfunction.visibility = View.GONE
        binding.textStateNotMalfunction.visibility = View.GONE
        binding.switchActivityLogData.visibility = View.GONE

        // убераем все что навключал пользователь
        binding.switchActivityLogData.isChecked = false

        binding.textDialogExitDiag.visibility = View.VISIBLE
        binding.textDialogExitDiag.visibility = View.VISIBLE

        // меням все текста на исходные
        binding.textTimer.text = getString(R.string.timeZero)
        binding.serinerNumber.text = getString(R.string.serinerNumber)
        binding.textVersionFirmware.text = getString(R.string.versionProgram)
        binding.buttonDiagStart.text = getString(R.string.startDiagTitle)

        // убераем все что нашлось у прошлого модема
        val itemOperatorAdapter = ItemOperatorAdapter(requireContext(), listOf())
        binding.recyclerItemOperators.adapter = itemOperatorAdapter
        binding.recyclerItemOperators.layoutManager = LinearLayoutManager(requireContext())
    }

    // программное завершение диагностики
    private fun endDiag() {
        // закратие анимации загрузки операторов
        animJob?.cancel()

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

    // Анимация загрузки операторов
    private fun startLoadingAnimation() {
        animJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                for (i in 0..3) {
                    binding.textTitleOperators.text = getString(R.string.operatorsTitle) + ".".repeat(i)
                    timerAnim()
                    delay(TIMEOUT_ANIM_LOADING_OPERATORS)
                }
            }
        }
    }

    fun timerAnim() {
        // таймер
        if (!flagTimer) {
            val curentTimeSec: Int = try {
                binding.textTimer.text.toString().substringAfter(":").toInt()
            } catch (e: Exception) { -1 }
            val curentTimeMin = try {
                binding.textTimer.text.toString().substringBefore(":").toInt()
            } catch (e: Exception) { -1 }

            if (curentTimeMin != -1 && curentTimeSec != -1) {
                if (curentTimeSec != 59)
                    binding.textTimer.text = String.format("%02d:%02d", curentTimeMin, curentTimeSec+1)
                else
                    binding.textTimer.text = String.format("%02d:%02d", curentTimeMin+1, 0)

                if (curentTimeMin == 60) {
                    binding.textTimer.text = getString(R.string.timeZero)
                }
            }
        } else {
            binding.textTimer.text = getString(R.string.timeZero)
            flagTimer = false
        }
    }
    fun timerZero() {
        binding.textTimer.text = getString(R.string.timeZero)
    }


    override fun printAllInfo(info: String) {
        binding.switchActivityLogData.visibility = View.VISIBLE
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
                    "\uD83D\uDFE9 $value \n"
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
            DROP_END_FOR_DATA
        )

        // если есть ошибка то выводим диалог
        if (flagErrorCurrentDev) {
            binding.mainScrollLayoutDiag.visibility = View.VISIBLE
            binding.textStateMalfunction.visibility = View.VISIBLE
            binding.textStateNotMalfunction.visibility = View.GONE
            binding.switchActivityLogData.isChecked = true
        } else {
            binding.mainScrollLayoutDiag.visibility = View.GONE
            binding.textStateMalfunction.visibility = View.GONE
            binding.textStateNotMalfunction.visibility = View.VISIBLE
            binding.switchActivityLogData.isChecked = false
        }
    }

    override fun printAllOperator(allOperators: String) {

        flagTimer = true

        //  разрешение показа операторов
        flagViewDiag = true

        binding.progressBarOperators.visibility = View.GONE
        binding.textDialogExitDiag.visibility = View.GONE

        val operatorsString: List<String> = allOperators.split("\n")
        val itemsOperators: MutableList<ItemOperator> = mutableListOf()

        // если операторы не найдены то вывоит текст о том что операторы не найдены
        if (operatorsString.isEmpty()) {
            binding.textNonFindOperators.visibility = View.VISIBLE
        } else {
            binding.textNonFindOperators.visibility = View.GONE

            // разделение строки по отдельным данным
            for (operatorString in operatorsString) {
                val datas: List<String> = operatorString.split(",")

                try {
                    val frequency = arfcnToFrequency(datas[5].substringAfter(":").toInt())

                    // Для краткого по умолчанию
                    var itemOperator: ItemOperator = ItemOperator(
                        "  " + datas[0].substringAfter("\"").substringBefore("\""),
                        "",
                        "",
                        "  " + datas[3].substringAfter(":"),
                        "",
                        "  " + "${frequency?.first}-${frequency?.second}",
                        "",
                        ""
                    )

                    // добавляем все данные опараторов в глобальный лист
                    val itemOperatorGlobal = ItemOperator(
                        "  " + datas[0].substringAfter("\"").substringBefore("\""),
                        "  " + datas[1].substringAfter(":"),
                        "  " + datas[2].substringAfter(":"),
                        "  " + datas[3].substringAfter(":"),
                        "  " + datas[4].substringAfter(":"),
                        "  ${frequency?.first}-${frequency?.second}",
                        "  " + datas[6].substringAfter(":"),
                        "  " + datas[7].substringAfter(":")
                    )
                    listOperators.add(itemOperatorGlobal)

                    // проверка расширеный или не расширеный
                    if (binding.switchAdvancedOperators.isChecked) {
                        itemsOperators.add(itemOperatorGlobal)
                    } else {
                        itemsOperators.add(itemOperator)
                    }


                } catch (e: Exception) {

                }
            }

            val itemOperatorAdapter = ItemOperatorAdapter(requireContext(), itemsOperators)
            binding.recyclerItemOperators.adapter = itemOperatorAdapter
            binding.recyclerItemOperators.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun printError() {
        binding.progressBarOperators.visibility = View.GONE
        binding.textNonFindOperators.visibility = View.VISIBLE
        flagWorkAnimLoadingOperators = false

        // закратие анимации загрузки операторов
        animJob?.cancel()

        // убераем найденые операторы
        val itemOperatorAdapter = ItemOperatorAdapter(requireContext(), listOf())
        binding.recyclerItemOperators.adapter = itemOperatorAdapter
        binding.recyclerItemOperators.layoutManager = LinearLayoutManager(requireContext())

        //  запрет показа операторов
        flagViewDiag = false
        timerZero()
        flagTimer = false

    }

    override fun noConnect() {
        endDiag()
        endViewDiag()
        showAlertDialog(getString(R.string.Disconnected))
    }

    // перевод кaнала в частоту
    fun arfcnToFrequency(arfcn: Int): Pair<Double, Double>? {
        return when {
            // GSM 900 (Primary GSM)
            arfcn in 1..124 -> {
                val downlink = 935.0 + 0.2 * (arfcn - 1)
                val uplink = downlink - 45
                Pair(downlink, uplink)
            }

            // GSM 1900 (PCS 1900)
            arfcn in 512..810 -> {
                val downlink = 1930.0 + 0.2 * (arfcn - 512)
                val uplink = downlink - 80
                Pair(downlink, uplink)
            }

            // GSM 1800 (DCS 1800)
            arfcn in 512..885 -> {
                val downlink = 1805.0 + 0.2 * (arfcn - 512)
                val uplink = downlink - 95
                Pair(downlink, uplink)
            }

            else -> null // ARFCN вне диапазонов GSM 900, 1800, 1900
        }
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

    override fun runDiag() {
        usbCommandsProtocol.readDiag(getString(R.string.commandRunDiagnostics),
            getString(R.string.endDiagBeginning),
            requireContext(),
            this, this)
        flagStartDiag = true

        // binding.buttonDiagStart.visibility = View.GONE
        binding.buttonDiagStart.text = getString(R.string.endDiagTitle)

        // выводим прогресс бары
        binding.progressBarData.visibility = View.VISIBLE
        binding.progressBarOperators.visibility = View.VISIBLE

        // Анимация загрузки операторов
        startLoadingAnimation()
    }

    override fun printVerAndSernum(version: String, SerialNum: String) {
        // верийный номер и версия прошибки
        val serNum: String = SerialNum
        binding.serinerNumber.text = serNum

        val versionPr: String = version
        binding.textVersionFirmware.text = versionPr
    }

}