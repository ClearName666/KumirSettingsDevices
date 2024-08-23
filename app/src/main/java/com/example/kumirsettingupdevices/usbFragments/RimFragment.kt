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
import com.example.kumirsettingupdevices.DataShowInterface
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.databinding.FragmentRimBinding
import com.example.kumirsettingupdevices.formaters.ValidDataSettingsDevice
import com.example.kumirsettingupdevices.usb.RimUsb
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbFragment
import kotlin.experimental.or


class RimFragment : Fragment(), UsbFragment, DataShowInterface {

    private lateinit var binding: FragmentRimBinding
    override val usbCommandsProtocol = UsbCommandsProtocol()


    private var flagPermissionShow: Boolean = false

    companion object {
        private const val SIZE_PASSWORD: Int = 6
        val DEFAULT_PASSWORD = ByteArray(SIZE_PASSWORD) { 0x00.toByte() }

        private const val SEARCH_SPEED_INDEX: Int = 5;
        private const val MAX_TIME_WAIT_SEARCH_SPEED_ANSWER = 700L
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRimBinding.inflate(inflater)

        // вывод названия типа устройства
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.printDeviceTypeName(getString(R.string.rim))

            // убераем ат команды на всякий случай
            context.usb.flagAtCommandYesNo = false
        }

        binding.imageDownLoad.setOnClickListener {
            writeSettingStart()
        }

        // базовые настрйки
        binding.inputPasswordNow.setText("000000")
        binding.inputAddress.setText("14")
        binding.inputAddressNow.setText("14")
        binding.switchPasswordDefault.isChecked = true

        // если пароль по умолчанию то нечего не устанавливаем и убераем поле для воода пароля
        binding.switchPasswordDefault.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutInputPasswordNow.visibility = View.GONE
            } else {
                binding.layoutInputPasswordNow.visibility = View.VISIBLE
            }
        }

        createAdapters()

        binding.spinnerSpeedNow.setSelection(SEARCH_SPEED_INDEX)



        return binding.root
    }

    // при уничтожении
    override fun onDestroyView() {
        val context = (requireContext() as MainActivity)

        // сбрасываем настройки до стандартных
        context.usb.onSerialSpeed(9)
        context.usb.onSerialParity(0)

        super.onDestroyView()
    }

    private fun createAdapters() {


        // адаптер для выбора скорости
        val itemSelectSpeed = listOf(
            getString(R.string.speed_4800),
            getString(R.string.speed_9600),
            getString(R.string.speed_19200),
            getString(R.string.speed_38400),
            getString(R.string.speed_57600),
            getString(R.string.serchSpeed)
        )


        val itemSelectProtocol = listOf(
            getString(R.string.rs485),
            getString(R.string.modbas)
        )


        val adapterSelectSpeedNow = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed)
        val adapterSelectSpeed = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectSpeed.dropLast(1)) // dropLast т к поиск скорости ненужен в записывемых скоростях
        val adapterSelectProtocol = ArrayAdapter(requireContext(),
            R.layout.item_spinner, itemSelectProtocol)

        adapterSelectSpeedNow.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        adapterSelectSpeed.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        adapterSelectProtocol.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)



        binding.spinnerSpeed.adapter = adapterSelectSpeed
        binding.spinnerSpeedNow.adapter = adapterSelectSpeedNow

        binding.spinnerDataProtocolNow.adapter = adapterSelectProtocol
        binding.spinnerDataProtocol.adapter = adapterSelectProtocol
    }



    override fun printSettingDevice(settingMap: Map<String, String>) {
        showAlertDialog(settingMap.keys.toString())
        showAlertDialog(settingMap.values.toString())
    }


    override fun writeSettingStart() {
        // проверка на валидность
        if (!validAll()) return

        // проверяем выставлено ли значение поиска скаорости
        if (binding.spinnerSpeedNow.selectedItemPosition != SEARCH_SPEED_INDEX)
            (context as MainActivity).usb.onSerialSpeed(binding.spinnerSpeedNow.selectedItemPosition + 4)
        else {
            // посик скорости и выход потому что когда она найдется эта функция автоматически ее включет
            searchSpeed()
            return
        }



        val rimUsb = RimUsb((requireContext() as MainActivity), usbCommandsProtocol)


        val passwordByteSand =
            if (binding.switchPasswordDefault.isChecked)
                byteArrayOf(    // команда ввода пароля
                    0x14.toByte(),
                    0x08.toByte()
                ) + DEFAULT_PASSWORD
            else byteArrayOf(    // команда ввода пароля
                0x14.toByte(),
                0x08.toByte(),
                binding.inputPasswordNow.text.toString()[0].code.toByte(),
                binding.inputPasswordNow.text.toString()[1].code.toByte(),
                binding.inputPasswordNow.text.toString()[2].code.toByte(),
                binding.inputPasswordNow.text.toString()[3].code.toByte(),
                binding.inputPasswordNow.text.toString()[4].code.toByte(),
                binding.inputPasswordNow.text.toString()[5].code.toByte()
            )

        // байт протокола с которым будем ориться
        val protocolByte =
            if (binding.spinnerDataProtocol.selectedItem == getString(R.string.rs485)) 0x00.toByte()
            else 0x40.toByte()


        // если выбран rs485 то
        if (binding.spinnerDataProtocolNow.selectedItem.toString() == getString(R.string.rs485))
        {
            // поток для отправки данных
            binding.loadMenuProgress.visibility = View.VISIBLE
            binding.fonLoadMenu.visibility = View.VISIBLE

            // поток отправки для 485
            Thread {

                // разрешение на вывод данных т к идет общение
                flagPermissionShow = true

                // отправка по rs485
                val flagSucSendPassword = rimUsb.writeRS485(
                    binding.inputAddressNow.text.toString().toByte(),
                    passwordByteSand,
                    this
                )

                // только если пароль введен успешно
                if (flagSucSendPassword) {
                    rimUsb.writeRS485(
                        binding.inputAddressNow.text.toString().toByte(),
                        byteArrayOf(    // команда ввода настройки адреса
                            0x03.toByte(),
                            0x04.toByte(),
                            binding.inputAddress.text.toString().toInt().toByte(),
                            (binding.spinnerSpeed.selectedItemPosition.toByte() or protocolByte)
                        ),
                        this,
                        true
                    )
                }
            }.start()
        } else { // modbus
            binding.loadMenuProgress.visibility = View.VISIBLE
            binding.fonLoadMenu.visibility = View.VISIBLE

            // поток отправки для modbus
            Thread {

                // разрешение на вывод данных т к идет общение
                flagPermissionShow = true

                val flagSucSendPassword = rimUsb.writeModBus(
                    byteArrayOf(
                        // команда ввода пароля
                        binding.inputAddressNow.text.toString().toByte(),
                    ) + passwordByteSand,
                    this
                )

                // только если пароль введен успешно
                if (flagSucSendPassword) {
                    rimUsb.writeModBus(
                        byteArrayOf(    // команда ввода настройки адреса
                            binding.inputAddressNow.text.toString().toByte(),
                            0x03.toByte(),
                            0x04.toByte(),
                            binding.inputAddress.text.toString().toInt().toByte(),
                            (binding.spinnerSpeed.selectedItemPosition.toByte() or protocolByte)
                        ),
                        this,
                        true
                    )
                }
            }.start()
        }
    }

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

            //--------------------------------------------------------------------------------------

            binding.imageDownLoad.setOnClickListener {
                showAlertDialog(getString(R.string.Usb_NoneConnect))
            }
        } else {
            binding.imageDownLoad.setOnClickListener {
                writeSettingStart()
            }
        }
    }


    private fun searchSpeed() {
        val address: Byte = binding.inputAddressNow.text.toString().toByte()

        // отккрываем згрузочное меню
        binding.loadMenuProgress.visibility = View.VISIBLE
        binding.fonLoadMenu.visibility = View.VISIBLE
        binding.textLoadMenu.text = getString(R.string.serchSpeed)

        val context: MainActivity = (requireContext() as MainActivity)

        // если протокол 485 то
        if (binding.spinnerDataProtocolNow.selectedItem == getString(R.string.rs485)) {

            // поток поиска скорости при протоколе
            Thread {

                context.usb.onSerialParity(3) // переходим на четность PARITY_MARK
                Thread.sleep(500)

                var speedFind = -1;
                end@for (i in 0..4) {
                    for (p in 0..1) { // 2 попытки
                        // очищение буфера приема
                        context.curentDataByteAll = byteArrayOf()

                        context.usb.onSerialSpeed(i + 4) // +4 потому что скорости начинаются с 4800

                        // отправка адреса для того что бы посмотреть ответит ли кто то
                        context.usb.writeDevice("", false, byteArrayOf(address), false)

                        // ожидание ответа
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < MAX_TIME_WAIT_SEARCH_SPEED_ANSWER) {
                            if (context.curentDataByteAll.isNotEmpty()) {
                                speedFind = i
                                break@end
                            }
                        }
                    }
                }

                // если поиск прошел успешно
                context.runOnUiThread {
                    if (speedFind != -1) {
                        binding.spinnerSpeedNow.setSelection(speedFind)
                        writeSettingStart() // начинаем запись
                    } else {
                        // не успешно поэтому кидаем сообщение что скорость не удалось найти
                        showAlertDialog(getString(R.string.errorNotFindSpeedRim))

                        // закрытие меню
                        binding.loadMenuProgress.visibility = View.GONE
                        binding.fonLoadMenu.visibility = View.GONE
                    }

                    binding.textLoadMenu.text = getString(R.string.sand)
                }
            }.start()
        } else {

            val passwordByteSand = byteArrayOf(    // команда ввода пароля]
                    0x14.toByte(),
                    0x08.toByte(),
                    binding.inputPasswordNow.text.toString()[0].code.toByte(),
                    binding.inputPasswordNow.text.toString()[1].code.toByte(),
                    binding.inputPasswordNow.text.toString()[2].code.toByte(),
                    binding.inputPasswordNow.text.toString()[3].code.toByte(),
                    binding.inputPasswordNow.text.toString()[4].code.toByte(),
                    binding.inputPasswordNow.text.toString()[5].code.toByte()
                )
            Thread {

                context.usb.onSerialParity(3) // переходим на четность PARITY_NONE
                Thread.sleep(500)

                var speedFind = -1;
                end@for (i in 0..4) {
                    for (p in 0..1) { // 2 попытки
                        // очищение буфера приема
                        context.curentDataByteAll = byteArrayOf()

                        context.usb.onSerialSpeed(i + 4) // +4 потому что скорости начинаются с 4800

                        // отправка адреса для того что бы посмотреть ответит ли кто то
                        context.usb.writeDevice("", false, byteArrayOf(address) + passwordByteSand, true)

                        // ожидание ответа
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < MAX_TIME_WAIT_SEARCH_SPEED_ANSWER) {
                            if (context.curentDataByteAll.isNotEmpty()) {
                                speedFind = i
                                break@end
                            }
                        }
                    }
                }

                // если поиск прошел успешно
                context.runOnUiThread {
                    if (speedFind != -1) {
                        binding.spinnerSpeedNow.setSelection(speedFind)
                        writeSettingStart() // начинаем запись
                    } else {
                        // не успешно поэтому кидаем сообщение что скорость не удалось найти
                        showAlertDialog(getString(R.string.errorNotFindSpeedRim))

                        // закрытие меню
                        binding.loadMenuProgress.visibility = View.GONE
                        binding.fonLoadMenu.visibility = View.GONE
                    }

                    binding.textLoadMenu.text = getString(R.string.sand)
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


    private fun validAll(): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()

        // проверка адреса
        if (!validDataSettingsDevice.validCharStringCode(binding.inputAddressNow.text.toString()) ||
            (binding.inputAddress.text.toString().isNotEmpty() && !validDataSettingsDevice.validCharStringCode(binding.inputAddress.text.toString()))) {
            showAlertDialog(getString(R.string.errorValidAddressRim))
            return false
        }

        // проверка поролей
        if (!validDataSettingsDevice.validPassword(binding.inputPasswordNow.text.toString())) {
            showAlertDialog(getString(R.string.errorValidPasswordRim))
            return false
        }
        return true
    }

    // сюда придет ответ были ли принеты данные или нет
    override fun showData(data: String) {
        if (flagPermissionShow) {
            // закрываем диалоговое окно
            binding.loadMenuProgress.visibility = View.GONE
            binding.fonLoadMenu.visibility = View.GONE


            // вывоод результата отправки данных
            if (data == "yes")  {
                // помещяем из новых в тикущие для удобства
                binding.inputAddressNow.setText(binding.inputAddress.text.toString())
                binding.spinnerSpeedNow.setSelection(binding.spinnerSpeed.selectedItemPosition)
                binding.spinnerDataProtocolNow.setSelection(binding.spinnerDataProtocol.selectedItemPosition)

                showAlertDialog(getString(R.string.sucLoadRim))
            } else if (data == "error_password") {
                showAlertDialog(getString(R.string.errorPasswordRim))
            } else if (data == "error_settings") {
                showAlertDialog(getString(R.string.errorValidSettingsToDevice))
            } else
                showAlertDialog(getString(R.string.errorLoadRim))


            flagPermissionShow = false
        }
    }

    // ненужные переопределения
    override fun readSettingStart() {}
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}

}