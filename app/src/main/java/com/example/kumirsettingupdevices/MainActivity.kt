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
import com.example.kumirsettingupdevices.databinding.MainActivityBinding
import com.example.kumirsettingupdevices.ports.PortDeviceSetting
import com.example.kumirsettingupdevices.settings.DeviceAccountingPrisets
import com.example.kumirsettingupdevices.usb.Usb
import com.example.kumirsettingupdevices.usb.UsbActivityInterface
import com.example.kumirsettingupdevices.usb.UsbFragment
import com.example.kumirsettingupdevices.usbFragments.A61Fragment
import com.example.kumirsettingupdevices.usbFragments.ACCB030CoreFragment
import com.example.kumirsettingupdevices.usbFragments.ACCB030Fragment
import com.example.kumirsettingupdevices.usbFragments.Enforma1318Fragment
import com.example.kumirsettingupdevices.usbFragments.K21K23Fragment
import com.example.kumirsettingupdevices.usbFragments.M31Fragment
import com.example.kumirsettingupdevices.usbFragments.M32Fragment
import com.example.kumirsettingupdevices.usbFragments.M32LiteFragment
import com.example.kumirsettingupdevices.usbFragments.P101Fragment
import com.example.kumirsettingupdevices.usbFragments.PM81Fragment
import com.example.testappusb.settings.ConstUsbSettings

class MainActivity : AppCompatActivity(), UsbActivityInterface {

    override val usb: Usb = Usb(this)
    private lateinit var binding: MainActivityBinding

    private var usbComsMenu: UsbComsMenu? = null
    var curentData: String = ""

    companion object {
        const val TIMEOUT_TOWAIT_RESTART_DEVICE: Int = 29 // 30 - 1 секудны
        const val NORM_LENGHT_DATA_START = 5
    }

    // устройства прибора учета
    val portsDeviceSetting: List<PortDeviceSetting> = listOf(
        PortDeviceSetting(DeviceAccountingPrisets.SPT941Speed, DeviceAccountingPrisets.SPT941BitData, DeviceAccountingPrisets.SPT941StopBit, DeviceAccountingPrisets.SPT941Parity),
        PortDeviceSetting(DeviceAccountingPrisets.SPT944Speed, DeviceAccountingPrisets.SPT944BitData, DeviceAccountingPrisets.SPT944StopBit, DeviceAccountingPrisets.SPT944Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TSP025Speed, DeviceAccountingPrisets.TSP025BitData, DeviceAccountingPrisets.TSP025StopBit, DeviceAccountingPrisets.TSP025Parity),
        PortDeviceSetting(DeviceAccountingPrisets.PSCH4TMV23Speed, DeviceAccountingPrisets.PSCH4TMV23BitData, DeviceAccountingPrisets.PSCH4TMV23StopBit, DeviceAccountingPrisets.PSCH4TMV23Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TSP027Speed, DeviceAccountingPrisets.TSP027BitData, DeviceAccountingPrisets.TSP027StopBit, DeviceAccountingPrisets.TSP027Parity),
        PortDeviceSetting(DeviceAccountingPrisets.PowerCE102MSpeed, DeviceAccountingPrisets.PowerCE102MBitData, DeviceAccountingPrisets.PowerCE102MStopBit, DeviceAccountingPrisets.PowerCE102MParity),
        PortDeviceSetting(DeviceAccountingPrisets.Mercury206Speed, DeviceAccountingPrisets.Mercury206BitData, DeviceAccountingPrisets.Mercury206StopBit, DeviceAccountingPrisets.Mercury206Parity),
        PortDeviceSetting(DeviceAccountingPrisets.KM5PM5Speed, DeviceAccountingPrisets.KM5PM5BitData, DeviceAccountingPrisets.KM5PM5StopBit, DeviceAccountingPrisets.KM5PM5Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TEM104Speed, DeviceAccountingPrisets.TEM104BitData, DeviceAccountingPrisets.TEM104StopBit, DeviceAccountingPrisets.TEM104Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TEM106Speed, DeviceAccountingPrisets.TEM106BitData, DeviceAccountingPrisets.TEM106StopBit, DeviceAccountingPrisets.TEM106Parity),
        PortDeviceSetting(DeviceAccountingPrisets.BKT5Speed, DeviceAccountingPrisets.BKT5BitData, DeviceAccountingPrisets.BKT5StopBit, DeviceAccountingPrisets.BKT5Parity),
        PortDeviceSetting(DeviceAccountingPrisets.BKT7Speed, DeviceAccountingPrisets.BKT7BitData, DeviceAccountingPrisets.BKT7StopBit, DeviceAccountingPrisets.BKT7Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TePOCCSpeed, DeviceAccountingPrisets.TePOCCBitData, DeviceAccountingPrisets.TePOCCStopBit, DeviceAccountingPrisets.TePOCCParity),
        PortDeviceSetting(DeviceAccountingPrisets.SPT942Speed, DeviceAccountingPrisets.SPT942BitData, DeviceAccountingPrisets.SPT942StopBit, DeviceAccountingPrisets.SPT942Parity),
        PortDeviceSetting(DeviceAccountingPrisets.SPT943Speed, DeviceAccountingPrisets.SPT943BitData, DeviceAccountingPrisets.SPT943StopBit, DeviceAccountingPrisets.SPT943Parity),
        PortDeviceSetting(DeviceAccountingPrisets.SPT961Speed, DeviceAccountingPrisets.SPT961BitData, DeviceAccountingPrisets.SPT961StopBit, DeviceAccountingPrisets.SPT961Parity),
        PortDeviceSetting(DeviceAccountingPrisets.KT7AbacanSpeed, DeviceAccountingPrisets.KT7AbacanBitData, DeviceAccountingPrisets.KT7AbacanStopBit, DeviceAccountingPrisets.KT7AbacanParity),
        PortDeviceSetting(DeviceAccountingPrisets.MT200DSSpeed, DeviceAccountingPrisets.MT200DSBitData, DeviceAccountingPrisets.MT200DSStopBit, DeviceAccountingPrisets.MT200DSParity),
        PortDeviceSetting(DeviceAccountingPrisets.TCP010Speed, DeviceAccountingPrisets.TCP010BitData, DeviceAccountingPrisets.TCP010StopBit, DeviceAccountingPrisets.TCP010Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TCP010MSpeed, DeviceAccountingPrisets.TCP010MBitData, DeviceAccountingPrisets.TCP010MStopBit, DeviceAccountingPrisets.TCP010MParity),
        PortDeviceSetting(DeviceAccountingPrisets.TCP023Speed, DeviceAccountingPrisets.TCP023BitData, DeviceAccountingPrisets.TCP023StopBit, DeviceAccountingPrisets.TCP023Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TCPB024Speed, DeviceAccountingPrisets.TCPB024BitData, DeviceAccountingPrisets.TCPB024StopBit, DeviceAccountingPrisets.TCPB024Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TCP026Speed, DeviceAccountingPrisets.TCP026BitData, DeviceAccountingPrisets.TCP026StopBit, DeviceAccountingPrisets.TCP026Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TCPB03XSpeed, DeviceAccountingPrisets.TCPB03XBitData, DeviceAccountingPrisets.TCPB03XStopBit, DeviceAccountingPrisets.TCPB03XParity),
        PortDeviceSetting(DeviceAccountingPrisets.TCPB042Speed, DeviceAccountingPrisets.TCPB042BitData, DeviceAccountingPrisets.TCPB042StopBit, DeviceAccountingPrisets.TCPB042Parity),
        PortDeviceSetting(DeviceAccountingPrisets.YCPB5XXSpeed, DeviceAccountingPrisets.YCPB5XXBitData, DeviceAccountingPrisets.YCPB5XXStopBit, DeviceAccountingPrisets.YCPB5XXParity),
        PortDeviceSetting(DeviceAccountingPrisets.PCL212Speed, DeviceAccountingPrisets.PCL212BitData, DeviceAccountingPrisets.PCL212StopBit, DeviceAccountingPrisets.PCL212Parity),
        PortDeviceSetting(DeviceAccountingPrisets.SA942MSpeed, DeviceAccountingPrisets.SA942MBitData, DeviceAccountingPrisets.SA942MStopBit, DeviceAccountingPrisets.SA942MParity),
        PortDeviceSetting(DeviceAccountingPrisets.SA943Speed, DeviceAccountingPrisets.SA943BitData, DeviceAccountingPrisets.SA943StopBit, DeviceAccountingPrisets.SA943Parity),
        PortDeviceSetting(DeviceAccountingPrisets.MKTCSpeed, DeviceAccountingPrisets.MKTCBitData, DeviceAccountingPrisets.MKTCStopBit, DeviceAccountingPrisets.MKTCParity),
        PortDeviceSetting(DeviceAccountingPrisets.CKM2Speed, DeviceAccountingPrisets.CKM2BitData, DeviceAccountingPrisets.CKM2StopBit, DeviceAccountingPrisets.CKM2Parity),
        PortDeviceSetting(DeviceAccountingPrisets.DYMETIC5102Speed, DeviceAccountingPrisets.DYMETIC5102BitData, DeviceAccountingPrisets.DYMETIC5102StopBit, DeviceAccountingPrisets.DYMETIC5102Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TEPLOVACHESLITELTB7Speed, DeviceAccountingPrisets.TEPLOVACHESLITELTB7BitData, DeviceAccountingPrisets.TEPLOVACHESLITELTB7StopBit, DeviceAccountingPrisets.TEPLOVACHESLITELTB7Parity),
        PortDeviceSetting(DeviceAccountingPrisets.ELFSpeed, DeviceAccountingPrisets.ELFBitData, DeviceAccountingPrisets.ELFStopBit, DeviceAccountingPrisets.ELFParity),
        PortDeviceSetting(DeviceAccountingPrisets.STU1Speed, DeviceAccountingPrisets.STU1BitData, DeviceAccountingPrisets.STU1StopBit, DeviceAccountingPrisets.STU1Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TURBOFLOUGFGFSpeed, DeviceAccountingPrisets.TURBOFLOUGFGFBitData, DeviceAccountingPrisets.TURBOFLOUGFGFStopBit, DeviceAccountingPrisets.TURBOFLOUGFGFParity),
        PortDeviceSetting(DeviceAccountingPrisets.EK260Speed, DeviceAccountingPrisets.EK260BitData, DeviceAccountingPrisets.EK260StopBit, DeviceAccountingPrisets.EK260Parity),
        PortDeviceSetting(DeviceAccountingPrisets.EK270Speed, DeviceAccountingPrisets.EK270BitData, DeviceAccountingPrisets.EK270StopBit, DeviceAccountingPrisets.EK270Parity),
        PortDeviceSetting(DeviceAccountingPrisets.BKG2Speed, DeviceAccountingPrisets.BKG2BitData, DeviceAccountingPrisets.BKG2StopBit, DeviceAccountingPrisets.BKG2Parity),
        PortDeviceSetting(DeviceAccountingPrisets.CPG741Speed, DeviceAccountingPrisets.CPG741BitData, DeviceAccountingPrisets.CPG741StopBit, DeviceAccountingPrisets.CPG741Parity),
        PortDeviceSetting(DeviceAccountingPrisets.CPG742Speed, DeviceAccountingPrisets.CPG742BitData, DeviceAccountingPrisets.CPG742StopBit, DeviceAccountingPrisets.CPG742Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TC2015Speed, DeviceAccountingPrisets.TC2015BitData, DeviceAccountingPrisets.TC2015StopBit, DeviceAccountingPrisets.TC2015Parity),
        PortDeviceSetting(DeviceAccountingPrisets.MERCURI230ARTSpeed, DeviceAccountingPrisets.MERCURI230ARTBitData, DeviceAccountingPrisets.MERCURI230ARTStopBit, DeviceAccountingPrisets.MERCURI230ARTParity),
        PortDeviceSetting(DeviceAccountingPrisets.PULSAR2MSpeed, DeviceAccountingPrisets.PULSAR2MBitData, DeviceAccountingPrisets.PULSAR2MStopBit, DeviceAccountingPrisets.PULSAR2MParity),
        PortDeviceSetting(DeviceAccountingPrisets.PULSAR10MSpeed, DeviceAccountingPrisets.PULSAR10MBitData, DeviceAccountingPrisets.PULSAR10MStopBit, DeviceAccountingPrisets.PULSAR10MParity),
        PortDeviceSetting(DeviceAccountingPrisets.KUMIRK21K22Speed, DeviceAccountingPrisets.KUMIRK21K22BitData, DeviceAccountingPrisets.KUMIRK21K22StopBit, DeviceAccountingPrisets.KUMIRK21K22Parity),
        PortDeviceSetting(DeviceAccountingPrisets.IM2300Speed, DeviceAccountingPrisets.IM2300BitData, DeviceAccountingPrisets.IM2300StopBit, DeviceAccountingPrisets.IM2300Parity),
        PortDeviceSetting(DeviceAccountingPrisets.ENERGOMERACE303Speed, DeviceAccountingPrisets.ENERGOMERACE303BitData, DeviceAccountingPrisets.ENERGOMERACE303StopBit, DeviceAccountingPrisets.ENERGOMERACE303Parity),
        PortDeviceSetting(DeviceAccountingPrisets.TEM116Speed, DeviceAccountingPrisets.TEM116BitData, DeviceAccountingPrisets.TEM116StopBit, DeviceAccountingPrisets.TEM116Parity)
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


        // смена настроек usb ---------------------------------------------------
        ConstUsbSettings.speedIndex = 9 // скорость 115200
        usb.flagAtCommandYesNo = true

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

        val enforma1318 = Enforma1318Fragment()
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

    private fun createSettingFragment(fragment: Fragment) {
        if (usb.checkConnectToDevice()) {

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

    private fun workFonDarkMenu() {
        try {
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            transaction.remove(usbComsMenu!!)
            transaction.commit()
        } catch (e: Exception) {}

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


    fun showTimerDialog(usbFragment: UsbFragment, nameTypeDevice: String, flagWrite: Boolean = false) {

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
            showAlertDialog(getString(R.string.ConnectSuccess))
        }
    }

    override fun withdrawalsShow(msg: String, notshowdis: Boolean) {
        showAlertDialog(msg)
    }

    override fun printData(data: String) {

        curentData += data

        // прокурчивание вниз
        binding.ScrollWriteLoadingForDevice.post {
            binding.ScrollWriteLoadingForDevice.scrollTo(0,
                binding.ScrollWriteLoadingForDevice.bottom)
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