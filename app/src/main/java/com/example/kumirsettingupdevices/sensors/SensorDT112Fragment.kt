package com.example.kumirsettingupdevices.sensors

import android.app.Activity
import android.app.AlertDialog
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
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSensorID
import com.example.kumirsettingupdevices.usb.OneWire
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment

class SensorDT112Fragment : Fragment(), UsbFragment, RealUpdateTempInterface<ItemSensorID>, OneWireInterfacePower {

    override val usbCommandsProtocol = UsbCommandsProtocol()
    var flagСancellation = false

    private lateinit var binding: FragmentSensorDT112Binding
    private var flagWorkScan = false


    // адаптер для вывода адресов и температур датчиков
    private lateinit var itemSensorIDAdapter: ItemSensorIDAdapter

    private lateinit var oneWire: OneWire

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {



        binding = FragmentSensorDT112Binding.inflate(inflater)

        // инициализация класса для общения по oneWire
        val context: Context = requireContext()
        if (context is MainActivity) {
            oneWire = OneWire(context.usb, context)


        }


        // кнопка отмены
        binding.buttonStop.setOnClickListener {
            showConfirmationDialog()
        }

        binding.buttonOKStop.setOnClickListener {
            flagСancellation = true
            binding.menuStopScan.visibility = View.GONE
        }

        binding.buttonNoStop.setOnClickListener {
            binding.menuStopScan.visibility = View.GONE
        }

        // если выстаавлен флаг онлайн считывания температуры то флаг получения температуры выставляем тоже
        binding.checkBoxOnline.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                binding.switchTemp.isChecked = true
            else
                flagСancellation = true
        }
        binding.switchTemp.setOnCheckedChangeListener { _, isChacked ->
            if (binding.checkBoxOnline.isChecked && !isChacked)
                binding.switchTemp.isChecked = true
        }

        // отслеживание того что найдено
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (binding.checkBoxOnline.isChecked) {
                    showAlertDialog("Для того что-бы произвести поиск убаерите галочку автоматическогоо обновления значений")
                    return
                }

                // обновляем все до изначального
                for (i in 0..<oneWire.listOneWireAddres.size) {
                    oneWire.listOneWireAddres[i].sensorID = oneWire.listOneWireAddres[i].sensorID.replace("\n", "")
                }

                itemSensorIDAdapter = ItemSensorIDAdapter(requireContext(), oneWire.listOneWireAddres)
                binding.recyclerSensors.adapter = itemSensorIDAdapter
                binding.recyclerSensors.layoutManager =
                    LinearLayoutManager(requireContext())

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
                    val listIndexesAddress = mutableListOf<Int>()
                    var allAdreses = ""
                    for ((indexAddress, id) in oneWire.listOneWireAddres.withIndex()) {
                        if (id.sensorID.lowercase().contains(binding.inputSearch.text.toString().lowercase())) {
                            allAdreses += " ${id.sensorID}"
                            cntContains++
                            listIndexesAddress.add(indexAddress)
                        }
                    }

                    // нету совпадений
                    if (cntContains == 0) {
                        binding.textSesrchRezult.text = getString(R.string.findCoincidences) + " " +
                                getString(R.string.No)
                    } else {
                        binding.textSesrchRezult.text = getString(R.string.findCoincidences) + allAdreses

                        // перебираем и обновлеям меняя местами и перемещяя наверх найденое и выдиляя
                        for (addressIndex in listIndexesAddress) {
                            val currentItem = oneWire.listOneWireAddres[addressIndex]
                            currentItem.sensorID += '\n'
                            itemSensorIDAdapter.currentHighlighting(addressIndex, currentItem)
                        }
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

    private fun showConfirmationDialog() {

        binding.menuStopScan.visibility = View.VISIBLE

        /*val myText = getString(R.string.checkingForCancellation)

        val builder = AlertDialog.Builder(context)
        builder.setMessage(myText)
            .setPositiveButton("ОК") { dialog, _ ->
                flagСancellation = true
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()*/
    }



    private fun error(msg: String = "") {
        showAlertDialog(getString(R.string.errorCodeNone) + msg)
    }


    private fun getSensorsIDAndPrint() {

        // защита от двойного нажатия
        if (flagWorkScan) return
        flagWorkScan = true
        flagСancellation = false
        val context: Context = requireContext()
        if (context is MainActivity) {

            // откурваем загрузочное окно и фон
            binding.fonLoadMenu.visibility = View.VISIBLE
            binding.loadMenuProgress.visibility = View.VISIBLE
            binding.textLoadMenu.text = getString(R.string.scanAddreses)

            // поток для ожидания приход данных
            Thread {
                // чтение флага питания
                oneWire.getFlagPower((requireContext() as MainActivity), usbCommandsProtocol, this)


                // ждем получения данных
                expectationDataOneWite()

                // запускаем поиск
                oneWire.scanOneWireDevices(usbCommandsProtocol, this, (requireContext() as MainActivity))

                // ждем получения данных
                expectationDataOneWite()

                // выполнение в фоновом потоке
                (context as Activity).runOnUiThread {
                    // проверка данных
                    if (oneWire.listOneWireAddres.isEmpty()) {
                        if (!flagСancellation) error()
                    } else {

                        // выводим все найденые устройства
                        itemSensorIDAdapter = ItemSensorIDAdapter(requireContext(), oneWire.listOneWireAddres)
                        binding.recyclerSensors.adapter = itemSensorIDAdapter
                        binding.recyclerSensors.layoutManager =
                            LinearLayoutManager(requireContext())

                        // вывод количства адресов
                        binding.textCntDev.text = oneWire.listOneWireAddres.size.toString()
                    }
                }

                // теперь читаем температуру
                if (binding.switchTemp.isChecked && oneWire.listOneWireAddres.isNotEmpty()) {
                    // перееключение текста на поиск адресов
                    (context as Activity).runOnUiThread {
                        binding.textLoadMenu.text = getString(R.string.scanTemp)
                    }
                    // измерение температры
                    if (!binding.checkBoxOnline.isChecked) {
                        oneWire.getTempsDT112(usbCommandsProtocol, this)
                        expectationDataOneWite()
                        (context as Activity).runOnUiThread {
                            // выводим все найденые устройства
                            itemSensorIDAdapter = ItemSensorIDAdapter(requireContext(), oneWire.listOneWireAddres)
                            binding.recyclerSensors.adapter = itemSensorIDAdapter
                            binding.recyclerSensors.layoutManager =
                                LinearLayoutManager(requireContext())
                        }
                    } else {
                        // закрытие меню загрузки
                        closeMenuLoadUIThread()

                        // бесконечно пока флаг включен опрашиваем датчики
                        while (binding.checkBoxOnline.isChecked) {
                            oneWire.getTempsDT112(usbCommandsProtocol, this, true, this)
                            expectationDataOneWite()
                        }
                    }
                }

                //закрываем меню
                // закрытие меню загрузки
                closeMenuLoadUIThread()

                // заита от двойного нажатия
                flagWorkScan = false
            }.start()
        }
    }

    private fun closeMenuLoadUIThread() {
        (context as Activity).runOnUiThread {
            binding.fonLoadMenu.visibility = View.GONE
            binding.loadMenuProgress.visibility = View.GONE
        }
    }

    // метод для обновления температуры в 1 элементе
    override fun updateTempItem(index: Int, newItem: ItemSensorID) {
        itemSensorIDAdapter.updateItem(index, newItem)
    }

    // метод для вывода типа питания датчиков
    override fun printFlagPower(flagPower: Boolean) {
        if (flagPower) {
            binding.textAddres.text = getString(R.string.addres)
        } else {
            binding.textAddres.text = getString(R.string.addres) + "\uD83D\uDC1B"
        }
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

    // нунужные мметоды
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}
    override fun printSettingDevice(settingMap: Map<String, String>) {}
    override fun readSettingStart() {}
    override fun writeSettingStart() {}

}