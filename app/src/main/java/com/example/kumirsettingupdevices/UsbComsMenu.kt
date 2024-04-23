package com.example.kumirsettingupdevices

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

    private lateinit var showElements: FragmentUsbComsMenuBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        showElements = FragmentUsbComsMenuBinding.inflate(layoutInflater)

        // вывод всех устройств
        val usbStaticMethods = UsbStaticMethods()
        val usbDeviceNames = usbStaticMethods.getAllDevices(requireContext())

        val usbDeviceItems: List<ItemUsbComsView> = usbDeviceNames.map { name ->
            ItemUsbComsView(nameUsb = name)
        }

        val itemUsbComsAdapter = ItemUsbComsAdapter(requireContext(), usbDeviceItems)
        showElements.comsUsbItem.adapter = itemUsbComsAdapter
        showElements.comsUsbItem.layoutManager = LinearLayoutManager(requireContext())


        return showElements.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // убераем затеменение
        val mainContext: Context = requireContext()
        if (mainContext is MainActivity) {
            mainContext.ActivationFonDarkMenu(false)
        }
    }
}