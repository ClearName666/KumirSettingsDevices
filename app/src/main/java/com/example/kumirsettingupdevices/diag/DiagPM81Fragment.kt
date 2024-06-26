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
import com.example.kumirsettingupdevices.adapters.ItemBaseStationAdapter.ItemBaseStationAdapter
import com.example.kumirsettingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.example.kumirsettingupdevices.databinding.FragmentDiagPM81Binding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemBaseStation
import com.example.kumirsettingupdevices.model.recyclerModel.ItemOperator
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class DiagPM81Fragment(val nameDeviace: String) : Fragment(), UsbDiagPm, DiagFragmentInterface{

    private lateinit var binding: FragmentDiagPM81Binding

    private val usbCommandsProtocol = UsbCommandsProtocol()

    private var flagStartDiag: Boolean = false

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


        return binding.root
    }

    override fun onDestroyView() {

        // закратие анимации загрузки операторов
        animJob?.cancel()

        // отключение систем проверки сигнала и выход из потока
        try {
            // отключения потока прочитки сигнала если он включен
            if (usbCommandsProtocol.flagWorkDiagPm) {
                //usbCommandsProtocol.threadDiag.interrupt()
                usbCommandsProtocol.flagWorkDiagPm = false
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
    override fun printAllBaseStations(allBaseStations: String) {
        val list: List<String> = allBaseStations.split("\n")
        val listBaseStation: MutableList<ItemBaseStation> = mutableListOf()

        for (d in list) {
            // если данные полезные то добавлем данные длля отображения в адаптере
            if (!d.contains("msg: Scanning for BSS..") && !d.contains(getString(R.string.okSand))) {
                listBaseStation.add(
                    ItemBaseStation(
                        d.substringAfter("BSS ").substringBefore(" "),
                        d.substringAfter("SNR=")
                    )
                )
            }
        }

        val itemBaseStationAdapter = ItemBaseStationAdapter(requireContext(), listBaseStation)
        binding.recyclerItemnonFindBaseStations.adapter = itemBaseStationAdapter
        binding.recyclerItemnonFindBaseStations.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun printAllBasePingsend(pingsend: String) {

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
        binding.recyclerItemnonFindBaseStations.layoutManager = LinearLayoutManager(requireContext())
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

    }


    override fun runDiag() {
        usbCommandsProtocol.readDiagPm("locked", "msg", requireContext(),
            this, this)
        flagStartDiag = true

        binding.buttonDiagStart.visibility = View.GONE

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


}