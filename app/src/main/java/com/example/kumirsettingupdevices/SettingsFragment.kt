package com.example.kumirsettingupdevices

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsDataAdapter
import com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsEnforaDataAdapter
import com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsPmDataAdapter
import com.example.kumirsettingupdevices.databinding.FragmentSettingsBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSettingPreset
import kotlinx.coroutines.launch

// настройки
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater)

        // обновляем данные в настроках
        updataDataPersetAdapter()
        updataDataPersetEnforaAdapter() // enfora
        updataDataPersetPmAdapter() // Pm

        return binding.root
    }

    // вывод данных для m32 m32Lite
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
        } catch (_: Exception) {}
    }

    // вывод данных для enfora
    fun updataDataPersetEnforaAdapter() {
        try {
            lifecycleScope.launch {
                val context: Context = requireContext()
                if (context is MainActivity) {

                    // получаем все данные шаблонов
                    context.presetEnforaDao.getAll().collect { presets ->

                        // если нечего нет то выводит данных нет
                        if (presets.isEmpty()) {
                            val itemSettingPreset: List<ItemSettingPreset> = listOf()

                            (context as Activity).runOnUiThread {
                                binding.textNonDataPresetEnfora.visibility = View.VISIBLE

                                // очищение адаптера то что записей больше нет
                                val itemPresetSettingsEnforaDataAdapter =
                                    ItemPresetSettingsEnforaDataAdapter(
                                        context, this@SettingsFragment,
                                        context.presetEnforaDao, itemSettingPreset
                                    )
                                binding.reclyclerDataPresetEnfora.adapter = itemPresetSettingsEnforaDataAdapter
                                binding.reclyclerDataPresetEnfora.layoutManager =
                                    LinearLayoutManager(requireContext())
                            }
                        } else {
                            // получаем все шаблоны
                            val itemSettingPreset: List<ItemSettingPreset> = presets.map {
                                ItemSettingPreset(it.name!!, it.id)
                            }
                            (context as Activity).runOnUiThread {
                                val itemPresetSettingsEnforaDataAdapter =
                                    ItemPresetSettingsEnforaDataAdapter(
                                        context, this@SettingsFragment,
                                        context.presetEnforaDao, itemSettingPreset
                                    )
                                binding.reclyclerDataPresetEnfora.adapter = itemPresetSettingsEnforaDataAdapter
                                binding.reclyclerDataPresetEnfora.layoutManager =
                                    LinearLayoutManager(requireContext())
                            }
                        }
                    }

                }
            }
        } catch (_: Exception) {}
    }
    // вывод данных для Pm
    fun updataDataPersetPmAdapter() {
        try {
            lifecycleScope.launch {
                val context: Context = requireContext()
                if (context is MainActivity) {

                    // получаем все данные шаблонов
                    context.presetPmDao.getAll().collect { presets ->

                        // если нечего нет то выводит данных нет
                        if (presets.isEmpty()) {
                            val itemSettingPreset: List<ItemSettingPreset> = listOf()

                            (context as Activity).runOnUiThread {
                                binding.textNonDataPresetPm.visibility = View.VISIBLE

                                // очищение адаптера то что записей больше нет
                                val itemPresetSettingsPmDataAdapter =
                                    ItemPresetSettingsPmDataAdapter(
                                        context, this@SettingsFragment,
                                        context.presetPmDao, itemSettingPreset
                                    )
                                binding.reclyclerDataPresetPm.adapter = itemPresetSettingsPmDataAdapter
                                binding.reclyclerDataPresetPm.layoutManager =
                                    LinearLayoutManager(requireContext())
                            }
                        } else {
                            // получаем все шаблоны
                            val itemSettingPreset: List<ItemSettingPreset> = presets.map {
                                ItemSettingPreset(it.name!!, it.id)
                            }
                            (context as Activity).runOnUiThread {
                                val itemPresetSettingsPmDataAdapter =
                                    ItemPresetSettingsPmDataAdapter(
                                        context, this@SettingsFragment,
                                        context.presetPmDao, itemSettingPreset
                                    )
                                binding.reclyclerDataPresetPm.adapter = itemPresetSettingsPmDataAdapter
                                binding.reclyclerDataPresetPm.layoutManager =
                                    LinearLayoutManager(requireContext())
                            }
                        }
                    }

                }
            }
        } catch (_: Exception) {}
    }
}