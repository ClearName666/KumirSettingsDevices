package com.example.kumirsettingupdevices

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.OpenableColumns
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
import com.example.kumirsettingupdevices.filesMenager.GenerationFiles
import com.example.kumirsettingupdevices.filesMenager.IniFileModel
import com.example.kumirsettingupdevices.filesMenager.SaveDataFile
import com.example.kumirsettingupdevices.formaters.ValidDataIniFile
import com.example.kumirsettingupdevices.model.recyclerModel.ItemSettingPreset
import com.example.kumirsettingupdevices.model.recyclerModel.Priset
import com.example.kumirsettingupdevices.settings.PresetsEnforaValue
import com.example.kumirsettingupdevices.settings.PrisetsPmValue
import com.example.kumirsettingupdevices.settings.PrisetsValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ini4j.Ini
import org.ini4j.Profile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

// настройки
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    lateinit var contextMain: MainActivity

    var fileName: String = ""

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (it.toString().endsWith(".ini")) {
                fileName = it.toString().substringAfter("/")
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
        binding.imageDischarge.setOnClickListener {
            dischargeIniFiles()
        }

        binding.inputPath.setText("/storage/emulated/0/")


        return binding.root
    }

    //--------------------------------Выгрузка--------------------------------
    private fun dischargeIniFiles() {
        val listIniDataPreset: MutableList<IniFileModel> = mutableListOf()

        val generationFiles = GenerationFiles()
        val saveDataFile = SaveDataFile()

        // получение данных из базы данных m32
        try {
            lifecycleScope.launch {
                // присеты m32
                contextMain.presetDao.getAll().collect { presets ->
                    for (p in presets) {
                        listIniDataPreset.add(
                            IniFileModel(
                                p.name!!,
                                "Network setting M32",
                                p.apn!!,
                                p.port!!,
                                p.server!!,
                                p.password!!,
                                p.login!!,
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // генерация и сохранение по выбраному пути
        if (binding.inputPath.text.toString().replace(" ", "").isNotEmpty()) {
            if (saveDataFile.saveToExternalStorage(
                    generationFiles.generationIniFiles(listIniDataPreset),
                    binding.inputPath.text.toString()
                ))
            {
                contextMain.showAlertDialog(getString(R.string.yesSaveFiles))
            } else {
                contextMain.showAlertDialog(getString(R.string.noSaveFiles))

            }

        } else {
            contextMain.showAlertDialog(getString(R.string.nonPathSaveFile))
        }


    }
    //------------------------------------------------------------------------



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
        val ini = Ini()
        ini.load(iniContent.reader())

        // для проверки валидности и перевода в данные для приложения
        val validDataIniFile = ValidDataIniFile()

        // Пример: доступ к параметрам секции [Network setting M32]
        val networkM32Settings: Profile.Section? = ini["Network setting M32"]
        val networkACCB030CoreSettings: Profile.Section? = ini["Network setting ASSV030"]
        val networkRM81Settings: Profile.Section? = ini["Network setting RM81"]

        networkM32Settings?.let {
            val apn = it["Apn"]
            val tcpPort = it["TCPPort"]
            val eServer = it["eServer"]
            val password = it["Password"]
            val login = it["Login"]
            val devMode = it["DevMode"]

            // проверка на наличие иначе отваливаемся
            val mode = validDataIniFile.getModeMain(devMode)
            if (mode == -1 || apn == null || tcpPort == null || eServer == null ||
                password == null || login == null || devMode == null) {

                contextMain.showAlertDialog(getString(R.string.nonValidIniFile))
                return
            }

            // запись в базу данных
            val preset = Preset(0, fileName, mode, apn, eServer, tcpPort, login, password)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    contextMain.presetDao.insert(preset)
                    contextMain.runOnUiThread {
                        contextMain.showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                        // добавление в оперативнку
                        PrisetsValue.prisets[fileName] = Priset(fileName, mode, apn, tcpPort, eServer, login, password)
                        // обноление данныех
                        updataDataPersetAdapter()
                    }
                }
            }
        }

        networkACCB030CoreSettings?.let {
            val apn = it["Apn"]
            val tcpPort = " "
            val eServer = it["Server"]
            val password = it["Password"]
            val login = it["Login"]

            // проверка на наличие иначе отваливаемся
            if (apn == null || eServer == null || password == null || login == null) {
                contextMain.showAlertDialog(getString(R.string.nonValidIniFile))
                return
            }

            // запись в базу данных
            val preset = Preset(0, fileName, 0, apn, eServer, tcpPort, login, password)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    contextMain.presetDao.insert(preset)
                    contextMain.runOnUiThread {
                        contextMain.showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                        // добавление в оперативнку
                        PrisetsValue.prisets[fileName] = Priset(fileName, 0, apn, tcpPort, eServer, login, password)
                        // обноление данныех
                        updataDataPersetAdapter()
                    }
                }
            }
        }

        networkRM81Settings?.let {
            val power = it["Power"]
            val band = it["Band"]
            val accessKey = it["AccessKey"]
            val devMode = it["Mode"]

            // проверка на наличие иначе отваливаемся
            val mode = validDataIniFile.getModePm(devMode)
            val bandI = validDataIniFile.getBandPm(band)
            if (mode == -1 || band == null || accessKey == null || power == null || devMode == null) {
                contextMain.showAlertDialog(getString(R.string.nonValidIniFile))
                return
            }

            // запись в базу данных
            val preset = Pm(0, fileName, mode, accessKey, power, bandI)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    contextMain.presetPmDao.insert(preset)
                    contextMain.runOnUiThread {
                        contextMain.showAlertDialog(getString(R.string.sucPresetSaveDataBase))
                        // добавление в оперативнку
                        PrisetsPmValue.presets[fileName] = preset
                        // обноление данныех
                        updataDataPersetPmAdapter()
                    }
                }
            }
        }

        fileName = ""

        // не одна секция не найдена
        if (networkM32Settings == null && networkACCB030CoreSettings == null && networkRM81Settings == null)
            contextMain.showAlertDialog(getString(R.string.nonValidIniFile))

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