package com.example.kumirsettingupdevices.diag

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemBaseStationAdapter.ItemBaseStationAdapter
import com.example.kumirsettingupdevices.adapters.ItemPingrecvAdapter.ItemPingrecvAdapter
import com.example.kumirsettingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.example.kumirsettingupdevices.databinding.FragmentDiagPM81Binding
import com.example.kumirsettingupdevices.formaters.ValidDataSettingsDevice
import com.example.kumirsettingupdevices.model.recyclerModel.ItemBaseStation
import com.example.kumirsettingupdevices.model.recyclerModel.ItemPingrecv
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class DiagPM81Fragment(val nameDeviace: String) : Fragment(), UsbDiagPm, DiagFragmentInterface, UsbFragment{


    override var keyNet: String = ""
    override var mode: String = ""

    private lateinit var binding: FragmentDiagPM81Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    private var flagStartDiag: Boolean = false

    // лист для диагностики
    val listBaseStation: MutableList<ItemBaseStation> = mutableListOf()
    val listBasePingrecv: MutableList<ItemPingrecv> = mutableListOf()


    // флаг для работы анимации
    private var flagWorkAnimLoadingBaseStation: Boolean = true
    // поток для работы анимации
    private var animJob: Job? = null

    companion object {

        // задержка для анимации загрузки операторов
        const val TIMEOUT_ANIM_LOADING_OPERATORS: Long = 700
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagPM81Binding.inflate(inflater)

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.diagTitle))
        }

        // назначение кликов
        binding.buttonDiagStart.setOnClickListener {
            onClickStartDiag()
        }
        binding.buttonOldSet.setOnClickListener {
            dataRecovery()
        }

        // стандартный ключ доступа в сеть
        binding.inputKeyNet.setText("7586-100000-000003")

        // проверяем валиность введенных данных
        surveillanceInputText()


        return binding.root
    }

    override fun onDestroyView() {

        // закратие анимации загрузки операторов
        animJob?.cancel()

        clearDiag()
        super.onDestroyView()
    }

    // проверка на правильность введенных данных в полях input
    private fun surveillanceInputText() {
        // Настройка TextWatcher
        binding.inputKeyNet.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Проверяем введенный текст и меняем цвет поля
                val validDataSettingsDevice = ValidDataSettingsDevice()
                if (!validDataSettingsDevice.serverValid(s.toString()) || s.toString().length <= 60) { // не правильно
                    binding.textInputLayoutKeyNet.boxStrokeColor = resources.getColor(R.color.dangerous)
                } else {
                    binding.textInputLayoutKeyNet.boxStrokeColor = resources.getColor(R.color.fon2Element)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun clearDiag() {
        // отключение систем проверки и выход из потока
        try {
            // отключения потока  если он включен
            if (usbCommandsProtocol.flagWorkDiagPm) {
                //usbCommandsProtocol.threadDiag.interrupt()
                usbCommandsProtocol.flagWorkDiagPm = false
            }
        } catch (_: Exception) {}


        // очещение буфера данных
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.curentData = ""
        }

        // очищение данных
        listBaseStation.clear()
        listBasePingrecv.clear()
    }

    // востановление данных
    private fun dataRecovery() {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.showTimerDialog(this, nameDeviace, true)
        }
    }

    // запсук диагностики
    private fun onClickStartDiag() {
        if (!flagStartDiag) {
            val validDataSettingsDevice = ValidDataSettingsDevice()
            if (validDataSettingsDevice.serverValid(binding.inputKeyNet.text.toString()) || binding.inputKeyNet.text.toString().length <= 60) {
                val context: Context = requireContext()

                if (context is MainActivity) {
                    context.showTimerDialogDiag(this, nameDeviace)
                }
            } else {
                showAlertDialog(getString(R.string.errorRussionChar))
            }
        } else {
            clearDiag()
            stopDiag()
        }
    }

    // Анимация загрузки операторов
    private fun startLoadingAnimation() {
        animJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                for (i in 0..3) {
                    binding.textTitlenonFindBaseStations.text = getString(R.string.BaseStationsTitle) + ".".repeat(i)
                    delay(TIMEOUT_ANIM_LOADING_OPERATORS)
                }
            }
        }
    }


    // вывод базовых станций
    override fun printAll(allBaseStations: String) {
        var flagBaseStations: Boolean = false

        val list: List<String> = allBaseStations.split("\n")


        for (d in list) {
            // если данные полезные то добавлем данные для отображения в адаптере
            if (!d.contains("msg: Scanning for BSS..") && !d.contains(getString(R.string.okSand))) {
                if (d.contains("BSS") && !d.contains("locked") && !d.contains("acquire")) {
                    listBaseStation.add(
                        ItemBaseStation(
                            d.substringAfter("BSS ").substringBefore(" "),
                            d.substringAfter("SNR=")
                        )
                    )
                    flagBaseStations = true
                } else if (d.contains("locked")){
                    listBaseStation.add(
                        ItemBaseStation(
                            d.substringAfter("BSS ").substringBefore(" "),
                            ""
                        )
                    )
                    flagBaseStations = true
                } else if (d.contains("pingrecv")) {
                    listBasePingrecv.add(
                        ItemPingrecv(
                            d.substringAfter(": ").substringBefore(","),
                            d.substringAfter(", ").substringBefore(", "),
                            d.substringAfter("ms, ")
                        )
                    )
                }
            }
        }

        // проверка на пстые данные базовых станций то выключаем прогресс бар
        if (listBaseStation.isNotEmpty())
            binding.progressBarBaseStations.visibility = View.GONE

        // сначала пустой если данные какие то поступили то не пустой
        var itemBaseStationAdapter = ItemBaseStationAdapter(requireContext(), listOf())
        if (flagBaseStations)
            itemBaseStationAdapter = ItemBaseStationAdapter(requireContext(), listBaseStation.reversed().distinct())
        else
            listBaseStation.clear()

        // для вывода базовых станций
        binding.recyclerItemnonFindBaseStations.adapter = itemBaseStationAdapter
        binding.recyclerItemnonFindBaseStations.layoutManager = LinearLayoutManager(requireContext())

        // для вывода пакетов
        if (listBasePingrecv.isNotEmpty())
            binding.progressBarPackg.visibility = View.GONE

        val itemBasePingrecvAdapter = ItemPingrecvAdapter(requireContext(), listBasePingrecv)
        binding.recyclerItemnonPackg.adapter = itemBasePingrecvAdapter
        binding.recyclerItemnonPackg.layoutManager = LinearLayoutManager(requireContext())
    }


    override fun stopDiag() {
        binding.progressBarBaseStations.visibility = View.GONE
        binding.progressBarPackg.visibility = View.GONE

        flagWorkAnimLoadingBaseStation = false

        // закратие анимации загрузки операторов
        animJob?.cancel()

        // убераем найденые станций
        val itemOperatorAdapter = ItemOperatorAdapter(requireContext(), listOf())
        binding.recyclerItemnonFindBaseStations.adapter = itemOperatorAdapter

        // убраем найденые пакеты
        // убераем найденые станций
        val itemPackgAdapter = ItemPingrecvAdapter(requireContext(), listOf())
        binding.recyclerItemnonPackg.adapter = itemPackgAdapter

        binding.buttonDiagStart.text = getString(R.string.startDiagTitle)
        flagStartDiag = false


        binding.buttonOldSet.visibility = View.VISIBLE
    }


    override fun printError() {
        binding.progressBarBaseStations.visibility = View.GONE
        binding.progressBarPackg.visibility = View.GONE

        binding.textNonFindBaseStations.visibility = View.VISIBLE
        flagWorkAnimLoadingBaseStation = false

        // закратие анимации загрузки операторов
        animJob?.cancel()

        // убераем найденые станций
        val itemOperatorAdapter = ItemOperatorAdapter(requireContext(), listOf())
        binding.recyclerItemnonFindBaseStations.adapter = itemOperatorAdapter
        binding.recyclerItemnonFindBaseStations.layoutManager = LinearLayoutManager(requireContext())

        binding.buttonDiagStart.text = getString(R.string.startDiagTitle)
        flagStartDiag = false
    }


    override fun runDiag() {
        usbCommandsProtocol.readDiagPm(requireContext(),
            this, this, binding.inputKeyNet.text.toString())
        flagStartDiag = true

        // у кнопки назначаем текст для завершения диагностки
        binding.buttonDiagStart.text = getString(R.string.diagStopButton)

        // выводим прогресс бары
        binding.progressBarBaseStations.visibility = View.VISIBLE
        binding.progressBarPackg.visibility = View.VISIBLE

        // Анимация загрузки станций
        startLoadingAnimation()
    }

    override fun printVerAndSernum(version: String, SerialNum: String) {
        // верийный номер и версия прошибки
        val serNum: String = SerialNum
        binding.serinerNumber.text = serNum

        val versionPr: String = version
        binding.textVersionFirmware.text = versionPr
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

    // востановление настроек
    override fun writeSettingStart() {
        val usbCommandsProtocol = UsbCommandsProtocol()

        val dataMap: MutableMap<String, String> = mutableMapOf(
            getString(R.string.commandSetMode) to mode,
            getString(R.string.commandSetNetKey) to keyNet
        )

        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this)
    }

    override fun lockFromDisconnected(connect: Boolean) {
        if (connect) {
            binding.buttonDiagStart.setOnClickListener {
                onClickStartDiag()
            }
            binding.buttonOldSet.setOnClickListener {
                dataRecovery()
            }
        } else {
            binding.buttonDiagStart.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
            binding.buttonOldSet.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        }
    }

    // излишние методы
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}
    override fun printSettingDevice(settingMap: Map<String, String>) {}
    override fun readSettingStart() {}


}