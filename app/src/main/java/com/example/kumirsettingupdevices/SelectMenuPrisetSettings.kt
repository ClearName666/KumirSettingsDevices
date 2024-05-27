package com.example.kumirsettingupdevices

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.adapters.itemUsbComAdapter.ItemUsbComsAdapter
import com.example.kumirsettingupdevices.databinding.FragmentSelectMenuPrisetSettingsBinding
import com.example.kumirsettingupdevices.databinding.FragmentUsbComsMenuBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemUsbComsView
import com.example.kumirsettingupdevices.usb.UsbStaticMethods


class SelectMenuPrisetSettings : Fragment() {

    private lateinit var showElements: FragmentSelectMenuPrisetSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        showElements = FragmentSelectMenuPrisetSettingsBinding.inflate(layoutInflater)

        // вывод всех присетов

        /*val usbDeviceItems: List<ItemUsbComsView> = listOf(
            ItemUsbComsView(context.getString(R.string.priset1)),
            ItemUsbComsView(context.getString(R.string.priset2)),
            ItemUsbComsView(context.getString(R.string.priset3)),
            ItemUsbComsView(context.getString(R.string.priset4)),
            ItemUsbComsView(context.getString(R.string.priset5))
        )

        val itemUsbComsAdapter = ItemUsbComsAdapter(requireContext(), usbDeviceItems)
        showElements.prisetItem.adapter = itemUsbComsAdapter
        showElements.prisetItem.layoutManager = LinearLayoutManager(requireContext())*/


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