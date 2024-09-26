package com.kumir.settingupdevices.usbFragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.kumir.settingupdevices.LoadInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentFirmwareSTMBinding
import com.kumir.settingupdevices.usb.Stm
import com.kumir.settingupdevices.usb.StmLoader
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class FirmwareSTMFragment(private val contextMain: MainActivity): Fragment(), UsbFragment,
    LoadInterface {

    override val usbCommandsProtocol: UsbCommandsProtocol = UsbCommandsProtocol()

    private lateinit var stmLoader: StmLoader
    private lateinit var binding: FragmentFirmwareSTMBinding

    override fun onDestroyView() {
        contextMain.usb.onSerialSpeed(9) // 115200
        contextMain.usb.onSerialParity(0) // none

        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contextMain.usb.onSerialParity(1) // even
        contextMain.usb.onSelectUumBit(true) // 8 bit
        contextMain.usb.onSerialStopBits(0) // 1 bit

        binding = FragmentFirmwareSTMBinding.inflate(inflater)

        // клик на запись
        binding.imageDownLoad.setOnClickListener {
            onClickWriteSettingsDevice(binding.imageDownLoad)
        }

        // клик на то что перезкгрузил
        binding.buttonReboot.setOnClickListener {
            onClickResetButton()
        }

        stmLoader = StmLoader(usbCommandsProtocol, requireContext() as MainActivity)


        createAdapters()

        return binding.root
    }

    // создание адаптеров для выбора устройства
    private fun createAdapters() {

        // адаптер для выбора скорости
        val itemSelectSpeed = listOf(
            getString(R.string.speed_115200),
            getString(R.string.speed_230400)
        )
        // адаптер для выбора четности
        val itemSelectDevice = listOf(
            getString(R.string.m32Version3),
            getString(R.string.m32Version4),
            getString(R.string.m32lite)
        )

        val adapterSelectDevice = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectDevice)
        val adapterSelectSpeed= ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed)


        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)
        adapterSelectDevice.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerDevice.adapter = adapterSelectDevice
    }

    private fun onClickWriteSettingsDevice(view: View) {
        writeSettingStart()
    }

    override fun writeSettingStart() {

        // установка выбраной скоости
        contextMain.usb.onSerialSpeed(binding.spinnerSpeed.selectedItemPosition + 9)

        // открываем загрузочное меню
        binding.fonLoading.visibility = View.VISIBLE
        binding.loading.visibility = View.VISIBLE


        // поток для записи
        Thread {
            Log.d("loadFileStm", "Начало загрузки")

            try {
                val bootloaderFile = getTempFileFromAssets(requireContext(), binding.spinnerDevice.selectedItemPosition, true)
                val programFile = getTempFileFromAssets(requireContext(), binding.spinnerDevice.selectedItemPosition, false)

                Log.d("loadFileStm", "Файлы получены: $bootloaderFile, $programFile \n Начало установки файлов")


                // в зависемости от остоятельств запускаем в том или ином режиме
                if (!stmLoader.loadFile(
                        bootloaderFile, programFile,
                        0x08000000, 0x08008000,
                        (bootloaderFile.length().toInt() / 1024), (programFile.length().toInt() / 1024),
                        this,
                        binding.spinnerDevice.selectedItemPosition == 1,
                        if (binding.spinnerDevice.selectedItemPosition == 1) this else null
                )
                ) {
                    Log.d("loadFileStm", "Не успешно")
                }
            } catch (e: Exception) {
                closeMenuProgress()
                Log.e("loadFileStm", e.message.toString())
            }



            Log.d("loadFileStm", "Завершино")

        }.start()
    }


    private fun getTempFileFromAssets(context: Context, numberDev: Int, bootLoader: Boolean): File {
        // Открываем файл из assets
        val inputStream: InputStream? =
            if (numberDev == 2 && bootLoader) resources.openRawResource(R.raw.kumir_m32_lite_boot)
            else if (numberDev == 2 && !bootLoader) resources.openRawResource(R.raw.kumir_m32_lite_7_1_1_5260)
            else if (numberDev == 0 && bootLoader) resources.openRawResource(R.raw.kumir_m32c_7_1_5_6411_boot)
            else if (numberDev == 0 && !bootLoader) resources.openRawResource(R.raw.kumir_m32c_7_1_5_6411)
            else if (numberDev == 1 && bootLoader) resources.openRawResource(R.raw.kumir_m32d_7_2_6_6409_boot)
            else if (numberDev == 1 && !bootLoader) resources.openRawResource(R.raw.kumir_m32d_7_2_6_6409)
            else null

        // Создаем временный файл
        val tempFile = File.createTempFile("file", null, context.cacheDir)

        // Записываем данные из InputStream во временный файл
        FileOutputStream(tempFile).use { output ->
            inputStream?.copyTo(output)
        }

        return tempFile
    }


    // контроль отсоединения кабеля
    override fun lockFromDisconnected(connect: Boolean) {
        // текстрки для кнопок
        val drawablImageDownLoad = ContextCompat.getDrawable(requireContext(), R.drawable.download)

        if (!connect) {
            //------------------------------------------------------------------------------------------
            // покраска кнопки записи в серый
            // Обертываем наш Drawable для совместимости и изменяем цвет

            drawablImageDownLoad?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.GRAY)
                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }

            binding.imageDownLoad.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        } else {
            // -------------активайия кнопки после прочтения-------------
            // перекраска в красный цвет кнопки загрузки
            val drawablImageDownLoad = ContextCompat.getDrawable(requireContext(), R.drawable.download)
            drawablImageDownLoad?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.RED)
                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }

            // только после чтения
            binding.imageDownLoad.setOnClickListener {
                onClickWriteSettingsDevice(it)
            }
        }
    }

    private fun showAlertDialog(text: String) {
        contextMain.showAlertDialog(text)
    }

    // методы для управления отображением статуса прошивки
    override fun loadingProgress(prgress: Int) {
        binding.progressBarLoading.progress = prgress
    }

    override fun closeMenuProgress() {
        binding.fonLoading.visibility = View.GONE
        binding.loading.visibility = View.GONE
    }

    override fun errorSend() {
        closeMenuProgress()
        loadingProgress(0)
        binding.buttonReboot.visibility = View.GONE

        showAlertDialog(getString(R.string.errorLoadStm))

    }

    // требование перезагрузить устройство
    fun needResetPlease() {
        showAlertDialog(getString(R.string.pleaseResetStm203))
        binding.buttonReboot.visibility = View.VISIBLE
    }

    // нажатие на кнопку что бы уведомить о перезагрузки
    fun onClickResetButton()  {
        stmLoader.flagResetOk = true
        binding.buttonReboot.visibility = View.GONE
    }


    // неиспользуемые методы
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}
    override fun printSettingDevice(settingMap: Map<String, String>) {}
    override fun readSettingStart() {}

}