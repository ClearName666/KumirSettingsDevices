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
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.example.kumirsettingupdevices.databinding.MainActivityBinding
import com.example.kumirsettingupdevices.usb.Usb
import com.example.kumirsettingupdevices.usb.UsbActivityInterface

class MainActivity : AppCompatActivity(), UsbActivityInterface {

    override val usb: Usb = Usb(this)
    private lateinit var binding: MainActivityBinding

    private var usbComsMenu: UsbComsMenu? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mainFragment = MainFragment()

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Новый фрагент
        transaction.replace(binding.fragmentContainerMainContent.id, mainFragment)
        transaction.addToBackStack("MainFragment")
        transaction.commit()

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

        } /*else if (supportFragmentManager.getBackStackEntryAt(0).name != "MainFragment") {

        } */else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.mainActivityExitCheck))

            builder.setPositiveButton(getString(R.string.Yes)) { dialog, _ ->
                dialog.dismiss()
                super.onBackPressed()
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
        transaction.addToBackStack("UsbComsMenu")
        transaction.commit()

        ActivationFonDarkMenu(true)
    }




    // ----------------клики в раскрывающимся меню-------------------

    fun onClickEnforma1318(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)

        if (usb.checkConnectToDevice()) {
            val enforma1318 = Enforma1318Fragment()

            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            // Новый фрагент
            transaction.replace(binding.fragmentContainerMainContent.id, enforma1318)
            transaction.addToBackStack("Enforma1318Fragment")
            transaction.commit()
        } else {
            showAlertDialog(getString(R.string.UsbNoneConnectDevice))
        }
    }
    fun onClickM31(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickK21K23(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickM32(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickM32Lite(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickA61(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickPM81(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickACCB030Core(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickACCB030(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
    }
    fun onClickP101(view: View) {
        binding.drawerMenuSelectTypeDevice.closeDrawer(GravityCompat.START)
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
    private fun showAlertDialog(msg: String) {
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

    }

    override fun withdrawalsShow(msg: String, notshowdis: Boolean) {
        showAlertDialog(msg)
    }

    override fun printData(data: String) {

    }

    override fun printDSR_CTS(dsr: Int, cts: Int) {

    }

    override fun disconnected() {

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