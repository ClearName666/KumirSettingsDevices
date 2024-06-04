package com.example.kumirsettingupdevices

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.example.kumirsettingupdevices.databinding.FragmentDiagBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemOperator
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbDiag

class DiagFragment(val serialNumber: String, private val programVersion: String) : Fragment(), UsbDiag {

    private lateinit var binding: FragmentDiagBinding

    private val usbCommandsProtocol = UsbCommandsProtocol()

    private var flagStartDiag: Boolean = false

    companion object {
        const val DROP_START_FOR_DATA: Int = 2
        const val DROP_END_FOR_DATA: Int = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagBinding.inflate(inflater)

        // верийный номер и версия прошибки
        val serNum: String = serialNumber
        binding.serinerNumber.text = serNum

        val version: String = programVersion
        binding.textVersionFirmware.text = version

        // назначение кликов
        binding.buttonDiagStart.setOnClickListener {
            onClickStartDiag()
        }
        binding.imageBack.setOnClickListener {
            onClickBack()
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
            usbCommandsProtocol.readDiag(getString(R.string.commandRunDiagnostics),
                getString(R.string.endDiagBeginning),
                requireContext(),
                this)
            flagStartDiag = true

            binding.buttonDiagStart.visibility = View.GONE

            // выводим прогресс бары
            binding.progressBarData.visibility = View.VISIBLE
            binding.progressBarOperators.visibility = View.VISIBLE
        }

    }

    // возврат пока что к m32
    private fun onClickBack() {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.onClickM32(binding.imageBack)
        }
    }

    override fun printAllInfo(info: String) {
        binding.progressBarData.visibility = View.GONE

        // насло с ок и заканичается на CELLSCAN
        binding.textDiag.text = info.substringAfter(getString(R.string.okSand)).
                substringBefore(getString(R.string.endDiagBeginning)).drop(DROP_START_FOR_DATA).dropLast(DROP_END_FOR_DATA)
    }

    override fun printAllOperator(allOperators: String) {

        binding.progressBarOperators.visibility = View.GONE

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

                    val itemOperator = ItemOperator(
                        datas[0].substringAfter("\"").substringBefore("\""),
                        datas[1].substringAfter(":"),
                        datas[2].substringAfter(":"),
                        datas[3].substringAfter(":"),
                        datas[4].substringAfter(":"),
                        "${frequency?.first}-${frequency?.second}",
                        datas[6].substringAfter(":"),
                        datas[7].substringAfter(":")
                    )
                    itemsOperators.add(itemOperator)

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
    }

    // перевод конала в частоту
    fun arfcnToFrequency(arfcn: Int): Pair<Double, Double>? {
        return when {
            // GSM 900 (Primary GSM)
            arfcn in 1..124 -> {
                val downlink = 935.0 + 0.2 * (arfcn - 1)
                val uplink = downlink - 45
                Pair(downlink, uplink)
            }
            // GSM 1800 (DCS 1800)
            arfcn in 512..885 -> {
                val downlink = 1805.0 + 0.2 * (arfcn - 512)
                val uplink = downlink - 95
                Pair(downlink, uplink)
            }
            // GSM 1900 (PCS 1900)
            arfcn in 512..810 -> {
                val downlink = 1930.0 + 0.2 * (arfcn - 512)
                val uplink = downlink - 80
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

}