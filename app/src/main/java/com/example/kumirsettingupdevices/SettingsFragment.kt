package com.example.kumirsettingupdevices

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsDataAdapter
import com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsEnforaDataAdapter
import com.example.kumirsettingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsPmDataAdapter
import com.example.kumirsettingupdevices.dataBasePreset.Enfora
import com.example.kumirsettingupdevices.dataBasePreset.Pm
import com.example.kumirsettingupdevices.dataBasePreset.Preset
import com.example.kumirsettingupdevices.databinding.FragmentSettingsBinding
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSettingPreset
import com.example.kumirsettingupdevices.model.recyclerModel.Priset
import com.example.kumirsettingupdevices.settings.PresetsEnforaValue
import com.example.kumirsettingupdevices.settings.PrisetsPmValue
import com.example.kumirsettingupdevices.settings.PrisetsValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

// настройки
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    lateinit var contextMain: MainActivity

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (it.toString().endsWith(".ini")) {
                readIniFileContent(it)
            } else {
                contextMain.showAlertDialog(getString(R.string.nonIniFile))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater)

        val contextMain_: Context = requireContext()
        if (contextMain_ is MainActivity) {
            contextMain = contextMain_
        }

        // обновляем данные в настроках
        updataDataPersetAdapter()
        updataDataPersetEnforaAdapter() // enfora
        updataDataPersetPmAdapter() // Pm

        // клики
        binding.imageAddFilePreset.setOnClickListener {
            selectFile()
        }

        return binding.root
    }

    //--------------------------------Выбрать файл----------------------------
    private fun selectFile() {
        getContent.launch("*/*") // Можно указать конкретный тип файла, например, "application/octet-stream"
    }

    private fun readIniFileContent(uri: Uri) {
        val context: Context? = context
        context?.let {
            val inputStream = it.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = StringBuilder()
            var line: String?

            try {
                while (reader.readLine().also { line = it } != null) {
                    content.append(line).append("\n")
                }
                parseIniContent(content.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
            }
        }
    }

    private fun parseIniContent(iniContent: String) {
        val properties = Properties()
        properties.load(iniContent.reader())

        // 1 тип
        try {
            if (properties.getProperty("TYPE") == "1") {
                val title = properties.getProperty("TITLE")
                val apn = properties.getProperty("APN")
                val mode = properties.getProperty("MODE")
                val tcpPort = properties.getProperty("TCP_PORT")
                val server1 = properties.getProperty("SERVER1")
                val login = properties.getProperty("LOGIN")
                val password = properties.getProperty("PASSWORD")

                // Используйте полученные значения
                val preset = Preset(0,title, mode.toInt() - 1, apn, server1, tcpPort, login, password)

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        contextMain.presetDao.insert(preset)
                        contextMain.runOnUiThread {
                            contextMain.showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                            // добавление в оперативнку
                            PrisetsValue.prisets[title] = Priset(title, mode.toInt() - 1, apn, tcpPort, server1, login, password)
                            // обноление данныех
                            updataDataPersetAdapter()
                        }
                    }
                }
            } else if (properties.getProperty("TYPE") == "2") {
                val title = properties.getProperty("TITLE")
                val apn = properties.getProperty("APN")
                val server1 = properties.getProperty("SERVER1")
                val server2 = properties.getProperty("SERVER2")
                val login = properties.getProperty("LOGIN")
                val password = properties.getProperty("PASSWORD")
                val timeout = properties.getProperty("TIMEOUT")
                val sizeBuffer = properties.getProperty("SIZEBUFFER")

                // Используйте полученные значения
                val preset = Enfora(0, title, apn, login, password, server1, server2, timeout, sizeBuffer)

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        contextMain.presetEnforaDao.insert(preset)
                        contextMain.runOnUiThread {
                            contextMain.showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                            // добавление в оперативнку
                            PresetsEnforaValue.presets[title] = preset
                            // обноление данныех
                            updataDataPersetEnforaAdapter()
                        }
                    }
                }
            } else if (properties.getProperty("TYPE") == "3") {
                val title = properties.getProperty("TITLE")
                val mode = properties.getProperty("MODE")
                val keyNet = properties.getProperty("KEYNET")
                val power = properties.getProperty("POWER")
                val diopozone = properties.getProperty("RANGE")

                // Используйте полученные значения
                val preset = Pm(0, title, mode.toInt() - 1, keyNet, power, diopozone.toInt() - 1)

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        contextMain.presetPmDao.insert(preset)
                        contextMain.runOnUiThread {
                            contextMain.showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                            // добавление в оперативнку
                            PrisetsPmValue.presets[title] = preset
                            // обноление данныех
                            updataDataPersetPmAdapter()
                        }
                    }
                }
            } else {
                contextMain.showAlertDialog(getString(R.string.nonValidIniFile))
            }
        } catch (e: Exception) {
            contextMain.showAlertDialog(getString(R.string.nonValidIniFile))
        }

    }
    //------------------------------------------------------------------------

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