package com.kumir.settingupdevices.presetFragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.adapters.itemPrisetsAdapter.ItemPrisetsAdapter
import com.kumir.settingupdevices.databinding.FragmentSelectMenuPrisetSettingsBinding
import com.kumir.settingupdevices.model.recyclerModel.ItemPrisetsView
import com.kumir.settingupdevices.model.recyclerModel.Priset
import com.kumir.settingupdevices.settings.PrisetsValue
import com.kumir.settingupdevices.usbFragments.PrisetFragment


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