package com.example.kumirsettingupdevices

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.adapters.itemPrisetsAdapter.ItemPrisetsAdapter
import com.example.kumirsettingupdevices.databinding.FragmentSelectMenuPrisetSettingsBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemPrisetsView
import com.example.kumirsettingupdevices.settings.PrisetsValue
import com.example.kumirsettingupdevices.usbFragments.PrisetFragment


class SelectMenuPrisetSettings(val context: PrisetFragment) : Fragment() {

    private lateinit var showElements: FragmentSelectMenuPrisetSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        showElements = FragmentSelectMenuPrisetSettingsBinding.inflate(layoutInflater)

        // вывод всех присетов

        val itemPrisetsView: List<ItemPrisetsView> = PrisetsValue.prisets.keys.map { ItemPrisetsView(it) }


        val itemPrisetsAdapter = ItemPrisetsAdapter(requireContext(), context, itemPrisetsView)
        showElements.prisetItem.adapter = itemPrisetsAdapter
        showElements.prisetItem.layoutManager = LinearLayoutManager(requireContext())


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