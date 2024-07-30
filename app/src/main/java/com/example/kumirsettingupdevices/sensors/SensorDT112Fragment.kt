package com.example.kumirsettingupdevices.sensors

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemPingrecvAdapter.ItemSensorIDAdapter
import com.example.kumirsettingupdevices.databinding.FragmentSensorDT112Binding
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment

class SensorDT112Fragment : Fragment(), UsbFragment {

    override val usbCommandsProtocol = UsbCommandsProtocol()
    private lateinit var binding: FragmentSensorDT112Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSensorDT112Binding.inflate(inflater)

        // отслеживание того что найдено
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Код, который выполняется в момент изменения текста

                //  если нечего нет то выходим и убераем поле результата посика
                if (binding.inputSearch.text.toString().isEmpty()) {
                    binding.layoutSearchRezult.visibility = View.GONE
                    return
                }

                binding.layoutSearchRezult.visibility = View.VISIBLE

                // что то введено
                val context: Context = requireContext()
                if (context is MainActivity) {
                    // переребираем все и говорм есть они или нет
                    var cntContains = 0 // счетчик совпадений

                    // перебор всех адресов
                    var allAdreses = ""
                    for (id in context.usb.listOneWireAddres) {
                        if (id.lowercase().contains(binding.inputSearch.text.toString().lowercase())) {
                            allAdreses += " $id"
                            cntContains++
                        }
                    }

                    // нету совпадений
                    if (cntContains == 0) {
                        binding.textSesrchRezult.text = getString(R.string.findCoincidences) + " " +
                                getString(R.string.No)
                    } else {
                        binding.textSesrchRezult.text = getString(R.string.findCoincidences) + allAdreses
                    }
                }
            }


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })


        // кнопка для начала диагностики
        binding.layoutButtonScanerStart.setOnClickListener {
            getSensorsIDAndPrint()
        }

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



    private fun error(msg: String = "") {
        showAlertDialog(getString(R.string.errorCodeNone) + msg)
    }


    private fun getSensorsIDAndPrint() {
        val context: Context = requireContext()
        if (context is MainActivity) {

            // откурваем загрузочное окно и фон
            binding.fonLoadMenu.visibility = View.VISIBLE

            // запускаем поиск
            context.usb.scanOneWireDevices(usbCommandsProtocol, (requireContext() as MainActivity))

            // поток для ожидания приход данных
            Thread {
                // ждем получения данных
                Thread.sleep(context.usb.TIMEOUT_GET_ONEWIRE + 100)

                // выполнение в фоновом потоке
                (context as Activity).runOnUiThread {
                    // проверка данных
                    if (context.usb.listOneWireAddres.isEmpty()) {
                        error()
                    } else {

                        // выводим все найденые устройства
                        val itemSensorIDAdapter =
                            ItemSensorIDAdapter(requireContext(), context.usb.listOneWireAddres)
                        binding.recyclerSensors.adapter = itemSensorIDAdapter
                        binding.recyclerSensors.layoutManager =
                            LinearLayoutManager(requireContext())

                    }

                    // закрытие меню загрузки
                    binding.fonLoadMenu.visibility = View.GONE
                    binding.loadMenuProgress.visibility = View.GONE
                }
            }.start()

            // поток для анимации
            Thread {
                (context as Activity).runOnUiThread {
                    binding.loadMenuProgress.visibility = View.VISIBLE
                }

                // шаг для загрузки прогрсс бара
                val page: Long = (context.usb.TIMEOUT_GET_ONEWIRE + 100) / 100

                // делаем загрузку
                for (i in 1..100) {
                    Thread.sleep(page)
                    (context as Activity).runOnUiThread {
                        binding.progressBarLoad.progress = i
                    }
                }
                (context as Activity).runOnUiThread {
                    binding.loadMenuProgress.visibility = View.GONE
                }
            }.start()
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
            binding.layoutButtonScanerStart.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        } else {
            // кнопка для начала диагностики
            binding.layoutButtonScanerStart.setOnClickListener {
                getSensorsIDAndPrint()
            }
        }
    }

    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}
    override fun printSettingDevice(settingMap: Map<String, String>) {}
    override fun readSettingStart() {}
    override fun writeSettingStart() {}
}