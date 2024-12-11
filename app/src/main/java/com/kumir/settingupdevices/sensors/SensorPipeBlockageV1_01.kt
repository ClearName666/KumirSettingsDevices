package com.kumir.settingupdevices.sensors

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.adapters.ItemSensorPipeBlockageAdapter.ItemSensorPipeBlockageAdapter
import com.kumir.settingupdevices.databinding.FragmentSensorPipeBlockageV101Binding
import com.kumir.settingupdevices.model.recyclerModel.ItemSensorPipeBlockage
import com.kumir.settingupdevices.usb.OneWire
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment


class SensorPipeBlockageV1_01(val contextMain: MainActivity) : Fragment(), UsbFragment {

    override val usbCommandsProtocol = UsbCommandsProtocol()


    lateinit var binding: FragmentSensorPipeBlockageV101Binding

    private lateinit var oneWire: OneWire
    var flagWorkDiag: Boolean = false

    var flagСancellation = false
    var flagKolibrovka = false
    var flagEditThracholdForKolibrovka: Boolean = false

    var valueThracholdForKolibrovka: Int = 500

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

        // кнопка для колибровки
        binding.buttonKolibrovka.setOnClickListener {
            flagKolibrovka = true
        }

        // кнопка для колибровки много
        binding.buttonKolibrovkaMore.setOnClickListener {
            flagKolibrovka = true
        }

        // кнопка для назначения колибровачного значения
        binding.buttonEditThrachold.setOnClickListener {
            binding.menuEditThrashold.visibility = View.VISIBLE
            binding.fonMenuEditThrashold.visibility = View.VISIBLE
        }

        // кнопка для назначения колибровачного значения много
        binding.buttonEditThracholdMore.setOnClickListener {
            binding.menuEditThrashold.visibility = View.VISIBLE
            binding.fonMenuEditThrashold.visibility = View.VISIBLE
        }

        binding.buttonSaveThrashold.setOnClickListener {
            try {
                valueThracholdForKolibrovka = binding.inputThrashold.text.toString().toInt()

                flagEditThracholdForKolibrovka = true
                binding.menuEditThrashold.visibility = View.GONE
                binding.fonMenuEditThrashold.visibility = View.GONE
                binding.inputThrashold.setText("")
            } catch (e: Exception) {
                showAlertDialog(getString(R.string.errorNotValue))
            }
        }

        binding.fonMenuEditThrashold.setOnClickListener {
            binding.menuEditThrashold.visibility = View.GONE
            binding.fonMenuEditThrashold.visibility = View.GONE
        }

        // кнопка для нечала диагностики
        binding.buttonStartDiag.setOnClickListener {
            if (!flagWorkDiag) {
                if (!usbCommandsProtocol.flagWorkDiag) {
                    flagWorkDiag = true
                    startReadData()
                    binding.buttonStartDiag.text = getString(R.string.endDiagTitle)


                    // запускаем анимацию загрузки
                    binding.progressBarWorkDiag.visibility = View.VISIBLE
                } else {
                    showAlertDialog(getString(R.string.expectationPlease))
                }

            } else {
                endDiag()
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

    fun printInfo(byteData: ByteArray, index: Int) {

        // ожидаемая структыра данных
        /*{
            u16 Threshold;
            u16 RawValue;
            u16 RawState;
            u16 ;
          } SCPDATA;
        */

        // только если диагностика продолжается
        if (flagWorkDiag) {

            // активация вохможности калибровки
            binding.buttonKolibrovka.visibility = View.VISIBLE
            binding.buttonEditThrachold.visibility = View.VISIBLE

            if (byteData.size == 8) {
                // распарсиваем данне
                val threshold: Int =
                    ((byteData[1].toInt() and 0xFF) shl 8) or (byteData[0].toInt() and 0xFF)
                val rawValue: Int =
                    ((byteData[3].toInt() and 0xFF) shl 8) or (byteData[2].toInt() and 0xFF)
                val rawState: Int =
                    ((byteData[5].toInt() and 0xFF) shl 8) or (byteData[4].toInt() and 0xFF)
                val state: Int =
                    ((byteData[7].toInt() and 0xFF) shl 8) or (byteData[6].toInt() and 0xFF)

                // обновляем данные в листе с данными об показаниях сенсоров
                oneWire.listOneWirePipeSensorsAddress[index] = ItemSensorPipeBlockage(
                    threshold,
                    rawValue,
                    rawState == TRUE_DATA,
                    state == TRUE_DATA,
                    oneWire.listOneWirePipeSensorsAddress[index].address
                    )


                // если датчик 1 то делаем одиночный вывод
                if (oneWire.listOneWirePipeSensorsAddress.size == 1) {

                    // активация одиночного лайаута
                    binding.oneSensor.visibility = View.VISIBLE
                    binding.moreSensors.visibility = View.GONE

                    // выводим данные
                    binding.thresholdValueText.text = threshold.toString()
                    binding.rawValueValueText.text = rawValue.toString()


                    // состояния
                    if (rawState == TRUE_DATA) {
                        binding.rawStateValueText.text = getString(R.string.Yes)
                        binding.layoutRawState.setBackgroundResource(R.drawable.error_rounded_background)
                    } else {
                        binding.rawStateValueText.text = getString(R.string.No)
                        binding.layoutRawState.setBackgroundResource(R.drawable.rounded_background2)
                    }

                    if (state == TRUE_DATA) {
                        binding.stateValueText.text = getString(R.string.Yes)
                        binding.layoutState.setBackgroundResource(R.drawable.error_rounded_background)

                    } else {
                        binding.stateValueText.text = getString(R.string.No)
                        binding.layoutState.setBackgroundResource(R.drawable.rounded_background2)

                    }

                    // выводим адресс
                    try {
                        binding.textAddressOne.text = oneWire.listOneWirePipeSensorsAddress[0].address

                        if (!oneWire.listOneWirePipeSensorsAddress[0].address.endsWith("85")) {
                            showAlertDialog(getString(R.string.errorTypeSensor))
                            endDiag()
                        }
                    } catch (_: Exception) {}


                    // для множественного вывода
                } else {
                    // активация многочисленного лайаута
                    binding.oneSensor.visibility = View.GONE
                    binding.moreSensors.visibility = View.VISIBLE

                    // кнопки для колибровки
                    binding.buttonKolibrovkaMore.visibility = View.VISIBLE
                    binding.buttonEditThracholdMore.visibility = View.VISIBLE

                    val itemSensorPipeBlockageAdapter = ItemSensorPipeBlockageAdapter(contextMain, oneWire.listOneWirePipeSensorsAddress)
                    binding.recyclerViewSensorsPipeBlockage.adapter = itemSensorPipeBlockageAdapter
                    binding.recyclerViewSensorsPipeBlockage.layoutManager = LinearLayoutManager(requireContext())
                }
            } else {
                error()
            }
        }

    }

    fun error(msg: String = "") {
        showAlertDialog(getString(R.string.errorCodeNone) + msg)
        endDiag()
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
            oneWire.getDataPipeBlockage(contextMain, usbCommandsProtocol, this)

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

    // завершение диагностикии
    private fun endDiag() {
        // завершение
        binding.buttonStartDiag.text = getString(R.string.startDiagTitle)

        binding.buttonEditThrachold.visibility = View.GONE
        binding.buttonKolibrovka.visibility = View.GONE

        flagWorkDiag = false

        // завершаем анимацию загрузки
        binding.progressBarWorkDiag.visibility = View.GONE

        // обратно возвращяем текст вместо значений
        binding.thresholdValueText.text = getString(R.string.value)
        binding.rawStateValueText.text = getString(R.string.value)
        binding.rawValueValueText.text = getString(R.string.value)
        binding.stateValueText.text = getString(R.string.value)

        binding.layoutRawState.setBackgroundResource(R.drawable.rounded_background2)
        binding.layoutState.setBackgroundResource(R.drawable.rounded_background2)

        // очещяем многочисленные выводы
        val itemSensorPipeBlockageAdapter = ItemSensorPipeBlockageAdapter(contextMain, mutableListOf())
        binding.recyclerViewSensorsPipeBlockage.adapter = itemSensorPipeBlockageAdapter
        binding.recyclerViewSensorsPipeBlockage.layoutManager = LinearLayoutManager(requireContext())

        // выводим одиночный вывод данных потому что он главный
        binding.oneSensor.visibility = View.VISIBLE
        binding.moreSensors.visibility = View.GONE

        // закрываем вохможность настроить трешхолд
        binding.buttonKolibrovka.visibility = View.GONE
        binding.buttonEditThrachold.visibility = View.GONE

        binding.buttonKolibrovkaMore.visibility = View.GONE
        binding.buttonEditThracholdMore.visibility = View.GONE

        binding.textAddressOne.text = ""
    }


    // на слуучай отключения кабеля
    override fun lockFromDisconnected(connect: Boolean) {
        if (!connect) {
            // кнопка для начала диагностики (выводит диалог о том что не успешно)
            binding.buttonStartDiag.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }

            endDiag()
        } else {
            // кнопка для нечала диагностики
            binding.buttonStartDiag.setOnClickListener {
                if (!flagWorkDiag) {
                    if (!usbCommandsProtocol.flagWorkDiag) {
                        flagWorkDiag = true
                        startReadData()
                        binding.buttonStartDiag.text = getString(R.string.endDiagTitle)

                        // запускаем анимацию загрузки
                        binding.progressBarWorkDiag.visibility = View.VISIBLE
                    } else {
                        showAlertDialog(getString(R.string.expectationPlease))
                    }
                } else {
                    endDiag()

                }
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