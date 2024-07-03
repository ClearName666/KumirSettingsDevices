package com.example.kumirsettingupdevices.usbFragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.adapters.ItemAbanentAdapter.ItemAbanentAdapter
import com.example.kumirsettingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.example.kumirsettingupdevices.databinding.FragmentP101Binding
import com.example.kumirsettingupdevices.formaters.FormatDataProtocol
import com.example.kumirsettingupdevices.model.recyclerModel.ItemAbanent
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment

class P101Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentP101Binding

    override val usbCommandsProtocol = UsbCommandsProtocol()

    private var listKeyAbanents: MutableList<String> = mutableListOf()
    private var flagRead: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentP101Binding.inflate(inflater)

        // назначение клика на меню что бы добавлять и удалять данные
        binding.fonWindowDarck.setOnClickListener {
            binding.editMenuAbanent.visibility = View.GONE
            binding.fonWindowDarck.visibility = View.GONE
        }
        // назначение кликак что бы добавлять абанента
        binding.buttonSave.setOnClickListener {
            writeSettingStart()
        }


        // поддержка пока что только 1 абанентаа((((------------------------------------------------
        binding.buttonAddAbanent.setOnClickListener {
            val command: MutableList<String> = mutableListOf()

            for (itemAb in listKeyAbanents) {
                val key = itemAb.replace(" ", "").replace("\n", "").
                    replace("\r", "")

                if (key.isNotEmpty()) {
                    command.add(getString(R.string.commandSetAbonent) + key)
                    command.add(getString(R.string.commandGetAdLoad))
                    command.add(getString(R.string.commandGetAbView))
                }

            }

            val usbCommandsProtocol = UsbCommandsProtocol()
            usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
        }
        binding.buttonAddAbanent.visibility = View.GONE




        // клики для чтения и записи
        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice(it)
        }

        createAdapters()

        return binding.root
    }

    private fun createAdapters() {

        // адаптер для выбора скорости
        val itemSelectSpeed = listOf(
            getString(R.string.speed_1200),
            getString(R.string.speed_2400),
            getString(R.string.speed_4800),
            getString(R.string.speed_9600),
            getString(R.string.speed_19200),
            getString(R.string.speed_38400),
            getString(R.string.speed_57600),
            getString(R.string.speed_115200)
        )
        // адаптер для выбора четности
        val itemSelectParity = listOf(
            getString(R.string.none),
            getString(R.string.even),
            getString(R.string.odd)
        )
        // адаптер для выбора стоп бит
        val itemSelectStopBit = listOf(
            getString(R.string.one),
            getString(R.string.two)
        )
        // адаптер дл я выбора битов данных
        val itemSelectBitData = listOf(
            getString(R.string.eight),
            getString(R.string.seven)
        )

        val adapterSelectSpeed = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed)
        val adapterSelectParity = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectParity)
        val adapterSelectStopBit = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectStopBit)
        val adapterSelectBitData = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectBitData)

        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectParity.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectStopBit.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectBitData.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerParity.adapter = adapterSelectParity
        binding.spinnerStopBit.adapter = adapterSelectStopBit
        binding.spinnerBitData.adapter = adapterSelectBitData
    }

    private fun onClickReadSettingsDevice(view: View) {
        readSettingStart()
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {

        // если чтение иначе вывод абанентов
        if (!flagRead) {
            binding.buttonAddAbanent.visibility = View.VISIBLE

            // делаем так что бы больше незя было прочитать устройство и нарушить алгаритм исполнения
            binding.imagedischarge.setOnClickListener {
                showAlertDialog(getString(R.string.readAlready))
            }
            // ------------------------------------------------------------

            // верийный номер и версия прошибки
            binding.serinerNumber.text = getString(R.string.serinerNumber) +
                    "\n" + settingMap[getString(R.string.commandGetSerialNum)]

            binding.textVersionFirmware.text = getString(R.string.versionProgram) +
                    "\n" + settingMap[getString(R.string.commandGetVersionFirmware)]


            binding.textSizeMember.text = getString(R.string.sizeMemberTitle) +
                    "\n" + settingMap[getString(R.string.commandGetFspace)]

            binding.textDriverVersion.text = getString(R.string.driverTitle) +
                    "\n" + settingMap[getString(R.string.commandGetDriver)]?.
            substringAfter("DRIVER: ")?.substringBefore("\n")

            // запись абанентов в лист
            val listAbanent: List<String> = settingMap[getString(R.string.commandGetAbanents)]?.replace("OK", "")?.split("\n")!!
            listKeyAbanents = listAbanent.map { it.substringAfter("ABONENT: ") }.toMutableList()


            // добавления адептера в выборку драйвера
            val listDriverStr: List<String> = settingMap[getString(R.string.commandGetDriver)]?.
                replace("OK", "")?.split("\n")!!
                .filter { it.trim().isNotEmpty() }

            val itemDrivers = listDriverStr.map { it.substringAfter("DRIVER: ").substringBefore(".") }

            val adapterDrivers = ArrayAdapter(requireContext(),
                R.layout.item_spinner, itemDrivers)
            adapterDrivers.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerDriver.adapter = adapterDrivers

            // для контроля
            flagRead = true
        } else {
            // клик добавления абанента
            binding.buttonAddAbanent.text = getString(R.string.addAbanentTitle)
            binding.buttonAddAbanent.setOnClickListener {
                binding.fonWindowDarck.visibility = View.VISIBLE
                binding.editMenuAbanent.visibility = View.VISIBLE
            }


            // вывод абанентов в список // только 1 абанент
            val ab: String = settingMap[getString(R.string.commandGetAbView)]!!
            val itemsAbonents: List<ItemAbanent> = listOf(
                ItemAbanent(ab.substringAfter("ABONENT: ").substringBefore("\n"),
                    ab.substringAfter("ABNAME: ").substringBefore("\n"),
                    "",
                    ab.substringAfter("ABDRIVER: ").substringBefore("\n"),
                    ab.substringAfter("ABDEVID: ").substringBefore("\n"),
                    ab.substringAfter("ABPORT: ").substringBefore("\n"),
                    "",
                    "",
                    ab.substringAfter("ABPARAMS: ").substringBefore("\n"),
                    false
                )
            )



            val itemAbonentAdapter = ItemAbanentAdapter(requireContext(), itemsAbonents)
            binding.recyclerView.adapter = itemAbonentAdapter
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun readSettingStart() {
        val command: List<String> = arrayListOf(
            getString(R.string.commandGetSerialNum),
            getString(R.string.commandGetVersionFirmware),
            getString(R.string.commandGetFspace),
            getString(R.string.commandGetAbanents),
            getString(R.string.commandGetAbView),
            getString(R.string.commandGetDevList)
        )

        usbCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }

    override fun writeSettingStart() {

        // закрфваем окно с редактированием абанента
        binding.fonWindowDarck.visibility = View.GONE
        binding.editMenuAbanent.visibility = View.GONE

        val formatDataProtocol = FormatDataProtocol()
        val dataMap: Map<String, String> = mapOf(
            getString(R.string.commandSetAbonent) to binding.inputKey.text.toString(),
            getString(R.string.commandGetAdLoad) to "",
            getString(R.string.commandSetAbonentName) to binding.inputName.text.toString(),
            getString(R.string.commandSetDriver) to binding.spinnerDriver.selectedItem.toString(),
            getString(R.string.commandSetDevId) to binding.inputNumDevice.text.toString(),
            getString(R.string.commandSetPortSet) to "${binding.spinnerSpeed.selectedItem}," +
                    "${binding.spinnerBitData.selectedItem}," +
                    "${formatDataProtocol.formatParityFromIndex(binding.spinnerParity.selectedItemPosition)}," +
                    "${binding.spinnerStopBit.selectedItem}," +
                    "${binding.inputRange.text}," +
                    "${binding.inputTimeOut.text}",
            getString(R.string.commandSetParams) to "p=${binding.inputPassword.text}a;n=${binding.inputAdress.text};a=t"
        )

        usbCommandsProtocol.writeSettingDevice(dataMap, requireContext(), this)
    }

    override fun lockFromDisconnected(connect: Boolean) {
        // текстрки для кнопок
        val drawablImageDischarge = ContextCompat.getDrawable(requireContext(), R.drawable.discharge)

        if (!connect) {
            //------------------------------------------------------------------------------------------
            // покраска кнопки записи в серый
            // Обертываем наш Drawable для совместимости и изменяем цвет

            drawablImageDischarge?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GRAY)
                binding.imagedischarge.setImageDrawable(wrappedDrawable)
            }

            //--------------------------------------------------------------------------------------

            // убераем возмоэность читать и записывать
            binding.imagedischarge.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        } else {
            drawablImageDischarge?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GREEN)
                binding.imagedischarge.setImageDrawable(wrappedDrawable)
            }

            // установка клика
            binding.imagedischarge.setOnClickListener {
                onClickReadSettingsDevice(it)
            }
        }
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }
}