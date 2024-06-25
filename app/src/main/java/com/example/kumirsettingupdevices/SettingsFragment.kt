package com.example.kumirsettingupdevices

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.example.kumirsettingupdevices.filesMenager.IniFilePmModel
import com.example.kumirsettingupdevices.formaters.ValidDataIniFile
import com.example.kumirsettingupdevices.formaters.ValidDataSettingsDevice
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

// настройки
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    lateinit var contextMain: MainActivity

    var fileName: String = ""

    // текущие данные для выгрузки
    val listIniDataPreset: MutableList<IniFileModel> = mutableListOf()
    val listIninDataPm: MutableList<IniFilePmModel> = mutableListOf()


    companion object {
        private const val REQUEST_CODE = 100
        private const val DIR_PRESETS_DEFAULTE: String = "/priesets"

        // для выгрузки ini файлов
        private const val CNT_TYPE_INI_FILES: Int = 3
        private const val MAX_CNT_TIMEOUT: Int = 20
        private const val TIMEOUT_SAVE_INIFILE: Long = 200
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (it.toString().endsWith(".ini")) {
                fileName = it.toString().reversed().substringBefore("%").reversed().substringBefore(".ini") // нужно для получения имени файла
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
            if (ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE)
            } else
                dischargeIniFiles()
        }
        // закрытие меню
        binding.darckFon.setOnClickListener {
            binding.editPresetSave.visibility = View.GONE
            binding.darckFon.visibility = View.GONE
        }

        binding.inputPath.setText(Environment.DIRECTORY_DOWNLOADS + DIR_PRESETS_DEFAULTE)


        return binding.root
    }

    // функция для активации окна изменения настроек
    fun viewEditMenu(priset: Preset?, pm: Pm?, enfora: Enfora?) {
        // активация окна изменения
        binding.editPresetSave.visibility = View.VISIBLE
        binding.darckFon.visibility = View.VISIBLE

        // убераем все остальное активируем далее
        binding.layoutInputSaveName.visibility = View.GONE
        binding.layoutInputSaveAPN.visibility = View.GONE
        binding.layoutInputSavePort.visibility = View.GONE
        binding.layoutInputSaveLogin.visibility = View.GONE
        binding.layoutInputSavePassword.visibility = View.GONE
        binding.layoutInputSaveServer1.visibility = View.GONE
        binding.layoutInputSaveServer2.visibility = View.GONE
        binding.layoutInputSaveTimeout.visibility = View.GONE
        binding.layoutInputSaveSizeBuffer.visibility = View.GONE
        binding.layoutinputSaveKeyNet.visibility = View.GONE
        binding.layoutInputSavePower.visibility = View.GONE
        binding.spinnerSaveMode.visibility = View.GONE
        binding.spinnerSaveRenge.visibility = View.GONE


        if (priset != null) {
            // m32 m32Lite
            priset.let { preset ->
                // списк режимов работы
                val itemsSpinnerDevMode = listOf(
                    getString(R.string.devmodeKumirNet),
                    getString(R.string.devmodeClient),
                    getString(R.string.devmodeTCPServer),
                    getString(R.string.devmodeGSMmodem),
                    getString(R.string.devmodePipeClient),
                    getString(R.string.devdodePipeServer)
                )
                val adapter = ArrayAdapter(requireContext(),
                    R.layout.item_spinner, itemsSpinnerDevMode)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSaveMode.adapter = adapter

                // подставляем данные для редоктирования
                binding.inputSaveName.setText(preset.name)
                binding.spinnerSaveMode.setSelection(preset.mode!!)
                binding.inputSaveAPN.setText(preset.apn)
                binding.inputSaveServer1.setText(preset.server)
                binding.inputSavePort.setText(preset.port)
                binding.inputSaveLogin.setText(preset.login)
                binding.inputSavePassword.setText(preset.password)

                // активируем нужные поля
                binding.layoutInputSaveName.visibility = View.VISIBLE
                binding.spinnerSaveMode.visibility = View.VISIBLE
                binding.layoutInputSaveAPN.visibility = View.VISIBLE
                binding.layoutInputSaveServer1.visibility = View.VISIBLE
                binding.layoutInputSavePort.visibility = View.VISIBLE
                binding.layoutInputSaveLogin.visibility = View.VISIBLE
                binding.layoutInputSavePassword.visibility = View.VISIBLE

                // на кнопку сохранить вешаем событие сохренения
                binding.buttonSavePresetEdit.setOnClickListener {
                    // проверка валидности данных
                    val validDataSettingsDevice = ValidDataSettingsDevice()
                    if (validDataSettingsDevice.tcpPortValid(binding.inputSavePort.text.toString())) {
                        lifecycleScope.launch {
                            try {
                                // присеты m32
                                contextMain.presetDao.updateById(
                                    preset.id,
                                    binding.inputSaveName.text.toString(),
                                    binding.spinnerSaveMode.selectedItemPosition,
                                    binding.inputSaveAPN.text.toString(),
                                    binding.inputSaveServer1.text.toString(),
                                    binding.inputSavePort.text.toString(),
                                    binding.inputSaveLogin.text.toString(),
                                    binding.inputSavePassword.text.toString()
                                )
                                // успешно и закрываем
                                (contextMain as Activity).runOnUiThread {
                                    binding.editPresetSave.visibility = View.GONE
                                    binding.darckFon.visibility = View.GONE
                                }
                            } catch (_: Exception) {
                                (contextMain as Activity).runOnUiThread {
                                    contextMain.showAlertDialog(getString(R.string.errorCodeNone))
                                }
                            }

                            //  обновляем данные в памяти
                            updateCurrentMemoryDataBase(preset.name)
                        }
                    } else {
                        contextMain.showAlertDialog(getString(R.string.errorTCPPORT))
                    }
                }
            }
        } else if (enfora != null) {
            // enfora
            enfora.let { curentEnfora ->

                // подставляем нужные поля информауию
                binding.inputSaveName.setText(curentEnfora.name)
                binding.inputSaveAPN.setText(curentEnfora.apn)
                binding.inputSaveLogin.setText(curentEnfora.login)
                binding.inputSavePassword.setText(curentEnfora.password)
                binding.inputSaveServer1.setText(curentEnfora.server1)
                binding.inputSaveServer2.setText(curentEnfora.server2)
                binding.inputSaveTimeout.setText(curentEnfora.timeout)
                binding.inputSaveSizeBuffer.setText(curentEnfora.sizeBuffer)

                // активируем нужные поля
                binding.layoutInputSaveName.visibility = View.VISIBLE
                binding.layoutInputSaveAPN.visibility = View.VISIBLE
                binding.layoutInputSaveLogin.visibility = View.VISIBLE
                binding.layoutInputSavePassword.visibility = View.VISIBLE
                binding.layoutInputSaveServer1.visibility = View.VISIBLE
                binding.layoutInputSaveServer2.visibility = View.VISIBLE
                binding.layoutInputSaveTimeout.visibility = View.VISIBLE
                binding.layoutInputSaveSizeBuffer.visibility = View.VISIBLE

                // на кнопку сохранить вешаем событие сохренения
                binding.buttonSavePresetEdit.setOnClickListener {
                    // проверка валидности данных
                    val validDataSettingsDevice = ValidDataSettingsDevice()
                    if (validDataSettingsDevice.padtoValid(binding.inputSaveTimeout.text.toString()) &&
                        validDataSettingsDevice.padblkValid(binding.inputSaveSizeBuffer.text.toString())) {
                        lifecycleScope.launch {
                            try {
                                // присеты m32
                                contextMain.presetEnforaDao.updateById(
                                    enfora.id,
                                    binding.inputSaveName.text.toString(),
                                    binding.inputSaveAPN.text.toString(),
                                    binding.inputSaveLogin.text.toString(),
                                    binding.inputSavePassword.text.toString(),
                                    binding.inputSaveServer1.text.toString(),
                                    binding.inputSaveServer2.text.toString(),
                                    binding.inputSaveTimeout.text.toString(),
                                    binding.inputSaveSizeBuffer.text.toString()
                                )
                                // успешно и закрываем
                                (contextMain as Activity).runOnUiThread {
                                    binding.editPresetSave.visibility = View.GONE
                                    binding.darckFon.visibility = View.GONE
                                }
                            } catch (_: Exception) {
                                (contextMain as Activity).runOnUiThread {
                                    contextMain.showAlertDialog(getString(R.string.errorCodeNone))
                                }
                            }
                            //  обновляем данные в памяти
                            updateCurrentMemoryDataBase(enfora.name)
                        }
                    } else {
                        contextMain.showAlertDialog(getString(R.string.errorValidPole))
                    }
                }
            }
        } else if (pm != null) {
            pm.let { curentPm ->
                // выборки

                // адаптер для выбора режима работы модема
                val itemsSpinnerDevMode = listOf(
                    getString(R.string.devmodeROUTER),
                    getString(R.string.devmodeCANPROXY),
                    getString(R.string.devmodeRS485),
                    getString(R.string.devmodeMONITOR)
                )
                val adapter = ArrayAdapter(requireContext(),
                    R.layout.item_spinner, itemsSpinnerDevMode)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSaveMode.adapter = adapter

                // адаптер для выбора диопазона
                val itemSelectRange = listOf(
                    getString(R.string.rangeMod1),
                    getString(R.string.rangeMod2),
                    getString(R.string.rangeMod3)
                )
                val adapterSelectRange = ArrayAdapter(requireContext(),
                    R.layout.item_spinner, itemSelectRange)
                adapterSelectRange.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSaveRenge.adapter = adapterSelectRange



                // заполнение полей
                binding.inputSaveName.setText(curentPm.name)
                binding.inputSaveKeyNet.setText(curentPm.keyNet)
                binding.inputSavePower.setText(curentPm.power)
                binding.spinnerSaveMode.setSelection(curentPm.mode)
                binding.spinnerSaveRenge.setSelection(curentPm.diopozone)

                // активация нужных полей
                binding.layoutInputSaveName.visibility = View.VISIBLE
                binding.layoutinputSaveKeyNet.visibility = View.VISIBLE
                binding.layoutInputSavePower.visibility = View.VISIBLE
                binding.spinnerSaveMode.visibility = View.VISIBLE
                binding.spinnerSaveRenge.visibility = View.VISIBLE

                // на кнопку сохранить вешаем событие сохренения
                binding.buttonSavePresetEdit.setOnClickListener {
                    // проверка валидности данных
                    val validDataSettingsDevice = ValidDataSettingsDevice()
                    if (binding.inputSaveKeyNet.text.toString().length < 61 &&
                        validDataSettingsDevice.powerValid(binding.inputSavePower.text.toString())) {
                        lifecycleScope.launch {
                            try {
                                // присеты m32
                                contextMain.presetPmDao.updateById(
                                    pm.id,
                                    binding.inputSaveName.text.toString(),
                                    binding.spinnerSaveMode.selectedItemPosition,
                                    binding.inputSaveKeyNet.text.toString(),
                                    binding.inputSavePower.text.toString(),
                                    binding.spinnerSaveRenge.selectedItemPosition
                                )
                                // успешно и закрываем
                                (contextMain as Activity).runOnUiThread {
                                    binding.editPresetSave.visibility = View.GONE
                                    binding.darckFon.visibility = View.GONE
                                }
                            } catch (_: Exception) {
                                (contextMain as Activity).runOnUiThread {
                                    contextMain.showAlertDialog(getString(R.string.errorCodeNone))
                                }
                            }
                            //  обновляем данные в памяти
                            updateCurrentMemoryDataBase(pm.name)
                        }
                    } else {
                        contextMain.showAlertDialog(getString(R.string.errorValidPole))
                    }
                }
            }
        }
    }


    //--------------------------------Выгрузка--------------------------------

    private fun dischargeIniFiles() {

        val validDataIniFile = ValidDataIniFile()
        val generationFiles = GenerationFiles()

        var dataCntTypeSuc: Int = 0
        // получение данных из базы данных m32
        try {
            // присеты enfora
            lifecycleScope.launch {

                // присеты enfora
                contextMain.presetEnforaDao.getAll().collect { enforaPresets ->

                    for (p in enforaPresets) {
                        listIniDataPreset.add(
                            IniFileModel(
                                p.name!!,
                                "Network setting Enfora1318",
                                p.apn!!,
                                "",
                                p.server1!!,
                                p.password!!,
                                p.login!!,
                                "",
                                p.timeout!!,
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
                    dataCntTypeSuc++
                }
            }

            // присеты m32
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
                                validDataIniFile.getModeMainReverse(p.mode),
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""
                            )
                        )
                    }
                    dataCntTypeSuc++
                }
            }

            // присеты pm
            lifecycleScope.launch {
                // присеты m32
                contextMain.presetPmDao.getAll().collect { pms ->
                    for (p in pms) {
                        listIninDataPm.add(
                            IniFilePmModel(
                                p.name!!,
                                validDataIniFile.getModePmReverse(p.mode),
                                validDataIniFile.getBandPmReverse(p.diopozone),
                                p.power!!,
                                p.keyNet!!
                            )
                        )
                    }
                    dataCntTypeSuc++
                }
            }

            // поток ждет пока данные поступят в listIniDataPreset
            Thread {

                // ожидание получения всех данных
                var cntTime: Int = 0
                while (CNT_TYPE_INI_FILES > dataCntTypeSuc) {
                    cntTime++
                    if (cntTime > MAX_CNT_TIMEOUT) {
                        contextMain.runOnUiThread {
                            contextMain.showAlertDialog(getString(R.string.noSaveFilesTime))
                        }
                        break
                    }
                    Thread.sleep(TIMEOUT_SAVE_INIFILE)
                }

                // генерация файлов и сохранение

                if (listIniDataPreset.isNotEmpty() || listIninDataPm.isNotEmpty()) {
                    contextMain.runOnUiThread {
                        // генерация и сохранение по выбраному пути
                        if (binding.inputPath.text.toString().replace(" ", "").isNotEmpty()) {
                            if (generationFiles.generationIniFiles(listIniDataPreset, binding.inputPath.text.toString(), requireContext()) &&
                                generationFiles.generationIniFilesPm(listIninDataPm, binding.inputPath.text.toString(), requireContext()))
                            {
                                contextMain.showAlertDialog(getString(R.string.yesSaveFiles))

                                // ояищение данных
                                listIniDataPreset.clear()
                            } else {
                                contextMain.showAlertDialog(getString(R.string.noSaveFiles))

                            }

                        } else {
                            contextMain.showAlertDialog(getString(R.string.nonPathSaveFile))
                        }
                    }
                } else { // нету данных
                    contextMain.runOnUiThread {
                        contextMain.showAlertDialog(getString(R.string.noFileSave))
                    }
                }

            }.start()
        } catch (_: Exception) {
            contextMain.showAlertDialog(getString(R.string.noSaveFiles))
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


    // обновления данных в оперативной памяти для корректной работы с редоктированием и удалением данных
    fun updateCurrentMemoryDataBase(nameDel: String?) {
        // загрузка всех присетов из базы данных
        try {
            lifecycleScope.launch {
                // присеты m32
                (context as MainActivity).presetDao.getAll().collect { presets ->
                    for (preset in presets) {
                        // уудаляем ненужный
                        PrisetsValue.prisets.remove(nameDel)

                        PrisetsValue.prisets[preset.name!!] = Priset(
                            preset.name, preset.mode!!, preset.apn!!,
                            preset.port!!, preset.server!!, preset.login!!, preset.password!!
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // загрузка всех присетов enfora
        try {
            lifecycleScope.launch {
                // присеты enfora
                (context as MainActivity).presetEnforaDao.getAll().collect { presets ->
                    for (enforaPreseet in presets) {
                        // уудаляем ненужный
                        PresetsEnforaValue.presets.remove(nameDel)

                        PresetsEnforaValue.presets[enforaPreseet.name!!] =
                            Enfora(
                                0, enforaPreseet.name, enforaPreseet.apn!!,
                                enforaPreseet.login!!, enforaPreseet.password!!,
                                enforaPreseet.server1!!, enforaPreseet.server2!!,
                                enforaPreseet.timeout!!, enforaPreseet.sizeBuffer!!
                            )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // загрузка всех присетов Pm
        try {
            lifecycleScope.launch {
                // присеты enfora
                (context as MainActivity).presetPmDao.getAll().collect { presets ->
                    for (PmPreset in presets) {
                        // уудаляем ненужный
                        PrisetsPmValue.presets.remove(nameDel)

                        PrisetsPmValue.presets[PmPreset.name!!] =
                            Pm(
                                0, PmPreset.name, PmPreset.mode, PmPreset.keyNet!!,
                                PmPreset.power!!, PmPreset.diopozone
                            )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    // дислоговое окно для подтвержадения удаления
    fun showConfirmationDialog(context: Context, name: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(getString(R.string.delFileYesNo) + " - $name")
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, id ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancellation)) { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }
}