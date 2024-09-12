
package com.kumir.settingupdevices.presetFragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.adapters.itemPrisetsAdapter.ItemPrisetsEnforaAdapter
import com.kumir.settingupdevices.dataBasePreset.Enfora
import com.kumir.settingupdevices.databinding.FragmentSelectMenuPrisetEnforaSettingsBinding
import com.kumir.settingupdevices.model.recyclerModel.ItemPresetsEnforaView
import com.kumir.settingupdevices.settings.PresetsEnforaValue
import com.kumir.settingupdevices.usbFragments.PrisetFragment


class SelectMenuPrisetEnforaSettings(val context: PrisetFragment<Enfora>) : Fragment() {

    private lateinit var showElements: FragmentSelectMenuPrisetEnforaSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        showElements = FragmentSelectMenuPrisetEnforaSettingsBinding.inflate(layoutInflater)


        // дбавление  присеты
        val itemPresetsView: List<ItemPresetsEnforaView> = PresetsEnforaValue.presets.keys
            .filter { it.trim().isNotBlank() }
            .map { ItemPresetsEnforaView(it) }

        val itemPresetsAdapter = ItemPrisetsEnforaAdapter(requireContext(), context, itemPresetsView)
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