package com.kumir.settingupdevices.sensors

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentSensorPipeBlockageV101Binding
import com.kumir.settingupdevices.usb.OneWire
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment


class SensorPipeBlockageV1_01(val contextMain: MainActivity) : Fragment(), UsbFragment {

    override val usbCommandsProtocol = UsbCommandsProtocol()


    lateinit var binding: FragmentSensorPipeBlockageV101Binding

    private lateinit var oneWire: OneWire
    private var flagWorkDiag: Boolean = false

    var flagСancellation = false

    companion object {
        const val TIME_DIAG_DATA_SPLIT: Long = 500

        const val TRUE_DATA: Int = 42405
        const val FALSE_DATA: Int = 23130
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSensorPipeBlockageV101Binding.inflate(inflater)

        // кнопка для нечала диагностики
        binding.buttonStartDiag.setOnClickListener {
            if (!flagWorkDiag) {
                flagWorkDiag = true
                startReadData()
                binding.buttonStartDiag.text = getString(R.string.endDiagTitle)

                // запускаем анимацию загрузки
                binding.progressBarWorkDiag.visibility = View.VISIBLE
            } else {
                binding.buttonStartDiag.text = getString(R.string.startDiagTitle)
                flagWorkDiag = false

                // завершаем анимацию загрузки
                binding.progressBarWorkDiag.visibility = View.GONE

                // обратно возвращяем текст вместо значений
                binding.thresholdValueText.text = getString(R.string.value)
                binding.rawStateValueText.text = getString(R.string.value)
                binding.rawValueValueText.text = getString(R.string.value)
                binding.stateValueText.text = getString(R.string.value)
            }
        }

        // инициализация класса для общения по oneWire
        oneWire = OneWire(contextMain.usb, contextMain)


        return binding.root
    }

    override fun onDestroyView() {
        //переключение скорости на 115200 (обратно)
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.usb.onSerialSpeed(9)
        }

        super.onDestroyView()
    }

    fun printInfo(byteData: ByteArray) {

        // ожидаемая структыра данных
        /*{
            u16 Threshold;
            u16 RawValue;
            u16 RawState;
            u16 ;
          } SCPDATA;
        */

        if (byteData.size == 8) {
            val threshold: Int = ((byteData[1].toInt() and 0xFF) shl 8) or (byteData[0].toInt() and 0xFF)
            val rawValue: Int = ((byteData[3].toInt() and 0xFF) shl 8) or (byteData[2].toInt() and 0xFF)
            val rawState: Int = ((byteData[5].toInt() and 0xFF) shl 8) or (byteData[4].toInt() and 0xFF)
            val state: Int = ((byteData[7].toInt() and 0xFF) shl 8) or (byteData[6].toInt() and 0xFF)

            binding.thresholdValueText.text = threshold.toString()
            binding.rawValueValueText.text = rawValue.toString()


            // состояния
            if (rawState == TRUE_DATA) {
                binding.rawStateValueText.text = getString(R.string.Yes)
            } else {
                binding.rawStateValueText.text = getString(R.string.No)
            }

            if (state == TRUE_DATA) {
                binding.stateValueText.text = getString(R.string.Yes)
            } else {
                binding.stateValueText.text = getString(R.string.No)
            }
        } else {
            error()
        }

    }

    fun error(msg: String = "") {
        showAlertDialog(getString(R.string.errorCodeNone) + msg)
    }


    private fun startReadData() {
        flagСancellation = false



        // поток для обработки данных
        Thread {
            // запускаем поиск
            oneWire.scanOneWireDevices(usbCommandsProtocol, null, (requireContext() as MainActivity), this)

            // ждем получения данных
            expectationDataOneWite()

            // бесконечно опрашиваем пока не закончиим диагностику
            while (flagWorkDiag) {
                Thread.sleep(TIME_DIAG_DATA_SPLIT)
                if (!usbCommandsProtocol.flagWorkRead) // по завершению прошлого запуска делаем новый
                    oneWire.getDataPipeBlockage(contextMain, usbCommandsProtocol, this)
            }

        }.start()
    }


    private fun expectationDataOneWite() {
        Thread.sleep(100) // задержка для того что бы подождать старта

        val timeMax = 50000 // 500 секунда на то что бы получить все
        var time = 0
        while (usbCommandsProtocol.flagWorkOneWire && timeMax > time) {
            time++
            Thread.sleep(1)
        }
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }


    override fun lockFromDisconnected(connect: Boolean) {
        if (!connect) {
            // кнопка для начала диагностики (выводит диалог о том что не успешно)
            binding.buttonStartDiag.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }

            // завершение
            binding.buttonStartDiag.text = getString(R.string.startDiagTitle)
            flagWorkDiag = false

            // завершаем анимацию загрузки
            binding.progressBarWorkDiag.visibility = View.GONE
        } else {
            // кнопка для начала диагностики
            if (!flagWorkDiag) {
                flagWorkDiag = true
                startReadData()
                binding.buttonStartDiag.text = getString(R.string.endDiagTitle)

                // запускаем анимацию загрузки
                binding.progressBarWorkDiag.visibility = View.VISIBLE
            } else {
                binding.buttonStartDiag.text = getString(R.string.startDiagTitle)
                flagWorkDiag = false

                // завершаем анимацию загрузки
                binding.progressBarWorkDiag.visibility = View.GONE
            }
        }
    }


    // ненужные методы
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}
    override fun printSettingDevice(settingMap: Map<String, String>) {}
    override fun readSettingStart() {}
    override fun writeSettingStart() {}

}