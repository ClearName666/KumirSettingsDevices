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
import androidx.drawerlayout.widget.DrawerLayout
import com.example.kumirsettingupdevices.databinding.MainActivityBinding
import com.example.kumirsettingupdevices.usb.Usb
import com.example.kumirsettingupdevices.usb.UsbActivityInterface

class MainActivity() : AppCompatActivity(), UsbActivityInterface {

    override val usb: Usb = Usb(this)
    private lateinit var binding: MainActivityBinding
    private var usbComsMenu: UsbComsMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onDestroy() {
        usb.onDestroy()
        super.onDestroy()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.navigationDevices.visibility == View.VISIBLE) {
            binding.navigationDevices.visibility = View.GONE
            ActivationFonDarkMenu(false)
        } else {
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
        ActivationFonDarkMenu(true)
        binding.navigationDevices.visibility = View.VISIBLE
    }

    // клик конпки выбора usb
    fun onClickUsbComs(view: View) {

        usbComsMenu = UsbComsMenu()

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Новый фрагент
        transaction.replace(binding.fragmentContainerUsbMenu.id, usbComsMenu!!)
        transaction.addToBackStack(null)
        transaction.commit()

        ActivationFonDarkMenu(true)
    }

    // клик по фону уничтожение фрагментов меню
    fun onClickFonDarkMenu(view: View) {
        try {
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            transaction.remove(usbComsMenu!!)
            transaction.commit()
        } catch (e: Exception) {}

        binding.navigationDevices.visibility = View.GONE
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