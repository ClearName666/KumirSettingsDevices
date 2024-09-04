package com.kumir.settingupdevices.diag

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.adapters.ItemBaseStationAdapter.ItemBaseStationAdapter
import com.kumir.settingupdevices.adapters.ItemPingrecvAdapter.ItemPingrecvAdapter
import com.kumir.settingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.kumir.settingupdevices.databinding.FragmentDiagPM81Binding
import com.kumir.settingupdevices.filesManager.GenerationFiles
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.model.recyclerModel.ItemBaseStation
import com.kumir.settingupdevices.model.recyclerModel.ItemPingrecv
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class DiagPM81Fragment(val nameDeviace: String) : Fragment(), UsbDiagPm, DiagFragmentInterface, UsbFragment{


    override var keyNet: String = ""
    override var mode: String = ""
    override var range: Int = 0

    private lateinit var binding: FragmentDiagPM81Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    private var flagStartDiag: Boolean = false

    // лист для диагностики
    val listBaseStation: MutableList<ItemBaseStation> = mutableListOf()
    val listBasePingrecv: MutableList<ItemPingrecv> = mutableListOf()

    // текущий уровень сигнала у бащвой станции
    var curentSignalBaseStation: String = "Не определено!"

    var baseStation: Boolean = false
    // для графика данных
    val entries = ArrayList<Entry>()


    // флаг для работы анимации
    private var flagWorkAnimLoadingBaseStation: Boolean = true
    // поток для работы анимации
    private var animJob: Job? = null

    // геопозиция

    private var dataPosSignal: String = ""

    companion object {

        // задержка для анимации загрузки операторов
        const val TIMEOUT_ANIM_LOADING_OPERATORS: Long = 700

        const val DEFFAULT_KEY_NET: String = "7586-100000-000003"
        const val LOCATION_PERMISSION_REQUEST_CODE: Int = 200
        const val DIR_POS_DATA_DEFAULTE: String = "/posData"
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagPM81Binding.inflate(inflater)

        // установка даных в tab layout
        binding.tabPresets.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.recyclerItemnonPackg.visibility = View.VISIBLE
                        binding.lineChart.visibility = View.GONE
                    }
                    1 -> {
                        binding.recyclerItemnonPackg.visibility = View.GONE
                        binding.lineChart.visibility = View.VISIBLE
                        binding.textDialogExitDiag.visibility = View.GONE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No-op
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
        binding.buttonOldSet.setOnClickListener {
            dataRecovery()
        }

        // скачивание позиций с данными
        binding.imageFilePositionData.setOnClickListener {
            val generationFiles = GenerationFiles()

            if (!generationFiles.createFile(dataPosSignal, "dataPos.txt", requireContext(),
                    Environment.DIRECTORY_DOWNLOADS + DIR_POS_DATA_DEFAULTE))
                showAlertDialog(getString(R.string.noSaveFiles)) else {
                showAlertDialog(getString(R.string.yesSaveFiles))
            }
        }

        // стандартный ключ доступа в сеть
        binding.inputKeyNet.setText(DEFFAULT_KEY_NET)

        // проверяем валиность введенных данных
        surveillanceInputText()

        createAdapters()

        lineChart("0", "0")

        return binding.root
    }

    fun extractFirstIntFromString(input: String): Int? {
        val regex = Regex("-?\\d+")
        val match = regex.find(input)
        return match?.value?.toInt()
    }



    private fun getLastKnownLocation(callback: (Double, Double) -> Unit) {
        val context: Context = requireContext()
        if (context is MainActivity && context.checkLocationPermissions())  {


            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) { return }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // Получение местоположения успешно
                    location?.let {
                        val latitude = it.latitude
                        val longitude = it.longitude
                        callback(latitude, longitude)
                    } ?: run {
                        // Местоположение не найдено
                        callback(Double.NaN, Double.NaN)
                    }
                }
                .addOnFailureListener { e ->
                    // Обработка ошибки
                    e.printStackTrace()
                    callback(Double.NaN, Double.NaN)
                }
        }
    }

    private fun lineChart(x: String, y: String, main: Boolean = false) {

        val xInt = extractFirstIntFromString(x)
        val yInt = extractFirstIntFromString(y)

        // Создание данных для графика
        entries.add(Entry(xInt?.toFloat()!!, yInt?.toFloat()!!))

        if (entries.size > 20) {
            entries.removeAt(0)
        }

        val dataSet = LineDataSet(entries, "Сигнал") // создаем набор данных с меткой
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.lineColor) // устанавливаем цвет линии
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.valueTextColor) // устанавливаем цвет текста значений

        // Настройка стиля точек
        dataSet.setDrawCircles(true)
        dataSet.setDrawCircleHole(false)
        dataSet.circleRadius = 2f
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.lineColor))

        // Настройка выделенной точки, если main равен true
        if (main) {
            val lastIndex = entries.size - 1
            if (lastIndex >= 0) {
                dataSet.circleColors = MutableList(entries.size) {
                    if (it == lastIndex) ContextCompat.getColor(requireContext(), R.color.lineColorMain)
                    else ContextCompat.getColor(requireContext(), R.color.lineColor)
                }
                dataSet.circleRadius = 2f // Увеличиваем радиус для выделенной точки
            }
        }

        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData

        // Настройка описания
        val description = Description()
        description.text = "График сигнала!"
        binding.lineChart.description = description

        // Обновление графика
        binding.lineChart.invalidate() // перерисовать график
    }


    private fun createAdapters() {

        // адаптер для выбора диопазона
        val itemSelectRange = listOf(
            getString(R.string.rangeMod1),
            getString(R.string.rangeMod2),
            getString(R.string.rangeMod3)
        )

        val adapterSelectRange = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectRange)

        adapterSelectRange.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerRange.adapter = adapterSelectRange
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

        // очищение графика
        entries.clear()

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
        if (!flagStartDiag) {
            if (context is MainActivity) {
                context.showTimerDialog(this, nameDeviace, true)
            }
        } else {
            if (context is MainActivity) {
                context.showAlertDialog(getString(R.string.notGoThread))
            }
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

        binding.textDialogExitDiag.visibility = View.GONE
    }


    // вывод базовых станций
    override fun printAll(allBaseStations: String) {

        binding.textDialogExitDiag.visibility = View.GONE

        var flagBaseStations: Boolean = false

        val list: List<String> = allBaseStations.split("\n")


        for (d in list) {
            // если данные полезные то добавлем данные для отображения в адаптере
            if (!d.contains("msg: Scanning for BSS..") && !d.contains(getString(R.string.okSand))) {
                if (d.contains("BSS") && !d.contains("locked") && !d.contains("acquire")) {
                    listBaseStation.add(
                        ItemBaseStation(
                            d.substringAfter("BSS ").substringBefore(" "),
                            d.substringAfter("SNR="),
                            main = false
                        )
                    )
                    flagBaseStations = true
                } else if (d.contains("locked")){
                    listBaseStation.add(
                        ItemBaseStation(
                            d.substringAfter("BSS ").substringBefore(" "),
                            curentSignalBaseStation,
                            main = true
                        )
                    )

                    // позиция и данные
                    getLastKnownLocation { latitude, longitude ->
                        dataPosSignal += d + " latitude=$latitude,longitude=$longitude\n"
                    }

                    baseStation = true
                    flagBaseStations = true
                } else if (d.contains("pingrecv")) {
                    listBasePingrecv.add(
                        ItemPingrecv(
                            d.substringAfter(": ").substringBefore(","),
                            d.substringAfter(", ").substringBefore(", "),
                            d.substringAfter("ms, ")
                        )
                    )

                    // позиция и данные
                    getLastKnownLocation { latitude, longitude ->
                        dataPosSignal += d + " latitude=$latitude,longitude=$longitude\n"
                    }

                    curentSignalBaseStation = d.substringAfter("ms, ")
                    if (!baseStation) {
                        lineChart(d.substringAfter(": ").substringBefore(",").trim(), curentSignalBaseStation)
                    } else {
                        lineChart(d.substringAfter(": ").substringBefore(",").trim(), curentSignalBaseStation, true)
                        baseStation = false
                    }
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

        val itemBasePingrecvAdapter = ItemPingrecvAdapter(requireContext(), listBasePingrecv.reversed())
        binding.recyclerItemnonPackg.adapter = itemBasePingrecvAdapter
        binding.recyclerItemnonPackg.layoutManager = LinearLayoutManager(requireContext())
    }


    override fun stopDiag() {
        binding.progressBarBaseStations.visibility = View.GONE
        binding.progressBarPackg.visibility = View.GONE

        flagWorkAnimLoadingBaseStation = false

        // закратие анимации загрузки операторов
        animJob?.cancel()

        dataPosSignal = ""

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
        // выброный диапозон
        val selectRange: String = (binding.spinnerRange.selectedItemPosition+1).toString()

        usbCommandsProtocol.readDiagPm(requireContext(),
            this, this, binding.inputKeyNet.text.toString(),
            selectRange)
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
            getString(R.string.commandSetNetKey) to keyNet,
            getString(R.string.commandSetRange) to range.toString()
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