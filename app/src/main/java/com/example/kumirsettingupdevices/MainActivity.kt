package com.example.kumirsettingupdevices

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.kumirsettingupdevices.databinding.MainActivityBinding
import com.example.kumirsettingupdevices.usb.Usb
import com.example.kumirsettingupdevices.usb.UsbActivityInterface
import com.example.testappusb.settings.ConstUsbSettings

class MainActivity : AppCompatActivity(), UsbActivityInterface {

    override val usb: Usb = Usb(this)
    private lateinit var binding: MainActivityBinding

    private var usbComsMenu: UsbComsMenu? = null
    var curentData: String = ""



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

        val m32 = M31Fragment()
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


    override fun showDeviceName(deviceName: String) {
        binding.textDeviceName.text = deviceName
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
        curentData = data
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