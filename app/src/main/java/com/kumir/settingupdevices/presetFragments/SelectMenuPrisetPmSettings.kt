package com.kumir.settingupdevices.presetFragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.adapters.itemPrisetsAdapter.ItemPrisetsPmAdapter
import com.kumir.settingupdevices.dataBasePreset.Pm
import com.kumir.settingupdevices.databinding.FragmentSelectMenuPrisetPmSettingsBinding
import com.kumir.settingupdevices.model.recyclerModel.ItemPresetsPmView
import com.kumir.settingupdevices.settings.PrisetsPmValue
import com.kumir.settingupdevices.usbFragments.PrisetFragment


class SelectMenuPrisetPmSettings(val context: PrisetFragment<Pm>) : Fragment() {

    private lateinit var showElements: FragmentSelectMenuPrisetPmSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        showElements = FragmentSelectMenuPrisetPmSettingsBinding.inflate(layoutInflater)


        // дбавление  присеты
        val itemPresetsView: List<ItemPresetsPmView> = PrisetsPmValue.presets.keys.map { ItemPresetsPmView(it) }

        val itemPresetsPmAdapter = ItemPrisetsPmAdapter(requireContext(), context, itemPresetsView)
        showElements.prisetItem.adapter = itemPresetsPmAdapter
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