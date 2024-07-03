package com.example.kumirsettingupdevices

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kumirsettingupdevices.presetFragments.SelectMenuPrisetSettings
import com.example.kumirsettingupdevices.dataBasePreset.AppDatabase
import com.example.kumirsettingupdevices.dataBasePreset.Enfora
import com.example.kumirsettingupdevices.dataBasePreset.EnforaDao
import com.example.kumirsettingupdevices.dataBasePreset.Pm
import com.example.kumirsettingupdevices.dataBasePreset.PmDao
import com.example.kumirsettingupdevices.dataBasePreset.Preset
import com.example.kumirsettingupdevices.dataBasePreset.PresetDao
import com.example.kumirsettingupdevices.databinding.MainActivityBinding
import com.example.kumirsettingupdevices.diag.ACCB030DiagFragment
import com.example.kumirsettingupdevices.diag.DiagFragment
import com.example.kumirsettingupdevices.diag.DiagFragmentInterface
import com.example.kumirsettingupdevices.diag.DiagPM81Fragment
import com.example.kumirsettingupdevices.model.recyclerModel.Priset
import com.example.kumirsettingupdevices.ports.PortDeviceSetting
import com.example.kumirsettingupdevices.presetFragments.SelectMenuPrisetEnforaSettings
import com.example.kumirsettingupdevices.presetFragments.SelectMenuPrisetPmSettings
import com.example.kumirsettingupdevices.settings.DeviceAccountingPrisets
import com.example.kumirsettingupdevices.settings.PresetsEnforaValue
import com.example.kumirsettingupdevices.settings.PrisetsPmValue
import com.example.kumirsettingupdevices.settings.PrisetsValue
import com.example.kumirsettingupdevices.usb.Usb
import com.example.kumirsettingupdevices.usb.UsbActivityInterface
import com.example.kumirsettingupdevices.usb.UsbFragment
import com.example.kumirsettingupdevices.usbFragments.ACCB030CoreFragment
import com.example.kumirsettingupdevices.usbFragments.ACCB030Fragment
import com.example.kumirsettingupdevices.diag.Enfora1318DiagFragment
import com.example.kumirsettingupdevices.usb.UsbCommandsProtocol
import com.example.kumirsettingupdevices.usb.UsbDeviceDescriptor
import com.example.kumirsettingupdevices.usbFragments.Enfora1318Fragment
import com.example.kumirsettingupdevices.usbFragments.K21K23Fragment
import com.example.kumirsettingupdevices.usbFragments.M31Fragment
import com.example.kumirsettingupdevices.usbFragments.M32Fragment
import com.example.kumirsettingupdevices.usbFragments.M32LiteFragment
import com.example.kumirsettingupdevices.usbFragments.P101Fragment
import com.example.kumirsettingupdevices.usbFragments.PM81Fragment
import com.example.kumirsettingupdevices.usbFragments.PrisetFragment
import com.example.testappusb.settings.ConstUsbSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), UsbActivityInterface {

    override val usb: Usb = Usb(this)
    private lateinit var binding: MainActivityBinding

    private var usbComsMenu: UsbComsMenu? = null
    private var selectMenuPrisetSettings: SelectMenuPrisetSettings? = null
    private var selectMenuPrisetEnforaSettings: SelectMenuPrisetEnforaSettings? = null
    private var selectMenuPrisetPmSettings: SelectMenuPrisetPmSettings? = null

    // буферы денных
    var curentData: String = ""
    var curentDataByte: ByteArray = byteArrayOf()

    // flag Для контроля передачи информации
    var flagThreadSerialCommands: Boolean = false

    // база данных
    lateinit var presetDao: PresetDao
    lateinit var presetEnforaDao: EnforaDao
    lateinit var presetPmDao: PmDao

    // список с устройств с котрыми можно работать
    private val devicesTypsAll: String =
        "KUMIR-К21К23 READY" +
        "KUMIR-M32 READY" +
        "KUMIR-M32LITE READY" +
        "KUMIR-RM81A READY"

    // текущий фрагмент
    var curentFragmentComProtocol: UsbCommandsProtocol? = null


    companion object {
        const val TIMEOUT_TOWAIT_RESTART_DEVICE: Int = 29 // 30 - 1 секудны
        const val NORM_LENGHT_DATA_START = 5

    }

    // фильтер для устройств
    private val usbDevices = setOf(
        UsbDeviceDescriptor(1027, 24577), // 0x0403 / 0x6001: FT232R
        UsbDeviceDescriptor(1027, 24592), // 0x0403 / 0x6010: FT2232H
        UsbDeviceDescriptor(1027, 24593), // 0x0403 / 0x6011: FT4232H
        UsbDeviceDescriptor(1027, 24596), // 0x0403 / 0x6014: FT232H
        UsbDeviceDescriptor(1027, 24597), // 0x0403 / 0x6015: FT230X, FT231X, FT234XD
        UsbDeviceDescriptor(4292, 60000), // 0x10C4 / 0xEA60: CP2102 and other CP210x single port devices
        UsbDeviceDescriptor(4292, 60016), // 0x10C4 / 0xEA70: CP2105
        UsbDeviceDescriptor(4292, 60017), // 0x10C4 / 0xEA71: CP2108
        UsbDeviceDescriptor(1659, 8963),  // 0x067B / 0x2303: PL2303HX, HXD, TA, ...
        UsbDeviceDescriptor(1659, 9123),  // 0x067B / 0x23A3: PL2303GC
        UsbDeviceDescriptor(1659, 9139),  // 0x067B / 0x23B3: PL2303GB
        UsbDeviceDescriptor(1659, 9155),  // 0x067B / 0x23C3: PL2303GT
        UsbDeviceDescriptor(1659, 9171),  // 0x067B / 0x23D3: PL2303GL
        UsbDeviceDescriptor(1659, 9187),  // 0x067B / 0x23E3: PL2303GE
        UsbDeviceDescriptor(1659, 9203),  // 0x067B / 0x23F3: PL2303GS
        UsbDeviceDescriptor(6790, 21795), // 0x1A86 / 0x5523: CH341A
        UsbDeviceDescriptor(6790, 29987), // 0x1A86 / 0x7523: CH340
        UsbDeviceDescriptor(9025, null),  // 0x2341 / ......: Arduino
        UsbDeviceDescriptor(5824, 1155),  // 0x16C0 / 0x0483: Teensyduino
        UsbDeviceDescriptor(1003, 8260),  // 0x03EB / 0x2044: Atmel Lufa
        UsbDeviceDescriptor(7855, 4),     // 0x1EAF / 0x0004: Leaflabs Maple
        UsbDeviceDescriptor(3368, 516),   // 0x0D28 / 0x0204: ARM mbed
        UsbDeviceDescriptor(1155, 22336), // 0x0483 / 0x5740: ST CDC
        UsbDeviceDescriptor(11914, 5),    // 0x2E8A / 0x0005: Raspberry Pi Pico Micropython
        UsbDeviceDescriptor(11914, 10),   // 0x2E8A / 0x000A: Raspberry Pi Pico SDK
        UsbDeviceDescriptor(6790, 21972)  // 0x1A86 / 0x55D4: Qinheng CH9102F
    )

    // устройства прибора учета
    val portsDeviceSetting: List<PortDeviceSetting> = listOf(
        PortDeviceSetting(DeviceAccountingPrisets.SPT941Speed, DeviceAccountingPrisets.SPT941BitData, DeviceAccountingPrisets.SPT941StopBit, DeviceAccountingPrisets.SPT941Parity, DeviceAccountingPrisets.SPT941Priset),
        PortDeviceSetting(DeviceAccountingPrisets.SPT944Speed, DeviceAccountingPrisets.SPT944BitData, DeviceAccountingPrisets.SPT944StopBit, DeviceAccountingPrisets.SPT944Parity, DeviceAccountingPrisets.SPT944Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TSP025Speed, DeviceAccountingPrisets.TSP025BitData, DeviceAccountingPrisets.TSP025StopBit, DeviceAccountingPrisets.TSP025Parity, DeviceAccountingPrisets.TSP025Priset),
        PortDeviceSetting(DeviceAccountingPrisets.PSCH4TMV23Speed, DeviceAccountingPrisets.PSCH4TMV23BitData, DeviceAccountingPrisets.PSCH4TMV23StopBit, DeviceAccountingPrisets.PSCH4TMV23Parity, DeviceAccountingPrisets.PSCH4TMV23Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TSP027Speed, DeviceAccountingPrisets.TSP027BitData, DeviceAccountingPrisets.TSP027StopBit, DeviceAccountingPrisets.TSP027Parity, DeviceAccountingPrisets.TSP027Priset),
        PortDeviceSetting(DeviceAccountingPrisets.PowerCE102MSpeed, DeviceAccountingPrisets.PowerCE102MBitData, DeviceAccountingPrisets.PowerCE102MStopBit, DeviceAccountingPrisets.PowerCE102MParity, DeviceAccountingPrisets.PowerCE102MPriset),
        PortDeviceSetting(DeviceAccountingPrisets.Mercury206Speed, DeviceAccountingPrisets.Mercury206BitData, DeviceAccountingPrisets.Mercury206StopBit, DeviceAccountingPrisets.Mercury206Parity, DeviceAccountingPrisets.Mercury206Priset),
        PortDeviceSetting(DeviceAccountingPrisets.KM5PM5Speed, DeviceAccountingPrisets.KM5PM5BitData, DeviceAccountingPrisets.KM5PM5StopBit, DeviceAccountingPrisets.KM5PM5Parity, DeviceAccountingPrisets.KM5PM5Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TEM104Speed, DeviceAccountingPrisets.TEM104BitData, DeviceAccountingPrisets.TEM104StopBit, DeviceAccountingPrisets.TEM104Parity, DeviceAccountingPrisets.TEM104Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TEM106Speed, DeviceAccountingPrisets.TEM106BitData, DeviceAccountingPrisets.TEM106StopBit, DeviceAccountingPrisets.TEM106Parity, DeviceAccountingPrisets.TEM106Priset),
        PortDeviceSetting(DeviceAccountingPrisets.BKT5Speed, DeviceAccountingPrisets.BKT5BitData, DeviceAccountingPrisets.BKT5StopBit, DeviceAccountingPrisets.BKT5Parity, DeviceAccountingPrisets.BKT5Priset),
        PortDeviceSetting(DeviceAccountingPrisets.BKT7Speed, DeviceAccountingPrisets.BKT7BitData, DeviceAccountingPrisets.BKT7StopBit, DeviceAccountingPrisets.BKT7Parity, DeviceAccountingPrisets.BKT7Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TePOCCSpeed, DeviceAccountingPrisets.TePOCCBitData, DeviceAccountingPrisets.TePOCCStopBit, DeviceAccountingPrisets.TePOCCParity, DeviceAccountingPrisets.TePOCCPriset),
        PortDeviceSetting(DeviceAccountingPrisets.SPT942Speed, DeviceAccountingPrisets.SPT942BitData, DeviceAccountingPrisets.SPT942StopBit, DeviceAccountingPrisets.SPT942Parity, DeviceAccountingPrisets.SPT942Priset),
        PortDeviceSetting(DeviceAccountingPrisets.SPT943Speed, DeviceAccountingPrisets.SPT943BitData, DeviceAccountingPrisets.SPT943StopBit, DeviceAccountingPrisets.SPT943Parity, DeviceAccountingPrisets.SPT943Priset),
        PortDeviceSetting(DeviceAccountingPrisets.SPT961Speed, DeviceAccountingPrisets.SPT961BitData, DeviceAccountingPrisets.SPT961StopBit, DeviceAccountingPrisets.SPT961Parity, DeviceAccountingPrisets.SPT961Priset),
        PortDeviceSetting(DeviceAccountingPrisets.KT7AbacanSpeed, DeviceAccountingPrisets.KT7AbacanBitData, DeviceAccountingPrisets.KT7AbacanStopBit, DeviceAccountingPrisets.KT7AbacanParity, DeviceAccountingPrisets.KT7AbacanPriset),
        PortDeviceSetting(DeviceAccountingPrisets.MT200DSSpeed, DeviceAccountingPrisets.MT200DSBitData, DeviceAccountingPrisets.MT200DSStopBit, DeviceAccountingPrisets.MT200DSParity, DeviceAccountingPrisets.MT200DSPriset),
        PortDeviceSetting(DeviceAccountingPrisets.TCP010Speed, DeviceAccountingPrisets.TCP010BitData, DeviceAccountingPrisets.TCP010StopBit, DeviceAccountingPrisets.TCP010Parity, DeviceAccountingPrisets.TCP010Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TCP010MSpeed, DeviceAccountingPrisets.TCP010MBitData, DeviceAccountingPrisets.TCP010MStopBit, DeviceAccountingPrisets.TCP010MParity, DeviceAccountingPrisets.TCP010MPriset),
        PortDeviceSetting(DeviceAccountingPrisets.TCP023Speed, DeviceAccountingPrisets.TCP023BitData, DeviceAccountingPrisets.TCP023StopBit, DeviceAccountingPrisets.TCP023Parity, DeviceAccountingPrisets.TCP023Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TCPB024Speed, DeviceAccountingPrisets.TCPB024BitData, DeviceAccountingPrisets.TCPB024StopBit, DeviceAccountingPrisets.TCPB024Parity, DeviceAccountingPrisets.TCPB024Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TCP026Speed, DeviceAccountingPrisets.TCP026BitData, DeviceAccountingPrisets.TCP026StopBit, DeviceAccountingPrisets.TCP026Parity, DeviceAccountingPrisets.TCP026Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TCPB03XSpeed, DeviceAccountingPrisets.TCPB03XBitData, DeviceAccountingPrisets.TCPB03XStopBit, DeviceAccountingPrisets.TCPB03XParity, DeviceAccountingPrisets.TCPB03XPriset),
        PortDeviceSetting(DeviceAccountingPrisets.TCPB042Speed, DeviceAccountingPrisets.TCPB042BitData, DeviceAccountingPrisets.TCPB042StopBit, DeviceAccountingPrisets.TCPB042Parity, DeviceAccountingPrisets.TCPB042Priset),
        PortDeviceSetting(DeviceAccountingPrisets.YCPB5XXSpeed, DeviceAccountingPrisets.YCPB5XXBitData, DeviceAccountingPrisets.YCPB5XXStopBit, DeviceAccountingPrisets.YCPB5XXParity, DeviceAccountingPrisets.YCPB5XXPriset),
        PortDeviceSetting(DeviceAccountingPrisets.PCL212Speed, DeviceAccountingPrisets.PCL212BitData, DeviceAccountingPrisets.PCL212StopBit, DeviceAccountingPrisets.PCL212Parity, DeviceAccountingPrisets.PCL212Priset),
        PortDeviceSetting(DeviceAccountingPrisets.SA942MSpeed, DeviceAccountingPrisets.SA942MBitData, DeviceAccountingPrisets.SA942MStopBit, DeviceAccountingPrisets.SA942MParity, DeviceAccountingPrisets.SA942MPriset),
        PortDeviceSetting(DeviceAccountingPrisets.SA943Speed, DeviceAccountingPrisets.SA943BitData, DeviceAccountingPrisets.SA943StopBit, DeviceAccountingPrisets.SA943Parity, DeviceAccountingPrisets.SA943Priset),
        PortDeviceSetting(DeviceAccountingPrisets.MKTCSpeed, DeviceAccountingPrisets.MKTCBitData, DeviceAccountingPrisets.MKTCStopBit, DeviceAccountingPrisets.MKTCParity, DeviceAccountingPrisets.MKTCPriset),
        PortDeviceSetting(DeviceAccountingPrisets.CKM2Speed, DeviceAccountingPrisets.CKM2BitData, DeviceAccountingPrisets.CKM2StopBit, DeviceAccountingPrisets.CKM2Parity, DeviceAccountingPrisets.CKM2Priset),
        PortDeviceSetting(DeviceAccountingPrisets.DYMETIC5102Speed, DeviceAccountingPrisets.DYMETIC5102BitData, DeviceAccountingPrisets.DYMETIC5102StopBit, DeviceAccountingPrisets.DYMETIC5102Parity, DeviceAccountingPrisets.DYMETIC5102Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TEPLOVACHESLITELTB7Speed, DeviceAccountingPrisets.TEPLOVACHESLITELTB7BitData, DeviceAccountingPrisets.TEPLOVACHESLITELTB7StopBit, DeviceAccountingPrisets.TEPLOVACHESLITELTB7Parity, DeviceAccountingPrisets.TEPLOVACHESLITELTB7Priset),
        PortDeviceSetting(DeviceAccountingPrisets.ELFSpeed, DeviceAccountingPrisets.ELFBitData, DeviceAccountingPrisets.ELFStopBit, DeviceAccountingPrisets.ELFParity, DeviceAccountingPrisets.ELFPriset),
        PortDeviceSetting(DeviceAccountingPrisets.STU1Speed, DeviceAccountingPrisets.STU1BitData, DeviceAccountingPrisets.STU1StopBit, DeviceAccountingPrisets.STU1Parity, DeviceAccountingPrisets.STU1Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TURBOFLOUGFGFSpeed, DeviceAccountingPrisets.TURBOFLOUGFGFBitData, DeviceAccountingPrisets.TURBOFLOUGFGFStopBit, DeviceAccountingPrisets.TURBOFLOUGFGFParity, DeviceAccountingPrisets.TURBOFLOUGFGFPriset),
        PortDeviceSetting(DeviceAccountingPrisets.EK260Speed, DeviceAccountingPrisets.EK260BitData, DeviceAccountingPrisets.EK260StopBit, DeviceAccountingPrisets.EK260Parity, DeviceAccountingPrisets.EK260Priset),
        PortDeviceSetting(DeviceAccountingPrisets.EK270Speed, DeviceAccountingPrisets.EK270BitData, DeviceAccountingPrisets.EK270StopBit, DeviceAccountingPrisets.EK270Parity, DeviceAccountingPrisets.EK270Priset),
        PortDeviceSetting(DeviceAccountingPrisets.BKG2Speed, DeviceAccountingPrisets.BKG2BitData, DeviceAccountingPrisets.BKG2StopBit, DeviceAccountingPrisets.BKG2Parity, DeviceAccountingPrisets.BKG2Priset),
        PortDeviceSetting(DeviceAccountingPrisets.CPG741Speed, DeviceAccountingPrisets.CPG741BitData, DeviceAccountingPrisets.CPG741StopBit, DeviceAccountingPrisets.CPG741Parity, DeviceAccountingPrisets.CPG741Priset),
        PortDeviceSetting(DeviceAccountingPrisets.CPG742Speed, DeviceAccountingPrisets.CPG742BitData, DeviceAccountingPrisets.CPG742StopBit, DeviceAccountingPrisets.CPG742Parity, DeviceAccountingPrisets.CPG742Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TC2015Speed, DeviceAccountingPrisets.TC2015BitData, DeviceAccountingPrisets.TC2015StopBit, DeviceAccountingPrisets.TC2015Parity, DeviceAccountingPrisets.TC2015Priset),
        PortDeviceSetting(DeviceAccountingPrisets.MERCURI230ARTSpeed, DeviceAccountingPrisets.MERCURI230ARTBitData, DeviceAccountingPrisets.MERCURI230ARTStopBit, DeviceAccountingPrisets.MERCURI230ARTParity, DeviceAccountingPrisets.MERCURI230ARTPriset),
        PortDeviceSetting(DeviceAccountingPrisets.PULSAR2MSpeed, DeviceAccountingPrisets.PULSAR2MBitData, DeviceAccountingPrisets.PULSAR2MStopBit, DeviceAccountingPrisets.PULSAR2MParity, DeviceAccountingPrisets.PULSAR2MPriset),
        PortDeviceSetting(DeviceAccountingPrisets.PULSAR10MSpeed, DeviceAccountingPrisets.PULSAR10MBitData, DeviceAccountingPrisets.PULSAR10MStopBit, DeviceAccountingPrisets.PULSAR10MParity, DeviceAccountingPrisets.PULSAR10MPriset),
        PortDeviceSetting(DeviceAccountingPrisets.KUMIRK21K22Speed, DeviceAccountingPrisets.KUMIRK21K22BitData, DeviceAccountingPrisets.KUMIRK21K22StopBit, DeviceAccountingPrisets.KUMIRK21K22Parity, DeviceAccountingPrisets.KUMIRK21K22Priset),
        PortDeviceSetting(DeviceAccountingPrisets.IM2300Speed, DeviceAccountingPrisets.IM2300BitData, DeviceAccountingPrisets.IM2300StopBit, DeviceAccountingPrisets.IM2300Parity, DeviceAccountingPrisets.IM2300Priset),
        PortDeviceSetting(DeviceAccountingPrisets.ENERGOMERACE303Speed, DeviceAccountingPrisets.ENERGOMERACE303BitData, DeviceAccountingPrisets.ENERGOMERACE303StopBit, DeviceAccountingPrisets.ENERGOMERACE303Parity, DeviceAccountingPrisets.ENERGOMERACE303Priset),
        PortDeviceSetting(DeviceAccountingPrisets.TEM116Speed, DeviceAccountingPrisets.TEM116BitData, DeviceAccountingPrisets.TEM116StopBit, DeviceAccountingPrisets.TEM116Parity, DeviceAccountingPrisets.TEM116Priset)
    )




    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            super.onCreate(savedInstanceState)

            binding = MainActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val mainFragment = MainFragment()

            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            // Новый фрагент
            transaction.replace(binding.fragmentContainerMainContent.id, mainFragment)
            //transaction.addToBackStack("MainFragment")
            transaction.commit()

            // загрузка базы данных
            val database = AppDatabase.getDatabase(this)
            presetDao = database.presetDao()
            presetEnforaDao = database.enforaDao()
            presetPmDao = database.pmDao()

            // загрузка всех присетов из базы данных
            try {
                lifecycleScope.launch {
                    // присеты m32
                    presetDao.getAll().collect { presets ->
                        for (preset in presets) {
                            PrisetsValue.prisets[preset.name!!] = Priset(
                                preset.name, preset.mode!!, preset.apn!!,
                                preset.port!!, preset.server!!, preset.login!!, preset.password!!
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }

            // загрузка всех присетов enfora
            try {
                lifecycleScope.launch {
                    // присеты enfora
                    presetEnforaDao.getAll().collect { presets ->
                        for (enforaPreseet in presets) {
                            PresetsEnforaValue.presets[enforaPreseet.name!!] =
                                Enfora(
                                    0, enforaPreseet.name, enforaPreseet.apn!!,
                                    enforaPreseet.login!!, enforaPreseet.password!!,
                                    enforaPreseet.server1!!, enforaPreseet.server2!!,
                                    enforaPreseet.timeout!!, enforaPreseet.sizeBuffer!!
                                )
                        }
                    }
                }
            } catch (_: Exception) {
            }

            // загрузка всех присетов Pm
            try {
                lifecycleScope.launch {
                    // присеты enfora
                    presetPmDao.getAll().collect { presets ->
                        for (PmPreset in presets) {
                            PrisetsPmValue.presets[PmPreset.name!!] =
                                Pm(
                                    0, PmPreset.name, PmPreset.mode, PmPreset.keyNet!!,
                                    PmPreset.power!!, PmPreset.diopozone
                                )
                        }
                    }
                }
            } catch (_: Exception) {
            }


            // смена настроек usb ---------------------------------------------------
            ConstUsbSettings.speedIndex = 9 // скорость 115200

            // usb.flagAtCommandYesNo = true


            // в случчае если девай подключен к usb то сразц подключиться к нему
            val intent = intent
            val usbDevice: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            usbDevice?.let {
                connectToUsbDevice(usbDevice)
            }
        }

    }

    override fun onDestroy() {
        usb.onDestroy()
        super.onDestroy()
    }

    // метод для кнопки назад
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {

        if (binding.drawerMenuSelectTypeDevice.isDrawerOpen(GravityCompat.START)) {
            binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        } else if (binding.fonMenu.isVisible) {
            workFonDarkMenu()

        }/* else if (supportFragmentManager.getBackStackEntryAt(0).name != "MainFragment") {

        } */else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.mainActivityExitCheck))

            builder.setPositiveButton(getString(R.string.Yes)) { dialog, _ ->
                dialog.dismiss()
                for (i in 0..supportFragmentManager.fragments.size) {
                    super.onBackPressed()
                    finishAffinity()
                }
            }
            builder.setNegativeButton(getString(R.string.No)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    // вункция для вывода имени устроства
    fun printDeviceTypeName(name: String) {
        binding.textNameDevice.text = name
    }

    // клик по кнопке выбора присета настроек
    fun onClickPrisetSettingFor(fragment: PrisetFragment<Priset>) {
        selectMenuPrisetSettings = SelectMenuPrisetSettings(fragment)

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Новый фрагент
        transaction.replace(binding.fragmentSelectPrisetSettings.id, selectMenuPrisetSettings!!)
        //transaction.addToBackStack("UsbComsMenu")
        transaction.commit()

        ActivationFonDarkMenu(true)
    }

    // клик по кнопке выбора присета настроек enfora
    fun onClickPrisetEnforaSettingFor(fragment: PrisetFragment<Enfora>) {
        selectMenuPrisetEnforaSettings = SelectMenuPrisetEnforaSettings(fragment)

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Новый фрагент
        transaction.replace(binding.fragmentSelectPrisetSettings.id, selectMenuPrisetEnforaSettings!!)
        //transaction.addToBackStack("UsbComsMenu")
        transaction.commit()

        ActivationFonDarkMenu(true)
    }

    // клик по кнопке выбора присета настроек pm
    fun onClickPrisetPmSettingFor(fragment: PrisetFragment<Pm>) {
        selectMenuPrisetPmSettings = SelectMenuPrisetPmSettings(fragment)

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Новый фрагент
        transaction.replace(binding.fragmentSelectPrisetSettings.id, selectMenuPrisetPmSettings!!)
        //transaction.addToBackStack("UsbComsMenu")
        transaction.commit()

        ActivationFonDarkMenu(true)
    }

    // клик конпки настроек
    fun onClickSettings(view: View) {
        binding.drawerMenuSelectTypeDevice.openDrawer(GravityCompat.START)
    }

    // клик конпки выбора usb
    fun onClickUsbComs(view: View) {

        usbComsMenu = UsbComsMenu()

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Новый фрагент
        transaction.replace(binding.fragmentContainerUsbMenu.id, usbComsMenu!!)
        //transaction.addToBackStack("UsbComsMenu")
        transaction.commit()

        ActivationFonDarkMenu(true)
    }

    fun onClickFonLockUses(view: View) {
        showAlertDialog(getString(R.string.nonConnectAdapter))
    }




    // ----------------клики в раскрывающимся меню-------------------

    // раскрывающаяся понель
    fun onClickEnforma1318(view: View) {
        if (binding.Enforma1318Settings.visibility == View.GONE) {
            binding.Enforma1318Settings.visibility = View.VISIBLE
            binding.Enforma1318Diag.visibility = View.VISIBLE
            binding.imageEnfora1318MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.top_arrow_5_svgrepo_com)
            )
        } else {
            binding.Enforma1318Settings.visibility = View.GONE
            binding.Enforma1318Diag.visibility = View.GONE
            binding.imageEnfora1318MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.down_2_svgrepo_com__1_)
            )
        }
    }
    fun onClickEnforma1318Settings(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val enforma1318 = Enfora1318Fragment()
        createSettingFragment(enforma1318)
    }
    fun onClickEnforma1318Diag(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val enforma1318Diag = Enfora1318DiagFragment()
        createSettingFragment(enforma1318Diag)
    }


    fun onClickM31(view: View) {
        if (binding.M31Settings.visibility == View.GONE) {
            binding.M31Settings.visibility = View.VISIBLE
            binding.M31Diag.visibility = View.VISIBLE
            binding.imageM31MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.top_arrow_5_svgrepo_com)
            )
        } else {
            binding.M31Settings.visibility = View.GONE
            binding.M31Diag.visibility = View.GONE
            binding.imageM31MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.down_2_svgrepo_com__1_)
            )
        }
    }
    fun onClickM31Settings(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m31 = M31Fragment()
        createSettingFragment(m31)
    }
    fun onClickM31Diag(view: View) {
        // диагностика такая же как в enfora
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val enforma1318Diag = Enfora1318DiagFragment()
        createSettingFragment(enforma1318Diag)
    }

    fun onClickK21K23(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val k21k23 = K21K23Fragment()
        createSettingFragment(k21k23)
    }

    // раскрывающаяся понель
    fun onClickM32(view: View) {
        if (binding.M32Settings.visibility == View.GONE) {
            binding.M32Settings.visibility = View.VISIBLE
            binding.M32Diag.visibility = View.VISIBLE
            binding.imageM32MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.top_arrow_5_svgrepo_com)
            )
        } else {
            binding.M32Settings.visibility = View.GONE
            binding.M32Diag.visibility = View.GONE
            binding.imageM32MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.down_2_svgrepo_com__1_)
            )
        }
    }
    fun onClickM32Settings(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32 = M32Fragment()
        createSettingFragment(m32)
    }
    fun onClickM32Diag(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32Diag = DiagFragment("KUMIR-M32 READY")
        createSettingFragment(m32Diag)
    }


    /*fun onClickDiag(serialNumber: String, programVersion: String) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val diag = DiagFragment(serialNumber, programVersion)
        createSettingFragment(diag)
    }*/
    fun onClickM32Lite(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32Lite = M32LiteFragment()
        createSettingFragment(m32Lite)
    }

    fun onClickPM81(view: View) {
        if (binding.PM81Settings.visibility == View.GONE) {
            binding.PM81Settings.visibility = View.VISIBLE
            binding.PM81Diag.visibility = View.VISIBLE
            binding.imagePM81MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.top_arrow_5_svgrepo_com)
            )
        } else {
            binding.PM81Settings.visibility = View.GONE
            binding.PM81Diag.visibility = View.GONE
            binding.imagePM81MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.down_2_svgrepo_com__1_)
            )
        }
    }
    fun onClickPM81Settings(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val pm81 = PM81Fragment()
        createSettingFragment(pm81)
    }
    fun onClickPM81Diag(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val pm81Diag = DiagPM81Fragment("KUMIR-RM81A READY")
        createSettingFragment(pm81Diag)
    }


    fun onClickACCB030Core(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val accbo30Core = ACCB030CoreFragment()
        createSettingFragment(accbo30Core)
    }

    // раскрывающаяся понель
    fun onClickACCB030(view: View) {
        if (binding.ACCB030FirmwareSettings.visibility == View.GONE) {
            binding.ACCB030FirmwareSettings.visibility = View.VISIBLE
            binding.ACCB030FirmwareDiag.visibility = View.VISIBLE
            binding.imageACCB00MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.top_arrow_5_svgrepo_com)
            )
        } else {
            binding.ACCB030FirmwareSettings.visibility = View.GONE
            binding.ACCB030FirmwareDiag.visibility = View.GONE
            binding.imageACCB00MenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.down_2_svgrepo_com__1_)
            )
        }
    }
    fun onClickACCB030Settings(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val accbo30 = ACCB030Fragment()
        createSettingFragment(accbo30)
    }
    fun onClickACCB030Diag(view: View) {

        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val accbo30Diag = ACCB030DiagFragment()
        createSettingFragment(accbo30Diag)
    }


    fun onClickP101(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val p101 = P101Fragment()
        createSettingFragment(p101)

    }

    fun onSettings(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        binding.textNameDevice.text = getString(R.string.settingsTitle)

        val settings = SettingsFragment()
        createSettingFragment(settings, true)

    }

    // сохрание настроек присета в базу данных
    fun onClickSavePreset(
        name: String,
        mode: Int,
        apn: String,
        server: String,
        port: String,
        login: String,
        password: String
    ) {
        if (name.isNotEmpty() && apn.isNotEmpty() && apn.isNotEmpty() && server.isNotEmpty() && port.isNotEmpty())
        {
            val preset = Preset(0, name, mode, apn, server, port, login, password)

            // Вставляем данные в базу данных
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {

                    try {
                        presetDao.insert(preset)

                        // сразу добовлеям что бы он стал активным и с ним можно было работать
                        PrisetsValue.prisets[name] = Priset(name, mode, apn, port, server, login, password)

                        runOnUiThread {
                            showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            showAlertDialog(getString(R.string.errorDataBase))
                        }
                    }
                }
            }
        } else {
            showAlertDialog(getString(R.string.nonEmptyData))
        }
    }

    // сохрание настроек присета в базу данных для enfora
    fun onClickSavePreset(
        name: String,
        apn: String,
        server1: String,
        server2: String,
        login: String,
        password: String,
        timeout: String,
        sizeBuffer: String
    ) {
        if (name.isNotEmpty() && apn.isNotEmpty() && apn.isNotEmpty() && server1.isNotEmpty() && timeout.isNotEmpty()
            && sizeBuffer.isNotEmpty())
        {
            val preset = Enfora(0, name, apn, login, password, server1, server2, timeout, sizeBuffer)

            // Вставляем данные в базу данных
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {

                    try {
                        presetEnforaDao.insert(preset)

                        // сразу добовлеям что бы он стал активным и с ним можно было работать
                        PresetsEnforaValue.presets[name] = Enfora(0, name, apn, login, password,
                            server1, server2, timeout, sizeBuffer)

                        runOnUiThread {
                            showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            showAlertDialog(getString(R.string.errorDataBase))
                        }
                    }
                }
            }
        } else {
            showAlertDialog(getString(R.string.nonEmptyData))
        }
    }

    // сохрание настроек присета в базу данных Pm
    fun onClickSavePreset(
        name: String,
        mode: Int,
        keyNet: String,
        power: String,
        range: Int) {
        if (name.isNotEmpty() && keyNet.isNotEmpty() && power.isNotEmpty())
        {
            val preset = Pm(0, name, mode, keyNet, power, range)

            // Вставляем данные в базу данных
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {

                    try {
                        presetPmDao.insert(preset)

                        // сразу добовлеям что бы он стал активным и с ним можно было работать
                        PrisetsPmValue.presets[name] = Pm(0, name, mode, keyNet, power, range)

                        runOnUiThread {
                            showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            showAlertDialog(getString(R.string.errorDataBase))
                        }
                    }
                }
            }
        } else {
            showAlertDialog(getString(R.string.nonEmptyData))
        }
    }

    private fun createSettingFragment(fragment: Fragment, flagChack: Boolean = false) {
        if (usb.checkConnectToDevice() || flagChack) {

            if (curentFragmentComProtocol != null) {
                if (curentFragmentComProtocol?.flagWorkDiag!! ||
                    curentFragmentComProtocol?.flagWorkChackSignal!! ||
                    curentFragmentComProtocol?.flagWorkDiagPm!! ||
                    curentFragmentComProtocol?.flagWorkWrite!! ||
                    curentFragmentComProtocol?.flagWorkRead!!) {
                    showAlertDialog(getString(R.string.readActivThread))
                } else {
                    val fragmentManager = supportFragmentManager
                    val transaction = fragmentManager.beginTransaction()

                    // Новый фрагент
                    transaction.replace(binding.fragmentContainerMainContent.id, fragment)
                    //transaction.addToBackStack("SettingDeviceFragment")
                    transaction.commit()

                    if (fragment is UsbFragment) {
                        curentFragmentComProtocol = fragment.usbCommandsProtocol
                    }
                }
            } else {
                val fragmentManager = supportFragmentManager
                val transaction = fragmentManager.beginTransaction()

                // Новый фрагент
                transaction.replace(binding.fragmentContainerMainContent.id, fragment)
                //transaction.addToBackStack("SettingDeviceFragment")
                transaction.commit()

                if (fragment is UsbFragment) {
                    curentFragmentComProtocol = fragment.usbCommandsProtocol
                }
            }

        } else {
            showAlertDialog(getString(R.string.UsbNoneConnectDevice))
        }
    }




    // клик по фону уничтожение фрагментов меню
    fun onClickFonDarkMenu(view: View) {
        workFonDarkMenu()
    }

    // клик по кнопке закрытия диалога с загрузкой
    fun onClickCloseDialogLoading(view: View) {
        openCloseLoadingView(false)

    }

    fun workFonDarkMenu() {
        try {
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            transaction.remove(usbComsMenu!!)
            transaction.commit()
        } catch (_: Exception) {}

        try {
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            transaction.remove(selectMenuPrisetSettings!!)
            transaction.commit()
        } catch (_: Exception) {}

        try {
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            transaction.remove(selectMenuPrisetEnforaSettings!!)
            transaction.commit()
        } catch (_: Exception) {}

        try {
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            transaction.remove(selectMenuPrisetPmSettings!!)
            transaction.commit()
        } catch (_: Exception) {}

        ActivationFonDarkMenu(false)
    }

    // активация затеменного экрана
    fun ActivationFonDarkMenu(flag: Boolean) {
        if (flag)
            binding.fonMenu.visibility = View.VISIBLE
        else
            binding.fonMenu.visibility = View.GONE
    }

    // вывод диалоговых окон с сообщениями пользователю
    fun showAlertDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }


    fun showTimerDialog(usbFragment: UsbFragment, nameTypeDevice: String,
                        flagWrite: Boolean = false, clearData: Boolean = true) {

        // очищение данных
        if (clearData)
            curentData = ""

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_timer, null)
        val timerTextView = dialogView.findViewById<TextView>(R.id.timer_text)

        val handler = Handler(Looper.getMainLooper())
        lateinit var alertDialog: AlertDialog  // Используем lateinit для поздней инициализации

        val startTime = TIMEOUT_TOWAIT_RESTART_DEVICE  // начальное время в секундах
        var timeLeft = startTime

        val updateRunnable = object : Runnable {
            override fun run() {

                val timerText: String = getString(R.string.restartDevicePlease).dropLast(2) +
                        if (timeLeft > 9) timeLeft.toString() else "0$timeLeft" // для того что бы если число
                                                                                 // от 1 до 9 то добавлялся 0 типа 03 04 07 и тп

                timerTextView.text = timerText

                if (timeLeft > 0) {
                    timeLeft--

                    if (curentData.isNotEmpty() && curentData.length > NORM_LENGHT_DATA_START) {

                        // проверка на соответсвие девайса
                        if (curentData.contains(nameTypeDevice)) {

                            // прверка диагностика это или драгое
                            if (flagWrite) {
                                runOnUiThread {
                                    usbFragment.writeSettingStart()
                                }
                            } else {
                                runOnUiThread {
                                    usbFragment.readSettingStart()
                                }
                            }
                        } else {
                            runOnUiThread {
                                // если устройство известное то
                                if (devicesTypsAll.contains(curentData.drop(2).dropLast(2))) {
                                    showAlertDialog(getString(R.string.notDeviceType) +
                                            "<" + curentData + ">")
                                } else {
                                    showAlertDialog(getString(R.string.notDeviceTypeApp))
                                }
                            }
                        }


                        timeLeft = 0
                        alertDialog.dismiss()
                    }
                    handler.postDelayed(this, 1000)
                } else {
                    alertDialog.dismiss() // Закрыть диалог после завершения отсчета
                }
            }
        }

        alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("Отмена") { dialog, which ->
                handler.removeCallbacks(updateRunnable) // Остановить Runnable при отмене
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
        handler.postDelayed(updateRunnable, 1000)  // Начать обратный отсчёт
    }

    // для диагностики
    fun showTimerDialogDiag(diagFragmentInterface: DiagFragmentInterface, nameTypeDevice: String) {

        // очищение данных
        curentData = ""

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_timer, null)
        val timerTextView = dialogView.findViewById<TextView>(R.id.timer_text)

        val handler = Handler(Looper.getMainLooper())
        lateinit var alertDialog: AlertDialog  // Используем lateinit для поздней инициализации

        val startTime = TIMEOUT_TOWAIT_RESTART_DEVICE  // начальное время в секундах
        var timeLeft = startTime

        val updateRunnable = object : Runnable {
            override fun run() {

                val timerText: String = getString(R.string.restartDevicePlease).dropLast(2) +
                        if (timeLeft > 9) timeLeft.toString() else "0$timeLeft" // для того что бы если число
                // от 1 до 9 то добавлялся 0 типа 03 04 07 и тп

                timerTextView.text = timerText

                if (timeLeft > 0) {
                    timeLeft--

                    if (curentData.isNotEmpty() && curentData.length > NORM_LENGHT_DATA_START) {

                        // проверка на соответсвие девайса
                        if (curentData.contains(nameTypeDevice)) {
                            runOnUiThread {
                                 diagFragmentInterface.runDiag()
                            }
                        } else {
                            runOnUiThread {
                                // если устройство известное то
                                if (devicesTypsAll.contains(curentData.drop(2).dropLast(2))) {
                                    showAlertDialog(getString(R.string.notDeviceType) +
                                            "<" + curentData + ">")
                                } else {
                                    showAlertDialog(getString(R.string.notDeviceTypeApp))
                                }
                            }
                        }


                        timeLeft = 0
                        alertDialog.dismiss()
                    }
                    handler.postDelayed(this, 1000)
                } else {
                    alertDialog.dismiss() // Закрыть диалог после завершения отсчета
                }
            }
        }

        alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("Отмена") { dialog, which ->
                handler.removeCallbacks(updateRunnable) // Остановить Runnable при отмене
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
        handler.postDelayed(updateRunnable, 1000)  // Начать обратный отсчёт
    }


    // метод для отображения загрузки и информации
    fun printInfoTermAndLoaging(data: String, progression: Int) {
        if (binding.fonLodingView.isVisible) {
            val textTerm: String = binding.textTerm.text.toString() + data
            binding.textTerm.text = textTerm

            binding.progressBarLoading.progress = progression
        }
    }

    // метод для открытия и закрытия меню с терменалом
    fun openCloseLoadingView(flag: Boolean) {
        if (!flagThreadSerialCommands) {
            binding.fonLodingView.visibility =
                if (flag) {
                    View.VISIBLE
                }
                else {
                    binding.progressBarLoading.progress = 0
                    binding.textTerm.text = getString(R.string.loadingTitle)
                    View.GONE
                }
        }
    }


    override fun showDeviceName(deviceName: String) {

        // изменение цвета если есть подлючение
        val drawable = ContextCompat.getDrawable(this, R.drawable.usb)

        // Обертываем наш Drawable для совместимости и изменяем цвет
        drawable?.let {
            val wrappedDrawable = DrawableCompat.wrap(it)

            if (deviceName.isNotEmpty()) DrawableCompat.setTint(wrappedDrawable, Color.GREEN)
            else  DrawableCompat.setTint(wrappedDrawable, Color.RED)

            binding.imageButtonUsbComs.setImageDrawable(wrappedDrawable)
        }
    }

    override fun showButtonConnection(con: Boolean) {

        binding.fonLockUses.visibility = View.GONE

        // при подключении уберестся фрагмент и вывод успеха подкл
        if (con) {
            workFonDarkMenu()


            // разблокируем кнопки-------------------------------------------------

            // получаем последний фрагмент который был сгенерирован
            val methodFragments = MethodFragments()
            val curentFragment = methodFragments.getLastFragmentUsb(supportFragmentManager)

            // если фрагмент является фрагментом usbFragment то
            curentFragment.let { fragment ->
                if (fragment is UsbFragment) {
                    fragment.lockFromDisconnected(true)
                }
            }
            //showAlertDialog(getString(R.string.ConnectSuccess))
        }
    }

    override fun withdrawalsShow(msg: String, notshowdis: Boolean) {
        showAlertDialog(msg)
    }

    override fun printData(data: String) {

        curentData += data

        // прокурчивание вниз
        binding.ScrollWriteLoadingForDevice.post {
            binding.ScrollWriteLoadingForDevice.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun printDataByte(data: ByteArray) {
        curentDataByte = data

        // прокурчивание вниз
        binding.ScrollWriteLoadingForDevice.post {
            binding.ScrollWriteLoadingForDevice.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun printDSR_CTS(dsr: Int, cts: Int) {

    }

    override fun disconnected() {
        // получаем последний фрагмент который был сгенерирован
        val methodFragments = MethodFragments()
        val curentFragment = methodFragments.getLastFragmentUsb(supportFragmentManager)

        // если фрагмент является фрагментом usbFragment то
        curentFragment.let { fragment ->
            if (fragment is UsbFragment) {
                fragment.lockFromDisconnected(false)
            }
        }


        //binding.fonLockUses.visibility = View.VISIBLE
        /*
        val mainFragment = MainFragment()

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Новый фрагент
        transaction.replace(binding.fragmentContainerMainContent.id, mainFragment)
        //transaction.addToBackStack("MainFragment")
        transaction.commit()

        // при отключении выводиться предупреждение о том что подлючение разорвано
        showAlertDialog(getString(R.string.Disconnected))

        // закрытие диалога с загрузкой
        openCloseLoadingView(false)

        // сброс имени типа устроства
        printDeviceTypeName("")*/
    }


    // Метод для проверки, является ли подключенное устройство целевым устройством
    private fun isTargetDevice(device: UsbDevice): Boolean {
        return usbDevices.any { it.vendorId == device.vendorId && (it.productId == null || it.productId == device.productId) }
    }


    // подключения и регистрация широковещятельного приемника
    override fun connectToUsbDevice(device: UsbDevice) {

        // проверка и подключение
        if (isTargetDevice(device)) {
            val usbManager: UsbManager = getSystemService(Context.USB_SERVICE) as UsbManager

            try {
                // Здесь мы просто проверяем, есть ли у нас устройство и подключаем его
                val connection = usbManager.openDevice(device)
                if (connection != null) {
                    // Успешно подключено
                    // Здесь можно выполнить дальнейшую работу с устройством
                    usb.connect(connection, device)
                } else {
                    // Обработка ошибки подключения
                    showAlertDialog(getString(R.string.mainActivityText_ErrorConnect))

                    // проверка вдруг нету разрешения
                    /*if (!usbManager.hasPermission(device)) {
                        requestPermission(device)
                    }*/
                }
            } catch (e: Exception) {
                // Обработка исключений
                showAlertDialog(getString(R.string.mainActivityText_ErrorConnect))

                // проверка вдруг нету разрешения
                /*if (!usbManager.hasPermission(device)) {
                    requestPermission(device)
                }*/
            }
        }
    }
}