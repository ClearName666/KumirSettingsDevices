package com.example.kumirsettingupdevices

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
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
import com.example.kumirsettingupdevices.usbFragments.A61Fragment
import com.example.kumirsettingupdevices.usbFragments.ACCB030CoreFragment
import com.example.kumirsettingupdevices.usbFragments.ACCB030Fragment
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


    companion object {
        const val TIMEOUT_TOWAIT_RESTART_DEVICE: Int = 29 // 30 - 1 секудны
        const val NORM_LENGHT_DATA_START = 5
    }

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
                        PrisetsValue.prisets[preset.name!!] = Priset(preset.name, preset.mode!!, preset.apn!!,
                            preset.port!!, preset.server!!, preset.login!!, preset.password!!)
                    }
                }
            }
        } catch (_: Exception) {}

        // загрузка всех присетов enfora
        try {
            lifecycleScope.launch {
                // присеты enfora
                presetEnforaDao.getAll().collect { presets ->
                    for (enforaPreseet in presets) {
                        PresetsEnforaValue.presets[enforaPreseet.name!!] =
                            Enfora(0, enforaPreseet.name, enforaPreseet.apn!!,
                                enforaPreseet.login!!, enforaPreseet.password!!,
                                enforaPreseet.server1!!, enforaPreseet.server2!!,
                                enforaPreseet.timeout!!, enforaPreseet.sizeBuffer!!)
                    }
                }
            }
        } catch (_: Exception) {}

        // загрузка всех присетов Pm
        try {
            lifecycleScope.launch {
                // присеты enfora
                presetPmDao.getAll().collect { presets ->
                    for (PmPreset in presets) {
                        PrisetsPmValue.presets[PmPreset.name!!] =
                            Pm(0, PmPreset.name, PmPreset.mode, PmPreset.keyNet!!,
                                PmPreset.power!!, PmPreset.diopozone)
                    }
                }
            }
        } catch (_: Exception) {}





        // смена настроек usb ---------------------------------------------------
        ConstUsbSettings.speedIndex = 9 // скорость 115200

        // usb.flagAtCommandYesNo = true


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




    // ----------------клики в раскрывающимся меню-------------------

    fun onClickEnforma1318(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val enforma1318 = Enfora1318Fragment()
        createSettingFragment(enforma1318)
    }
    fun onClickM31(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m31 = M31Fragment()
        createSettingFragment(m31)
    }
    fun onClickK21K23(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val k21k23 = K21K23Fragment()
        createSettingFragment(k21k23)
    }
    fun onClickM32(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32 = M32Fragment()
        createSettingFragment(m32)
    }
    fun onClickDiag(serialNumber: String, programVersion: String) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val diag = DiagFragment(serialNumber, programVersion)
        createSettingFragment(diag)
    }
    fun onClickM32Lite(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val m32Lite = M32LiteFragment()
        createSettingFragment(m32Lite)
    }
    fun onClickA61(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val a61 = A61Fragment()
        createSettingFragment(a61)
    }
    fun onClickPM81(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val pm81 = PM81Fragment()
        createSettingFragment(pm81)
    }
    fun onClickACCB030Core(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val accbo30Core = ACCB030CoreFragment()
        createSettingFragment(accbo30Core)
    }
    fun onClickACCB030(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        val accbo30 = ACCB030Fragment()
        createSettingFragment(accbo30)

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

            /*if (supportFragmentManager.fragments.size > 1) {
                supportFragmentManager.popBackStack()
            }*/

            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            // Новый фрагент
            transaction.replace(binding.fragmentContainerMainContent.id, fragment)
            //transaction.addToBackStack("SettingDeviceFragment")
            transaction.commit()
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
                        timeLeft.toString()
                timerTextView.text = timerText

                if (timeLeft > 0) {
                    timeLeft--

                    if (curentData.isNotEmpty() && curentData.length > NORM_LENGHT_DATA_START) {

                        // проверка на соответсвие девайса
                        if (curentData.contains(nameTypeDevice)) {
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
                                showAlertDialog(getString(R.string.notDeviceType) +
                                        "<" + curentData + ">")
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

        // при подключении уберестся фрагмент и вывод успеха подкл
        if (con) {
            workFonDarkMenu()
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
        printDeviceTypeName("")
    }

    // подключения и регистрация широковещятельного приемника
    override fun connectToUsbDevice(device: UsbDevice) {
        val usbManager: UsbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        try {
            val permissionIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Для Android 12 (API уровень 31)
                PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(usb.ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_MUTABLE)
            } else {
                // Для Android ниже 12
                PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(usb.ACTION_USB_PERMISSION),
                    0)
            }

            registerReceiver(usb.usbReceiver, IntentFilter(usb.ACTION_USB_PERMISSION))
            usbManager.requestPermission(device, permissionIntent)

        } catch (e: Exception) {
            showAlertDialog(getString(R.string.mainActivityText_ErrorConnect))
        }
    }


}