package com.example.kumirsettingupdevices

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.adapters.itemOperatorAdapter.ItemOperatorAdapter
import com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsDataAdapter
import com.example.kumirsettingupdevices.databinding.FragmentSettingsBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSettingPreset
import com.example.kumirsettingupdevices.model.recyclerModel.Priset
import com.example.kumirsettingupdevices.settings.PrisetsValue
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater)

        // обновляем данные в настроках
        updataDataPersetAdapter()

        return binding.root
    }

    fun updataDataPersetAdapter() {
        try {
            lifecycleScope.launch {
                val context: Context = requireContext()
                if (context is MainActivity) {
                    // получаем все данные шаблонов
                    context.presetDao.getAll().collect { presets ->
                        // если нечего нет то выводит данных нет
                        if (presets.isEmpty()) {
                            val itemSettingPreset: List<ItemSettingPreset> = listOf()

                                (context as Activity).runOnUiThread {
                                binding.textNonDataPreset.visibility = View.VISIBLE

                                    // очищение адаптера то что записей больше нет
                                    val itemPresetSettingsDataAdapter =
                                        ItemPresetSettingsDataAdapter(
                                            context, this@SettingsFragment,
                                            context.presetDao, itemSettingPreset
                                        )
                                    binding.reclyclerDataPreset.adapter = itemPresetSettingsDataAdapter
                                    binding.reclyclerDataPreset.layoutManager =
                                        LinearLayoutManager(requireContext())
                            }
                        } else {
                            // получаем все шаблоны
                            val itemSettingPreset: List<ItemSettingPreset> = presets.map {
                                ItemSettingPreset(it.name!!, it.id)
                            }
                            (context as Activity).runOnUiThread {
                                val itemPresetSettingsDataAdapter =
                                    ItemPresetSettingsDataAdapter(
                                        context, this@SettingsFragment,
                                        context.presetDao, itemSettingPreset
                                    )
                                binding.reclyclerDataPreset.adapter = itemPresetSettingsDataAdapter
                                binding.reclyclerDataPreset.layoutManager =
                                    LinearLayoutManager(requireContext())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {}
    }
}