package com.example.kumirsettingupdevices.usbFragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.databinding.FragmentK21k23Binding
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


        return binding.root
    }

    override fun printSerifalNumber(serialNumber: String) {
        binding.serinerNumber.text = serialNumber
    }

    override fun printVersionProgram(versionProgram: String) {
        binding.textVersionFirmware.text = versionProgram
    }

    override fun printSettingDevice(settingMap: Map<String, String>) {

        val version: String = getString(R.string.versionProgram) +
                "\n" + settingMap["0xA3"]
        binding.textVersionFirmware.text = version
    }

    override fun readSettingStart() {
        val command: List<ByteArray> = arrayListOf(
            byteArrayOf(0xA3.toByte())
        )

        val usbModBasCommandsProtocol = UsbModBasCommandProtocol()
        usbModBasCommandsProtocol.readSettingDevice(command, requireContext(), this)
    }

    override fun writeSettingStart() {

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