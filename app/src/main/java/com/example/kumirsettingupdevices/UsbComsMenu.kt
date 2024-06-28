package com.example.kumirsettingupdevices

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.usb.UsbStaticMethods
import com.example.kumirsettingupdevices.adapters.itemUsbComAdapter.ItemUsbComsAdapter
import com.example.kumirsettingupdevices.databinding.FragmentUsbComsMenuBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemUsbComsView

class UsbComsMenu : Fragment() {
    companion object {
        const val TIMEOUT_VIEW_COM: Long = 10
    }
    private var flagActivThreadComsView: Boolean = true

    private lateinit var showElements: FragmentUsbComsMenuBinding


    // количество доступных com
    var cnt_coms: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        showElements = FragmentUsbComsMenuBinding.inflate(layoutInflater)

        return showElements.root
    }

    override fun onStart() {
        super.onStart()

        // поток для вывода ком портов
        Thread {
            // вывод всех устройств
            while (flagActivThreadComsView) {
                val usbStaticMethods = UsbStaticMethods()
                val usbDeviceNames = usbStaticMethods.getAllDevices(requireContext())

                val usbDeviceItems: List<ItemUsbComsView> = usbDeviceNames.map { name ->
                    ItemUsbComsView(nameUsb = name)
                }

                // провеерка были ли новые подключения
                if (cnt_coms != usbDeviceItems.size) {
                    cnt_coms = usbDeviceItems.size
                    // если да то обновлем адаптер
                    val itemUsbComsAdapter = ItemUsbComsAdapter(requireContext(), usbDeviceItems)

                    // вывод в главном потоке
                    val context: Context = requireContext()
                    (context as Activity).runOnUiThread {
                        if (flagActivThreadComsView) { // дополнительная проверка вдруг поток уже закрыт
                            showElements.comsUsbItem.adapter = itemUsbComsAdapter
                            showElements.comsUsbItem.layoutManager = LinearLayoutManager(requireContext())
                        }

                    }
                }

                // задаержка для более низкой нагрузки
                Thread.sleep(TIMEOUT_VIEW_COM)
            }

        }.start()

    }

    override fun onDestroyView() {
        // закрытие потока для вывода ком портов
        flagActivThreadComsView = false

        // убераем затеменение
        val mainContext: Context = requireContext()
        if (mainContext is MainActivity) {
            mainContext.ActivationFonDarkMenu(false)
        }

        super.onDestroyView()
    }
}