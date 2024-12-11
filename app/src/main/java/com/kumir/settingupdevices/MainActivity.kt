package com.kumir.settingupdevices

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kumir.settingupdevices.presetFragments.SelectMenuPrisetSettings
import com.kumir.settingupdevices.dataBasePreset.AppDatabase
import com.kumir.settingupdevices.dataBasePreset.Enfora
import com.kumir.settingupdevices.dataBasePreset.EnforaDao
import com.kumir.settingupdevices.dataBasePreset.Pm
import com.kumir.settingupdevices.dataBasePreset.PmDao
import com.kumir.settingupdevices.dataBasePreset.Preset
import com.kumir.settingupdevices.dataBasePreset.PresetDao
import com.kumir.settingupdevices.databinding.MainActivityBinding
import com.kumir.settingupdevices.diag.ACCB030DiagFragment
import com.kumir.settingupdevices.diag.DiagFragment
import com.kumir.settingupdevices.diag.DiagFragmentInterface
import com.kumir.settingupdevices.diag.DiagM32DFragment
import com.kumir.settingupdevices.diag.DiagPM81Fragment
import com.kumir.settingupdevices.model.recyclerModel.Priset
import com.kumir.settingupdevices.ports.PortDeviceSetting
import com.kumir.settingupdevices.presetFragments.SelectMenuPrisetEnforaSettings
import com.kumir.settingupdevices.presetFragments.SelectMenuPrisetPmSettings
import com.kumir.settingupdevices.settings.DeviceAccountingPrisets
import com.kumir.settingupdevices.settings.PresetsEnforaValue
import com.kumir.settingupdevices.settings.PrisetsPmValue
import com.kumir.settingupdevices.settings.PrisetsValue
import com.kumir.settingupdevices.usb.Usb
import com.kumir.settingupdevices.usb.UsbActivityInterface
import com.kumir.settingupdevices.usb.UsbFragment
import com.kumir.settingupdevices.usbFragments.ACCB030CoreFragment
import com.kumir.settingupdevices.usbFragments.ACCB030Fragment
import com.kumir.settingupdevices.diag.Enfora1318DiagFragment
import com.kumir.settingupdevices.sensors.SensorDT112Fragment
import com.kumir.settingupdevices.usb.UsbCommandsProtocol
import com.kumir.settingupdevices.usb.UsbDeviceDescriptor
import com.kumir.settingupdevices.usbFragments.Enfora1318Fragment
import com.kumir.settingupdevices.usbFragments.FirmwareSTMFragment
import com.kumir.settingupdevices.usbFragments.K21K23Fragment
import com.kumir.settingupdevices.usbFragments.M31Fragment
import com.kumir.settingupdevices.usbFragments.M32DFragment
import com.kumir.settingupdevices.usbFragments.M32Fragment
import com.kumir.settingupdevices.usbFragments.M32LiteFragment
import com.kumir.settingupdevices.usbFragments.P101Fragment
import com.kumir.settingupdevices.usbFragments.PM81Fragment
import com.kumir.settingupdevices.usbFragments.PrisetFragment
import com.kumir.settingupdevices.usbFragments.RimFragment
import com.kumir.testappusb.settings.ConstUsbSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.widget.Toast

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.kumir.settingupdevices.auto.AutoFindDeviceFragment
import com.kumir.settingupdevices.auto.AutoFindOneWireFragment
import com.kumir.settingupdevices.sensors.SensorPipeBlockageV1_01

class MainActivity : AppCompatActivity(), UsbActivityInterface {

    override val usb: Usb = Usb(this)
    private lateinit var binding: MainActivityBinding

    private var usbComsMenu: UsbComsMenu? = null
    private var selectMenuPrisetSettings: SelectMenuPrisetSettings? = null
    private var selectMenuPrisetEnforaSettings: SelectMenuPrisetEnforaSettings? = null
    private var selectMenuPrisetPmSettings: SelectMenuPrisetPmSettings? = null

    // буферы денных
    var curentData: String = ""
    //var curentDataByte: ByteArray = byteArrayOf()
    var currentDataByteAll: ByteArray = byteArrayOf() // все данные в кучи
    // var curentDataByteNonClear: ByteArray = byteArrayOf()

    // flag Для контроля передачи информации
    var flagThreadSerialCommands: Boolean = false


    // дополнительный текст для диалога в купир ядро и прошивка
    var additionallyTextTimerDialog: String = ""
    var flagCoreOrProgramACCB030: Boolean = false

    // база данных
    lateinit var presetDao: PresetDao
    lateinit var presetEnforaDao: EnforaDao
    lateinit var presetPmDao: PmDao

    // флаг для контроля нужно ли использовать другуой текст для диалога с сохранением шаблона
    var flagLoadPreset: Boolean = false

    // список с устройств с котрыми можно работать
    private val devicesTypsAll: String =
        "KUMIR-К21К23 READY" +
        "KUMIR-M32 READY" +
        "KUMIR-M32LITE READY" +
        "KUMIR-RM81A READY" +
        "KUMIR-VZLET_ASSV030 READY" +
        "KUMIR-M32D READY"

    // текущий фрагмент
    var curentFragmentComProtocol: UsbCommandsProtocol? = null


    companion object {
        const val TIMEOUT_TOWAIT_RESTART_DEVICE: Int = 29 // 30 - 1 секудны
        const val NORM_LENGHT_DATA_START = 5
        const val TIMEOUT_RECONNECT = 1000L

        const val REQUEST_KOD_GEO: Int = 2
        const val REQUEST_KOD_PUSH: Int = 3

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


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (usb.ACTION_USB_PERMISSION == intent.action) {
            synchronized(this) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.apply {
                        connectToUsbDevice(device)
                    }
                }
            }
        }
        if (requestCode == REQUEST_KOD_GEO) {
            // проверяем есть ли разрешение на отправку пуш уведомлений
            checkPermissionPush()
        } else if (requestCode == REQUEST_KOD_PUSH) {
            // отправляем приветственное уведомление
            startNotificationWorker()
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (usb.ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            connectToUsbDevice(device)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            super.onCreate(savedInstanceState)

            val filter = IntentFilter(usb.ACTION_USB_PERMISSION)
            registerReceiver(usbReceiver, filter)

            binding = MainActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))


            // создание главного фрагмента (отображается при открытии)
            val mainFragment = MainFragment()
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            // Новый фрагент
            transaction.replace(binding.fragmentContaineContent.id, mainFragment)
            //transaction.addToBackStack("MainFragment")
            transaction.commit()

            loadInfoDataBase()

            // смена настроек usb ---------------------------------------------------
            ConstUsbSettings.speedIndex = 9 // скорость 115200

            // usb.flagAtCommandYesNo = true


            // в случчае если девай подключен к usb то сразц подключиться к нему
            val intent = intent
            val usbDevice: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            usbDevice?.let {
                connectToUsbDevice(usbDevice)
            }



            // Проверяем, есть ли разрешения на доступ к местоположению
            if (!checkLocationPermissions()) {
                requestLocationPermissions()
            }

            // проверяем есть ли разрешение на отправку пуш уведомлений
            //checkPermissionPush()
            // код теперь внутри обработчика разрешений
        }
    }


    private fun checkPermissionPush() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                // ОТПРАВЛЯЕМ ПУШ УВЕДОМЛЕНИЕ И ЗАПУСКАЕМ ФОНОВЫЙ ПРОЦЕСС
                startNotificationWorker()
            }
        } else {
            // Для более старых версий Android
            startNotificationWorker()
        }
    }

    // запуск фоновой активности для отправки пуш уведомлений
    private fun startNotificationWorker() {
        val notificationWork = OneTimeWorkRequest.Builder(NotificationWorker::class.java).build()
        WorkManager.getInstance(this).enqueue(notificationWork)
    }

    private fun loadInfoDataBase() {

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
        } catch (_: Exception) {}
    }

    fun checkLocationPermissions(): Boolean {
        val permissionState = ContextCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION),
            REQUEST_KOD_GEO
        )
    }


    /*override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                requestLocationPermissions()
            }
        }
    }*/

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

    // автоматический поиск модемов м32 м32лайт м32д
    fun onClickAutoFindDevice(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val autoFindDevice = AutoFindDeviceFragment(this)
        createSettingFragment(autoFindDevice)
    }

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
        onStartM32Settings(false)
    }
    fun onStartM32Settings(autoFlag: Boolean = false) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32 = M32Fragment(autoFlag)
        createSettingFragment(m32)
    }

    fun onClickM32Diag(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32Diag = DiagFragment("KUMIR-M32 READY")
        createSettingFragment(m32Diag)
    }

    // раскрывающаяся понель
    fun onClickM32D(view: View) {
        if (binding.M32DSettings.visibility == View.GONE) {
            binding.M32DSettings.visibility = View.VISIBLE
            binding.M32DDiag.visibility = View.VISIBLE
            binding.imageM32DMenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.top_arrow_5_svgrepo_com)
            )
        } else {
            binding.M32DSettings.visibility = View.GONE
            binding.M32DDiag.visibility = View.GONE
            binding.imageM32DMenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.down_2_svgrepo_com__1_)
            )
        }
    }
    fun onClickM32DSettings(view: View) {
        onStartM32DSettings()
    }
    fun onStartM32DSettings(autoFlag: Boolean = false) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32d = M32DFragment(autoFlag)
        createSettingFragment(m32d)
    }

    fun onClickM32DDiag(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32Diag = DiagM32DFragment("KUMIR-M32D READY")
        createSettingFragment(m32Diag)
    }


    /*fun onClickDiag(serialNumber: String, programVersion: String) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val diag = DiagFragment(serialNumber, programVersion)
        createSettingFragment(diag)
    }*/
    fun onClickM32Lite(view: View) {
        onStartM32Lite(false)
    }
    fun onStartM32Lite(autoFlag: Boolean = false) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32Lite = M32LiteFragment(autoFlag)
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
        onStartPM81Settings(false)
    }
    fun onStartPM81Settings(autoFlag: Boolean = false) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val pm81 = PM81Fragment(autoFlag)
        createSettingFragment(pm81)
    }

    fun onClickPM81Diag(view: View) {
        if (checkLocationPermissions()) {
            binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

            val pm81Diag = DiagPM81Fragment("KUMIR-RM81A READY")
            createSettingFragment(pm81Diag)
        } else {
            requestLocationPermissions()
            showAlertDialog(getString(R.string.nonPermissionsPos))
        }
    }


    fun onClickACCB030Core(view: View) {
        if (binding.ACCB030CoreSettings.visibility == View.GONE) {
            binding.ACCB030CoreSettings.visibility = View.VISIBLE
            //binding.ACCB030CoreDiag.visibility = View.VISIBLE
            binding.imageACCB030CoreMenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.top_arrow_5_svgrepo_com)
            )
        } else {
            binding.ACCB030CoreSettings.visibility = View.GONE
            //binding.ACCB030CoreDiag.visibility = View.GONE
            binding.imageACCB030CoreMenuButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.down_2_svgrepo_com__1_)
            )
        }
    }
    fun onClickACCB030CoreSettings(view: View) {
        onStartACCB030CoreSettings(false)
    }
    fun onStartACCB030CoreSettings(autoFlag: Boolean = false) {


        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val accbo30Core = ACCB030CoreFragment(autoFlag)
        createSettingFragment(accbo30Core)
        flagCoreOrProgramACCB030 = true
    }

    fun onClickACCB030CoreDiag(view: View) {
        showAlertDialog(getString(R.string.errorCodeNone))
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
        flagCoreOrProgramACCB030 = true
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

    fun onClickRim(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val rim = RimFragment()
        createSettingFragment(rim)
    }


    // функция для автоматического посика датчика
    fun onClickAutoFindOneWire(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val autoFindOneWire = AutoFindOneWireFragment(this)
        createSettingFragment(autoFindOneWire)
    }

    // проверка датчиков (сетка)
    fun onClickSensorDT112(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val dt112 = SensorDT112Fragment()
        createSettingFragment(dt112)

    }

    fun onClickSensorPipeBlockage(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        binding.textNameDevice.text = getString(R.string.sensorPipeBlockage)

        val sensorPipeBlockageV1_01 = SensorPipeBlockageV1_01(this)
        createSettingFragment(sensorPipeBlockageV1_01)

    }

    fun onClickStmLoader(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val stmLoaderFragment = FirmwareSTMFragment(this)
        createSettingFragment(stmLoaderFragment)
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
                        presetDao.upsert(preset, this@MainActivity)

                        // сразу добовлеям что бы он стал активным и с ним можно было работать
                        PrisetsValue.prisets[name] = Priset(name, mode, apn, port, server, login, password)

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
                        presetEnforaDao.upsert(preset, this@MainActivity)

                        // сразу добовлеям что бы он стал активным и с ним можно было работать
                        PresetsEnforaValue.presets[name] = Enfora(0, name, apn, login, password,
                            server1, server2, timeout, sizeBuffer)

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
                        presetPmDao.upsert(preset, this@MainActivity)

                        // сразу добовлеям что бы он стал активным и с ним можно было работать
                        PrisetsPmValue.presets[name] = Pm(0, name, mode, keyNet, power, range)

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

        // в прошивке и ядре выводить окно с диалогом напоминалкой о переключении П1:1 в on
        if (flagCoreOrProgramACCB030) {
            showAlertDialog(getString(R.string.coreProgramClueText))
            flagCoreOrProgramACCB030 = false
        }


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
                    transaction.replace(binding.fragmentContaineContent.id, fragment)
                    transaction.commitNow()

                    if (fragment is UsbFragment) {
                        curentFragmentComProtocol = fragment.usbCommandsProtocol
                    }
                }
            } else {
                val fragmentManager = supportFragmentManager
                val transaction = fragmentManager.beginTransaction()

                // Новый фрагент
                transaction.replace(binding.fragmentContaineContent.id, fragment)
                transaction.commitNow()

                if (fragment is UsbFragment) {
                    curentFragmentComProtocol = fragment.usbCommandsProtocol
                }
            }

        } else {
            showAlertDialog(getString(R.string.UsbNoneConnectDevice))
        }
    }


    // для выбора обновить данные или же изменить имя
    fun menuUpdateName(preset: Preset?, pm: Pm?, enfora: Enfora?) {

        // Открываем менб изменения имени
        binding.fonMenu.visibility = View.VISIBLE
        binding.updateName.visibility = View.VISIBLE
        binding.buttonEditData.visibility = View.VISIBLE


        // поверка для того что бы узнать что имя назначено то которое стоит по умолчанию
        val reservedName = listOf(
            getString(R.string.priset1),
            getString(R.string.priset2),
            getString(R.string.priset3),
            getString(R.string.priset4),
            getString(R.string.priset5),
        )

        if (preset != null) {
            if (preset.name!! in reservedName) {
                binding.buttonEditData.visibility = View.GONE
            }
        } else if (pm != null) {
            if (pm.name!! in reservedName) {
                binding.buttonEditData.visibility = View.GONE
            }
        } else if (enfora != null) {
            if (enfora.name!! in reservedName) {
                binding.buttonEditData.visibility = View.GONE
            }
        }

        if (preset != null) {
            // обновление данных
            binding.buttonEditData.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        presetDao.updateByName(
                            name = preset.name!!,
                            mode = preset.mode,
                            apn = preset.apn,
                            server = preset.server,
                            port = preset.port,
                            login = preset.login,
                            password = preset.password
                        )
                    }
                }

                // закрываем окно
                binding.fonMenu.visibility = View.GONE
                binding.updateName.visibility = View.GONE

                showAlertDialog(getString(R.string.sucPresetSaveDataBase))
            }

            // обновить имя
            binding.buttonNewName.setOnClickListener {
                if (preset.name != binding.inputUpdateName.text.toString().trim() &&
                    binding.inputUpdateName.text.toString().trim().isNotEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            presetDao.insert(
                                Preset(
                                    0,
                                    name = binding.inputUpdateName.text.toString(),
                                    mode = preset.mode,
                                    apn = preset.apn,
                                    server = preset.server,
                                    port = preset.port,
                                    login = preset.login,
                                    password = preset.password
                                )
                            )
                        }
                    }

                    // закрываем окно
                    binding.fonMenu.visibility = View.GONE
                    binding.updateName.visibility = View.GONE

                    if (flagLoadPreset) {
                        showAlertDialog(getString(R.string.sucPresetLoadDataBase))

                        // выозврат флага вдруг он соит в позиции диалога с успешным сохранением
                        flagLoadPreset = false
                    } else {
                        showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                    }
                } else {
                    showAlertDialog(getString(R.string.notValidUpdateName))
                }
            }

        } else if (pm != null) {
            // обновление данных
            binding.buttonEditData.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        presetPmDao.updateByName(
                            name = pm.name!!,
                            mode = pm.mode,
                            keyNet = pm.keyNet,
                            power = pm.power,
                            diopozone = pm.diopozone
                        )
                    }
                }

                // закрываем окно
                binding.fonMenu.visibility = View.GONE
                binding.updateName.visibility = View.GONE

                showAlertDialog(getString(R.string.sucPresetSaveDataBase))
            }

            // обновить имя
            binding.buttonNewName.setOnClickListener {
                if (pm.name != binding.inputUpdateName.text.toString().trim() &&
                    binding.inputUpdateName.text.toString().trim().isNotEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            presetPmDao.insert(
                                Pm(
                                    0,
                                    name = binding.inputUpdateName.text.toString(),
                                    mode = pm.mode,
                                    keyNet = pm.keyNet,
                                    power = pm.power,
                                    diopozone = pm.diopozone
                                )
                            )
                        }
                    }

                    // закрываем окно
                    binding.fonMenu.visibility = View.GONE
                    binding.updateName.visibility = View.GONE

                    if (flagLoadPreset) {
                        showAlertDialog(getString(R.string.sucPresetLoadDataBase))

                        // выозврат флага вдруг он соит в позиции диалога с успешным сохранением
                        flagLoadPreset = false
                    } else {
                        showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                    }
                } else {
                    showAlertDialog(getString(R.string.notValidUpdateName))
                }
            }

        } else if (enfora != null) {
            // обновление данных
            binding.buttonEditData.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        presetEnforaDao.updateByName(
                            name = enfora.name!!,
                            apn = enfora.apn,
                            login = enfora.login,
                            password = enfora.password,
                            server1 = enfora.server1,
                            server2 = enfora.server2,
                            timeout = enfora.timeout,
                            sizeBuffer = enfora.sizeBuffer
                        )
                    }
                }

                // закрываем окно
                binding.fonMenu.visibility = View.GONE
                binding.updateName.visibility = View.GONE

                showAlertDialog(getString(R.string.sucPresetSaveDataBase))
            }

            // обновить имя
            binding.buttonNewName.setOnClickListener {
                if (enfora.name != binding.inputUpdateName.text.toString().trim() &&
                    binding.inputUpdateName.text.toString().trim().isNotEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            presetEnforaDao.insert(
                                Enfora(
                                    0,
                                    name = binding.inputUpdateName.text.toString(),
                                    apn = enfora.apn,
                                    login = enfora.login,
                                    password = enfora.password,
                                    server1 = enfora.server1,
                                    server2 = enfora.server2,
                                    timeout = enfora.timeout,
                                    sizeBuffer = enfora.sizeBuffer
                                )
                            )
                        }
                    }

                    // закрываем окно
                    binding.fonMenu.visibility = View.GONE
                    binding.updateName.visibility = View.GONE

                    if (flagLoadPreset) {
                        showAlertDialog(getString(R.string.sucPresetLoadDataBase))

                        // выозврат флага вдруг он соит в позиции диалога с успешным сохранением
                        flagLoadPreset = false
                    } else {
                        showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                    }
                } else {
                    showAlertDialog(getString(R.string.notValidUpdateName))
                }
            }
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

        // закрытие меню изменения имени шаблона
        binding.updateName.visibility = View.GONE

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

                val timerText: String = additionallyTextTimerDialog + getString(R.string.restartDevicePlease).dropLast(2) +
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

    // функция для автоматического определения устройства
    fun showTimerDialogAutoFindDevice() {

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
                        if (curentData.contains("KUMIR-M32 READY")) {
                            onStartM32Settings(true)
                        } else if (curentData.contains("KUMIR-M32LITE READY")) {
                            onStartM32Lite(true)
                        } else if (curentData.contains("KUMIR-M32D READY")) {
                            onStartM32DSettings(true)
                        } else if (curentData.contains("KUMIR-RM81A READY")) {
                            onStartPM81Settings(true)
                        } else if (curentData.contains("KUMIR-VZLET_ASSV030 READY")) {
                            onStartACCB030CoreSettings(true)
                        }  else {
                            runOnUiThread {
                                showAlertDialog(getString(R.string.notDeviceM32))
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
        // curentDataByte = data

        Log.d("dataByte", data.joinToString(separator = "") { "%02X".format(it) })
        // прокурчивание вниз
        binding.ScrollWriteLoadingForDevice.post {
            binding.ScrollWriteLoadingForDevice.fullScroll(View.FOCUS_DOWN)
        }
        currentDataByteAll += data
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
                /*Thread {
                    // ждем время и проверяем вдруг это просто переподключение
                    Thread.sleep(TIMEOUT_RECONNECT)
                    if (!usb.checkConnectToDevice()) {
                        runOnUiThread {*/
                            fragment.lockFromDisconnected(false)
                       /* }
                    }
                }.start()*/
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
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(usb.ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)

        // проверка и подключение
        if (isTargetDevice(device)) {
            val usbManager: UsbManager = getSystemService(Context.USB_SERVICE) as UsbManager

            try {
                // Здесь мы просто проверяем, есть ли у нас устройство и подключаем его
                val connection = usbManager.openDevice(device)
                if (connection != null) {
                    // Успешно подключено
                    // Здесь можно выполнить дальнейшую работу с устройством
                    if (usbManager.hasPermission(device))
                        usb.connect(connection, device)
                } else {
                    if (!usbManager.hasPermission(device)) {
                        // Запрос разрешения, если его нет
                        usbManager.requestPermission(device, permissionIntent)
                    } else {
                        // Обработка ошибки подключения
                        showAlertDialog(getString(R.string.mainActivityText_ErrorConnect))
                    }
                }
            } catch (e: Exception) {
                if (!usbManager.hasPermission(device)) {
                    // Запрос разрешения, если его нет
                    usbManager.requestPermission(device, permissionIntent)
                } else {
                    // Обработка ошибки подключения
                    showAlertDialog(getString(R.string.mainActivityText_ErrorConnect))
                }
            }
        }
    }
}