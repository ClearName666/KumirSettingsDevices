package com.example.kumirsettingupdevices.presetFragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.adapters.itemPrisetsAdapter.ItemPrisetsAdapter
import com.example.kumirsettingupdevices.databinding.FragmentSelectMenuPrisetSettingsBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemPrisetsView
import com.example.kumirsettingupdevices.model.recyclerModel.Priset
import com.example.kumirsettingupdevices.settings.PrisetsValue
import com.example.kumirsettingupdevices.usbFragments.PrisetFragment


class SelectMenuPrisetSettings(val context: PrisetFragment<Priset>) : Fragment() {

    private lateinit var showElements: FragmentSelectMenuPrisetSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        showElements = FragmentSelectMenuPrisetSettingsBinding.inflate(layoutInflater)


        // дбавление  присеты
        val itemPresetsView: List<ItemPrisetsView> = PrisetsValue.prisets.keys.map { ItemPrisetsView(it) }

        val itemPresetsAdapter = ItemPrisetsAdapter(requireContext(), context, itemPresetsView)
        showElements.prisetItem.adapter = itemPresetsAdapter
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