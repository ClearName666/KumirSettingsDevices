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
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.databinding.FragmentK21k23Binding
import com.example.kumirsettingupdevices.modems.FormatModems
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment
import com.example.kumirsettingupdevices.usb.UsbModBasCommandProtocol


class K21K23Fragment : Fragment(), UsbFragment {

    private lateinit var binding: FragmentK21k23Binding

    private var NAME_TYPE_DEVICE = "KUMIR-К21К23 READY"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentK21k23Binding.inflate(inflater)

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.k21_k23))
        }

        // клики

        //------------------------------------------------------------------------------------------
        // покраска кнопки записи в серый
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.download)

        // Обертываем наш Drawable для совместимости и изменяем цвет
        drawable?.let {
            val wrappedDrawable = DrawableCompat.wrap(it)

            DrawableCompat.setTint(wrappedDrawable, Color.GRAY)

            binding.imageDownLoad.setImageDrawable(wrappedDrawable)
        }
        //------------------------------------------------------------------------------------------

        binding.imagedischarge.setOnClickListener {
            onClickReadSettingsDevice(it)

            // Обертываем наш Drawable для совместимости и изменяем цвет
            drawable?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)

                DrawableCompat.setTint(wrappedDrawable, Color.RED)

                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }

            // только после чтения
            binding.imageDownLoad.setOnClickListener {
                onClickWriteSettingsDevice(it)
            }
        }
        binding.imageDownLoad.setOnClickListener {
            showAlertDialog(getString(R.string.nonWriteSetting))
        }

        createAdapters()

        return binding.root
    }
    private fun createAdapters() {
        // адаптер для выбора режима работы модема
        val itemsSpinnerDevMode = listOf(
            getString(R.string.devmodeROUTER),
            getString(R.string.devmodeCANPROXY),
            getString(R.string.devmodeRS485),
            getString(R.string.devmodeMONITOR)
        )

        // адаптер для выбора приборра учета
        val itemPortDeviceAccounting = listOf(
            getString(R.string.deviceAccountingAdvancedSettings),
            getString(R.string.deviceAccountingSPT941),
            getString(R.string.deviceAccountingSPT944),
            getString(R.string.deviceAccountingTSP025),
            getString(R.string.deviceAccountingPSCH4TMV23),
            getString(R.string.deviceAccountingTSP027),
            getString(R.string.deviceAccountingPowerCE102M),
            getString(R.string.deviceAccountingMercury206),
            getString(R.string.deviceAccountingKM5PM5),
            getString(R.string.deviceAccountingTEM104),
            getString(R.string.deviceAccountingTEM106),
            getString(R.string.deviceAccountingBKT5),
            getString(R.string.deviceAccountingBKT7),
            getString(R.string.deviceAccountingTePOCC),
            getString(R.string.deviceAccountingSPT943),
            getString(R.string.deviceAccountingSPT961),
            getString(R.string.deviceAccountingKT7Abacan),
            getString(R.string.deviceAccountingMT200DS),
            getString(R.string.deviceAccountingTCP010),
            getString(R.string.deviceAccountingTCP010M),
            getString(R.string.deviceAccountingTCP023),
            getString(R.string.deviceAccountingTCPB024),
            getString(R.string.deviceAccountingTCP026),
            getString(R.string.deviceAccountingTCPB03X),
            getString(R.string.deviceAccountingTCPB042),
            getString(R.string.deviceAccountingYCPB5XX),
            getString(R.string.deviceAccountingPCL212),
            getString(R.string.deviceAccountingSA942M),
            getString(R.string.deviceAccountingSA943),
            getString(R.string.deviceAccountingMKTC),
            getString(R.string.deviceAccountingCKM2),
            getString(R.string.deviceAccountingDymetic5102),
            getString(R.string.deviceAccountingTEPLOVACHESLITELTB7),
            getString(R.string.deviceAccountingELF),
            getString(R.string.deviceAccountingSTU1),
            getString(R.string.deviceAccountingTURBOFLOUGFGF),
            getString(R.string.deviceAccountingEK260),
            getString(R.string.deviceAccountingEK270),
            getString(R.string.deviceAccountingBKG2),
            getString(R.string.deviceAccountingCPG741),
            getString(R.string.deviceAccountingCPG742),
            getString(R.string.deviceAccountingTC2015),
            getString(R.string.deviceAccountingMERCURI230ART5),
            getString(R.string.deviceAccountingPULSAR2M),
            getString(R.string.deviceAccountingPULSAR10M),
            getString(R.string.deviceAccountingKUMIRK21K22),
            getString(R.string.deviceAccountingIM2300),
            getString(R.string.deviceAccountingENERGOMERACE303),
            getString(R.string.deviceAccountingTEM116)
        )
        // адаптер для выбора скорости
        val itemSelectSpeed = listOf(
            getString(R.string.speed_300),
            getString(R.string.speed_600),
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

        val adapter = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemsSpinnerDevMode)
        val adapterPortDeviceAccounting = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemPortDeviceAccounting)
        val adapterSelectSpeed = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed)
        val adapterSelectParity = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectParity)
        val adapterSelectStopBit = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectStopBit)
        val adapterSelectBitData = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectBitData)


        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        adapterPortDeviceAccounting.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectParity.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectStopBit.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectBitData.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)


        binding.spinnerSelectPort1MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort1.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort1.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort1.adapter = adapterSelectBitData

        binding.spinnerSelectPort2MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed2.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort2.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort2.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort2.adapter = adapterSelectBitData

        binding.spinnerSelectPort3MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed3.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort3.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort3.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort3.adapter = adapterSelectBitData

        binding.spinnerSelectPort4MeteringDevice.adapter = adapterPortDeviceAccounting
        binding.spinnerSpeed4.adapter = adapterSelectSpeed
        binding.spinnerSelectParityPort4.adapter = adapterSelectParity
        binding.spinnerSelectStopBitPort4.adapter = adapterSelectStopBit
        binding.spinnerBitDataPort4.adapter = adapterSelectBitData
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {

        val version: String = getString(R.string.versionProgram) +
                "\n" + settingMap[0xA3.toByte().toString()]
        binding.textVersionFirmware.text = version


        val formatModems = FormatModems()

        // настроки 1 порта
        val listPort1: List<String>? = settingMap[0xA4.toByte().toString() +
                0x01.toByte().toString()]?.drop(1)?.dropLast(1)?.split(", ")

        listPort1.let { port ->
            try {
                binding.spinnerSelectPort1MeteringDevice.setSelection(0)
                port?.get(3)?.toInt()
                    ?.let { formatModems.formatPatityModBas(it.toByte()) }
                    ?.let { binding.spinnerSelectParityPort1.setSelection(it) }
                port?.get(2)?.toInt()
                    ?.let { formatModems.formatStopBitModBas(it) }
                    ?.let { binding.spinnerSelectStopBitPort1.setSelection(it) }
                port?.get(1)?.toInt()
                    ?.let { formatModems.formatBitDataModBas(it) }
                    ?.let { binding.spinnerBitDataPort1.setSelection(it) }
                port?.get(0)?.toInt()
                    ?.let { formatModems.formatSpeedModBas(it) }
                    ?.let { binding.spinnerSpeed.setSelection(it) }
            } catch (_: Exception) {}
        }

        // настроки 2 порта
        val listPort2: List<String>? = settingMap[0xA4.toByte().toString() +
                0x02.toByte().toString()]?.drop(1)?.dropLast(1)?.split(", ")

        listPort2.let { port ->
            try {
                binding.spinnerSelectPort2MeteringDevice.setSelection(0)
                port?.get(3)?.toInt()
                    ?.let { formatModems.formatPatityModBas(it.toByte()) }
                    ?.let { binding.spinnerSelectParityPort2.setSelection(it) }
                port?.get(2)?.toInt()
                    ?.let { formatModems.formatStopBitModBas(it) }
                    ?.let { binding.spinnerSelectStopBitPort2.setSelection(it) }
                port?.get(1)?.toInt()
                    ?.let { formatModems.formatBitDataModBas(it) }
                    ?.let { binding.spinnerBitDataPort2.setSelection(it) }
                port?.get(0)?.toInt()
                    ?.let { formatModems.formatSpeedModBas(it) }
                    ?.let { binding.spinnerSpeed2.setSelection(it) }
            } catch (_: Exception) {}
        }

        // настроки 3 порта
        val listPort3: List<String>? = settingMap[0xA4.toByte().toString() +
                0x03.toByte().toString()]?.drop(1)?.dropLast(1)?.split(", ")

        listPort3.let { port ->
            try {
                binding.spinnerSelectPort3MeteringDevice.setSelection(0)
                port?.get(3)?.toInt()
                    ?.let { formatModems.formatPatityModBas(it.toByte()) }
                    ?.let { binding.spinnerSelectParityPort3.setSelection(it) }
                port?.get(2)?.toInt()
                    ?.let { formatModems.formatStopBitModBas(it) }
                    ?.let { binding.spinnerSelectStopBitPort3.setSelection(it) }
                port?.get(1)?.toInt()
                    ?.let { formatModems.formatBitDataModBas(it) }
                    ?.let { binding.spinnerBitDataPort3.setSelection(it) }
                port?.get(0)?.toInt()
                    ?.let { formatModems.formatSpeedModBas(it) }
                    ?.let { binding.spinnerSpeed3.setSelection(it) }
            } catch (_: Exception) {}
        }

        // настроки 4 порта
        val listPort4: List<String>? = settingMap[0xA4.toByte().toString() +
                0x04.toByte().toString()]?.drop(1)?.dropLast(1)?.split(", ")

        listPort4.let { port ->
            try {
                binding.spinnerSelectPort4MeteringDevice.setSelection(0)
                port?.get(3)?.toInt()
                    ?.let { formatModems.formatPatityModBas(it.toByte()) }
                    ?.let { binding.spinnerSelectParityPort4.setSelection(it) }
                port?.get(2)?.toInt()
                    ?.let { formatModems.formatStopBitModBas(it) }
                    ?.let { binding.spinnerSelectStopBitPort4.setSelection(it) }
                port?.get(1)?.toInt()
                    ?.let { formatModems.formatBitDataModBas(it) }
                    ?.let { binding.spinnerBitDataPort4.setSelection(it) }
                port?.get(0)?.toInt()
                    ?.let { formatModems.formatSpeedModBas(it) }
                    ?.let { binding.spinnerSpeed4.setSelection(it) }
            } catch (_: Exception) {}
        }
    }

    override fun readSettingStart() {
        val command: List<ByteArray> = arrayListOf(
            byteArrayOf(0xA3.toByte()),
            byteArrayOf(0xA4.toByte(), 0x01.toByte()),
            byteArrayOf(0xA4.toByte(), 0x02.toByte()),
            byteArrayOf(0xA4.toByte(), 0x03.toByte()),
            byteArrayOf(0xA4.toByte(), 0x04.toByte()),
        )

        val usbModBasCommandsProtocol = UsbModBasCommandProtocol()
        usbModBasCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }

    override fun writeSettingStart() {
        val formatModems = FormatModems()
        val command: List<ByteArray> = arrayListOf(
            byteArrayOf(
                0xA7.toByte(),
                0x01.toByte(),
                formatModems.reverseFormatSpeedModBas(binding.spinnerSpeed.selectedItemPosition),
                formatModems.reverseFormatBitDataModBas(binding.spinnerBitDataPort1.selectedItemPosition),
                formatModems.reverseFormatStopBitModBas(binding.spinnerSelectStopBitPort1.selectedItemPosition),
                formatModems.reverseFormatPatityModBas(binding.spinnerSelectParityPort1.selectedItemPosition)
            ),
            byteArrayOf(
                0xA7.toByte(),
                0x02.toByte(),
                formatModems.reverseFormatSpeedModBas(binding.spinnerSpeed2.selectedItemPosition),
                formatModems.reverseFormatBitDataModBas(binding.spinnerBitDataPort2.selectedItemPosition),
                formatModems.reverseFormatStopBitModBas(binding.spinnerSelectStopBitPort2.selectedItemPosition),
                formatModems.reverseFormatPatityModBas(binding.spinnerSelectParityPort2.selectedItemPosition)
            ),
            byteArrayOf(
                0xA7.toByte(),
                0x03.toByte(),
                formatModems.reverseFormatSpeedModBas(binding.spinnerSpeed3.selectedItemPosition),
                formatModems.reverseFormatBitDataModBas(binding.spinnerBitDataPort3.selectedItemPosition),
                formatModems.reverseFormatStopBitModBas(binding.spinnerSelectStopBitPort3.selectedItemPosition),
                formatModems.reverseFormatPatityModBas(binding.spinnerSelectParityPort3.selectedItemPosition)
            ),
            byteArrayOf(
                0xA7.toByte(),
                0x04.toByte(),
                formatModems.reverseFormatSpeedModBas(binding.spinnerSpeed4.selectedItemPosition),
                formatModems.reverseFormatBitDataModBas(binding.spinnerBitDataPort4.selectedItemPosition),
                formatModems.reverseFormatStopBitModBas(binding.spinnerSelectStopBitPort4.selectedItemPosition),
                formatModems.reverseFormatPatityModBas(binding.spinnerSelectParityPort4.selectedItemPosition)
            )
        )

        val usbModBasCommandsProtocol = UsbModBasCommandProtocol()
        usbModBasCommandsProtocol.writeSettingDevice(command, requireContext())
    }

    private fun onClickReadSettingsDevice(view: View) {
        val context: Context = requireContext()

        if (context is MainActivity) {
            context.curentData = NAME_TYPE_DEVICE // обход проверки индитификатора
            context.showTimerDialog(this, NAME_TYPE_DEVICE, false,false)
        }
    }

    private fun onClickWriteSettingsDevice(view: View) {
        writeSettingStart()
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }

}