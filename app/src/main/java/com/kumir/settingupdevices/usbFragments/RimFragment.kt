package com.kumir.settingupdevices.usbFragments

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.kumir.settingupdevices.DataShowInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.databinding.FragmentRimBinding
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.usb.RimUsb
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbFragment
import java.nio.ByteBuffer
import kotlin.experimental.or


class RimFragment : Fragment(), UsbFragment, DataShowInterface {

    private lateinit var binding: FragmentRimBinding
    override val usbCommandsProtocol = UsbCommandsProtocol()


    private var flagReadDevice = false


    // эта переменная создана для того что бы сдвигать в зависимости от протокола индексы в ответах потому что в modbus в начле добавляется адрес
    private var shift = 0

    companion object {
        private const val SIZE_PASSWORD: Int = 6
        val DEFAULT_PASSWORD = ByteArray(SIZE_PASSWORD) { 0x00.toByte() }

        private const val SEARCH_SPEED_INDEX: Int = 5;
        private const val MAX_TIME_WAIT_SEARCH_SPEED_ANSWER = 700L


        private const val SERIAL_NUMBER_SIZE_BYTE: Int = 7
            private const val SERIAL_VERSION_PROGRAMMING_BYTE: Int = 9
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

        // кнопка для вывода картинки помощи
        binding.imageButtonHelpAddress.setOnClickListener {
            binding.halpLayoutAddressRim.visibility = View.VISIBLE
        }

        // закрытие фото помощи
        binding.halpLayoutAddressRim.setOnClickListener {
            binding.halpLayoutAddressRim.visibility = View.GONE
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
            getString(R.string.modbus)
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

                shift = 0


                // отправка по rs485
                val flagSucSendPassword = rimUsb.writeRS485(
                    binding.inputAddressNow.text.toString().toByte(),
                    passwordByteSand,
                    this
                )

                // только если пароль введен успешно
                if (flagSucSendPassword) {
                    // дополнительно получаем версию и номер прошивки
                    // если устройство еще не прочитано
                    if (!flagReadDevice) {
                        rimUsb.writeRS485(
                            binding.inputAddressNow.text.toString().toByte(),
                            byteArrayOf(
                                0x00.toByte(),
                                0x02.toByte(),
                            ),
                            this,
                            true
                        )
                        if (rimUsb.writeRS485(
                                binding.inputAddressNow.text.toString().toByte(),
                                byteArrayOf(
                                    0x01.toByte(),
                                    0x02.toByte(),
                                ),
                                this,
                                true
                            )) {
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

                            // отправка еще 1 команды для того что бы прибор перешел на нужную скорость
                            rimUsb.writeRS485(
                                binding.inputAddressNow.text.toString().toByte(),
                                byteArrayOf(
                                    0x00.toByte(),
                                    0x02.toByte(),
                                ),
                                this,
                                false,
                                false
                            )
                        }
                    } else {
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

                        // отправка еще 1 команды для того что бы прибор перешел на нужную скорость
                        rimUsb.writeRS485(
                            binding.inputAddressNow.text.toString().toByte(),
                            byteArrayOf(
                                0x00.toByte(),
                                0x02.toByte(),
                            ),
                            this,
                            false,
                            false
                        )
                    }
                }

            }.start()
        } else { // modbus
            binding.loadMenuProgress.visibility = View.VISIBLE
            binding.fonLoadMenu.visibility = View.VISIBLE

            // поток отправки для modbus
            Thread {

                shift = 1


                val flagSucSendPassword = rimUsb.writeModBus(
                    byteArrayOf(
                        // команда ввода пароля
                        binding.inputAddressNow.text.toString().toByte(),
                    ) + passwordByteSand,
                    this
                )

                // только если пароль введен успешно
                if (flagSucSendPassword) {
                    // если устройство еще не прочитано
                    if (!flagReadDevice) {
                        // дополнительно получаем версию и номер прошивки
                        rimUsb.writeModBus(
                            byteArrayOf(
                                binding.inputAddressNow.text.toString().toByte(),
                                0x00.toByte(),
                                0x02.toByte(),
                            ),
                            this,
                            true
                        )

                        if (rimUsb.writeModBus(
                            byteArrayOf(
                                binding.inputAddressNow.text.toString().toByte(),
                                0x01.toByte(),
                                0x02.toByte(),
                            ),
                            this,
                            true
                        )) {
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

                            // отправка еще 1 команды для того что бы прибор перешел на нужную скорость
                            rimUsb.writeModBus(
                                byteArrayOf(
                                    binding.inputAddressNow.text.toString().toByte(),
                                    0x00.toByte(),
                                    0x02.toByte(),
                                ),
                                this,
                                false,
                                false
                            )
                        }
                    } else {
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

                        // отправка еще 1 команды для того что бы прибор перешел на нужную скорость
                        rimUsb.writeModBus(
                            byteArrayOf(
                                binding.inputAddressNow.text.toString().toByte(),
                                0x00.toByte(),
                                0x02.toByte(),
                            ),
                            this,
                            false,
                            false
                        )
                    }
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
            drawablImageDownLoad?.let {
                val wrappedDrawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrappedDrawable, Color.RED)
                binding.imageDownLoad.setImageDrawable(wrappedDrawable)
            }

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
                usbCommandsProtocol.flagWorkWrite = true

                context.usb.onSerialParity(3) // переходим на четность PARITY_MARK
                Thread.sleep(500)

                var speedFind = -1;
                end@for (i in 0..4) {
                    for (p in 0..1) { // 2 попытки
                        // очищение буфера приема
                        context.currentDataByteAll = byteArrayOf()

                        context.usb.onSerialSpeed(i + 4) // +4 потому что скорости начинаются с 4800

                        // отправка адреса для того что бы посмотреть ответит ли кто то
                        if (context.usb.checkConnectToDevice())
                            context.usb.writeDevice("", false, byteArrayOf(address), false)
                        else {
                            // закрытие меню
                            context.runOnUiThread {
                                binding.loadMenuProgress.visibility = View.GONE
                                binding.fonLoadMenu.visibility = View.GONE
                            }

                            usbCommandsProtocol.flagWorkWrite = false
                            return@Thread
                        }
                        // ожидание ответа
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < MAX_TIME_WAIT_SEARCH_SPEED_ANSWER) {
                            if (context.currentDataByteAll.isNotEmpty()) {
                                speedFind = i
                                break@end
                            }
                        }
                    }
                }

                usbCommandsProtocol.flagWorkWrite = false


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
                        context.currentDataByteAll = byteArrayOf()

                        context.usb.onSerialSpeed(i + 4) // +4 потому что скорости начинаются с 4800

                        // отправка адреса для того что бы посмотреть ответит ли кто то
                        if (context.usb.checkConnectToDevice())
                            context.usb.writeDevice("", false, byteArrayOf(address) + passwordByteSand, true)
                        else return@Thread
                        // ожидание ответа
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < MAX_TIME_WAIT_SEARCH_SPEED_ANSWER) {
                            if (context.currentDataByteAll.isNotEmpty()) {
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
    @OptIn(ExperimentalStdlibApi::class)
    override fun showData(data: String, dataByteArray: ByteArray?) {
        // пришли пустые данные или их нет то выходим
        if (dataByteArray == null) return

        // закрываем только через некоторое веремя
        Thread {
            Thread.sleep(2000)
            (requireContext() as Activity).runOnUiThread {
                // закрываем диалоговое окно
                binding.loadMenuProgress.visibility = View.GONE
                binding.fonLoadMenu.visibility = View.GONE
            }
        }.start()

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
        } else if (data == "version_programming") {
            //val context: MainActivity = (requireContext() as MainActivity)

            // проверка нужный ли размер у посылки
            if (dataByteArray.size == SERIAL_VERSION_PROGRAMMING_BYTE + shift) {

                /*if (dataByteArray[0] != binding.inputAddressNow)*/
                binding.textVersionFirmware.text = getString(R.string.versionProgram) + " " +
                        dataByteArray[3 + shift].toString()
            } else {
                showAlertDialog(getString(R.string.errorGetSerialNumberOrVErsion))
            }
        } else if (data == "serial_number") {
            // val context: MainActivity = (requireContext() as MainActivity)

            Log.d("DataNumber", "true")
            // проверка нужный ли размер у посылки
            if (dataByteArray.size == SERIAL_NUMBER_SIZE_BYTE + shift) {

                // преобразование в число
                val number: Int = ByteBuffer.wrap(
                    byteArrayOf(
                        0x00.toByte(),
                        dataByteArray[4 + shift],
                        dataByteArray[3 + shift],
                        dataByteArray[2 + shift]
                    )
                ).int
                Log.d("DataNumber", "number - $number")

                binding.serinerNumber.text = getString(R.string.serinerNumber) + "\n" + number.toString()

                // если все хорошо то выставляем флаг что наш маодем прочитан
                flagReadDevice = true

            } else {
                showAlertDialog(getString(R.string.errorGetSerialNumberOrVErsion))
            }
        } else
            showAlertDialog(getString(R.string.errorLoadRim))
    }

    // ненужные переопределения
    override fun readSettingStart() {}
    override fun printSerifalNumber(serialNumber: String) {}
    override fun printVersionProgram(versionProgram: String) {}

}