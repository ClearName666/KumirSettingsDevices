
package com.example.kumirsettingupdevices.presetFragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.adapters.itemPrisetsAdapter.ItemPrisetsEnforaAdapter
import com.example.kumirsettingupdevices.dataBasePreset.Enfora
import com.example.kumirsettingupdevices.databinding.FragmentSelectMenuPrisetEnforaSettingsBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemPresetsEnforaView
import com.example.kumirsettingupdevices.settings.PresetsEnforaValue
import com.example.kumirsettingupdevices.usbFragments.PrisetFragment


class SelectMenuPrisetEnforaSettings(val context: PrisetFragment<Enfora>) : Fragment() {

    private lateinit var showElements: FragmentSelectMenuPrisetEnforaSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        showElements = FragmentSelectMenuPrisetEnforaSettingsBinding.inflate(layoutInflater)


        // дбавление  присеты
        val itemPresetsView: List<ItemPresetsEnforaView> = PresetsEnforaValue.presets.keys.map { ItemPresetsEnforaView(it) }

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