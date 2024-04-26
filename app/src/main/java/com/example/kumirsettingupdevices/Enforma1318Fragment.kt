package com.example.kumirsettingupdevices

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kumirsettingupdevices.databinding.FragmentEnforma1318Binding
import com.example.kumirsettingupdevices.usb.UsbActivityInterface


class Enforma1318Fragment : Fragment() {

    private lateinit var binding: FragmentEnforma1318Binding
    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 300
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentEnforma1318Binding.inflate(requireActivity().layoutInflater)

        // поток считывания сериного номера и прошивки
        Thread {
            val context: Context = requireActivity()
            if (context is MainActivity) {

                // серйный номер-------------------------------------------------
                context.usb.writeDevice(getString(R.string.conmmandGetSerialNum), false)

                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                // если данные поступили то выводим в серийный номер
                if (context.curentData.isNotEmpty()) {
                    (context as Activity).runOnUiThread {
                        val dataPrint: String =
                            getString(R.string.serinerNumber) +
                            context.curentData

                        binding.serinerNumber.text = dataPrint
                        context.curentData = ""
                    }
                } else {
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog(getString(R.string.identifyDeviceFailed))
                    }
                }

                // версия прошивки-------------------------------------------------
                context.usb.writeDevice(getString(R.string.conmmandGetVersionFirmware), false)

                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                // если данные поступили то выводим в серийный номер
                if (context.curentData.isNotEmpty()) {
                    (context as Activity).runOnUiThread {
                        val dataPrint: String =
                            getString(R.string.serinerNumber) +
                                    context.curentData

                        binding.textVersionFirmware.text = dataPrint
                        context.curentData = ""
                    }
                } else {
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog(getString(R.string.identifyDeviceFailed))
                    }
                }

            }
        }.start()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_enforma1318, container, false)
    }



}