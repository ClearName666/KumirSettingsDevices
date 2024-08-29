package com.kumir.settingupdevices

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kumir.settingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsDataAdapter
import com.kumir.settingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsEnforaDataAdapter
import com.kumir.settingupdevices.adapters.itemPresetSettingsDataAdapter.ItemPresetSettingsPmDataAdapter
import com.kumir.settingupdevices.dataBasePreset.Enfora
import com.kumir.settingupdevices.dataBasePreset.Pm
import com.kumir.settingupdevices.dataBasePreset.Preset
import com.kumir.settingupdevices.databinding.FragmentSettingsBinding
import com.kumir.settingupdevices.filesManager.GenerationFiles
import com.kumir.settingupdevices.filesManager.IniFileModel
import com.kumir.settingupdevices.filesManager.IniFilePmModel
import com.kumir.settingupdevices.formaters.ValidDataIniFile
import com.kumir.settingupdevices.formaters.ValidDataSettingsDevice
import com.kumir.settingupdevices.model.recyclerModel.ItemSettingPreset
import com.kumir.settingupdevices.model.recyclerModel.Priset
import com.kumir.settingupdevices.settings.PresetsEnforaValue
import com.kumir.settingupdevices.settings.PrisetsPmValue
import com.kumir.settingupdevices.settings.PrisetsValue
import com.google.android.material.tabs.TabLayout
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
        private const val DIR_PRESETS_DEFAULTE: String = "/presets"

        // для выгрузки ini файлов
        private const val CNT_TYPE_INI_FILES: Int = 3
        private const val MAX_CNT_TIMEOUT: Int = 20
        private const val TIMEOUT_SAVE_INIFILE: Long = 200


        private const val REQUEST_CODE_PERMISSIONS: Int = 200
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (getFileNameFromUri(uri, true).endsWith(".ini")) {
                contextMain.showAlertDialog(getString(R.string.nonIniFile) + " Ваш путь: $it")
                fileName = getFileNameFromUri(it)
                readIniFileContent(it)
            } else {
                contextMain.showAlertDialog(getString(R.string.nonIniFile) + " Ваш путь: $it")
            }
        }
    }

    // полуение имени файла по пути
    private fun getFileNameFromUri(uri: Uri, flagYesIniEnd: Boolean = false): String {
        var fileName = ""
        uri.let {
            val cursor = requireContext().contentResolver.query(it, null, null, null, null)
            cursor?.use {
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (flagYesIniEnd) return fileName

        return fileName.dropLast(".ini".length) // что бы убрать ini в конце названия
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


        // установка даных в tab layout
        binding.tabPresets.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.scrollViewM32m32Lite.visibility = View.VISIBLE
                        binding.scrollViewEnfora.visibility = View.GONE
                        binding.scrollViewPm.visibility = View.GONE
                    }
                    1 -> {
                        binding.scrollViewM32m32Lite.visibility = View.GONE
                        binding.scrollViewEnfora.visibility = View.VISIBLE
                        binding.scrollViewPm.visibility = View.GONE
                    }
                    2 -> {
                        binding.scrollViewM32m32Lite.visibility = View.GONE
                        binding.scrollViewEnfora.visibility = View.GONE
                        binding.scrollViewPm.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No-op
            }
        })

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

                // сохраняем имя для того что бы в случае если оно будет пустым заменить его на старое
                val name: String? = preset.name

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
                    if (validPreset()) {
                        lifecycleScope.launch {
                            try {
                                // присеты m32
                                contextMain.presetDao.updateById(
                                    preset.id,
                                    if (binding.inputSaveName.text.toString().replace(" ", "").isEmpty())
                                        name!! else binding.inputSaveName.text.toString(),
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

                // сохраняем имя для того что бы в случае если оно будет пустым заменить его на старое
                val name: String? = curentEnfora.name

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
                    if (validEnfora()) {
                        lifecycleScope.launch {
                            try {
                                // присеты m32
                                contextMain.presetEnforaDao.updateById(
                                    enfora.id,
                                    if (binding.inputSaveName.text.toString().replace(" ", "").isEmpty())
                                        name!! else binding.inputSaveName.text.toString(),
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

                val name: String? = curentPm.name

                // активация нужных полей
                binding.layoutInputSaveName.visibility = View.VISIBLE
                binding.layoutinputSaveKeyNet.visibility = View.VISIBLE
                binding.layoutInputSavePower.visibility = View.VISIBLE
                binding.spinnerSaveMode.visibility = View.VISIBLE
                binding.spinnerSaveRenge.visibility = View.VISIBLE

                // на кнопку сохранить вешаем событие сохренения
                binding.buttonSavePresetEdit.setOnClickListener {
                    // проверка валидности данных
                    if (validPm()) {
                        lifecycleScope.launch {
                            try {
                                // присеты m32
                                contextMain.presetPmDao.updateById(
                                    pm.id,
                                    if (binding.inputSaveName.text.toString().replace(" ", "").isEmpty())
                                        name!! else binding.inputSaveName.text.toString(),
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
                    }
                }
            }
        }
    }


    //--------------------------------Выгрузка--------------------------------

    private fun dischargeIniFiles() {

        // если нету разрешения то выходим
        if (!chackPermissionMember()) return

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

    private fun chackPermissionMember(): Boolean {
        return if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Permissions are not granted, request them
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
            false
        } else {
            true
        }
    }
    //------------------------------------------------------------------------



    //--------------------------------Выбрать файл----------------------------
    private fun selectFile() {
        getContent.launch("*/*") // Можно указать конкретный тип файла, например, "application/octet-stream"
    }

    private fun readIniFileContent(uri: Uri) {
        val context: Context = requireContext()
        context.let {
            try {
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
            } catch (e: Exception) {
                showAlertDialog(getString(R.string.fileError))
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
        val networkEnforaSettings: Profile.Section? = ini["Network setting Enfora1318"]

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
                    contextMain.presetDao.upsert(preset, requireContext() as MainActivity)
                    contextMain.runOnUiThread {
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
                    contextMain.presetDao.upsert(preset, requireContext() as MainActivity)
                    contextMain.runOnUiThread {
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
                    contextMain.presetPmDao.upsert(preset, requireContext() as MainActivity)
                    contextMain.runOnUiThread {
                        // добавление в оперативнку
                        PrisetsPmValue.presets[fileName] = preset
                        // обноление данныех
                        updataDataPersetPmAdapter()
                    }
                }
            }
        }

        networkEnforaSettings.let {
            if (it != null) {
                val apn = it["Apn"]
                val eServer = it["eServer"]
                val password = it["Password"]
                val login = it["Login"]
                val timeOut = it["Timeout"]


                // проверка на наличие иначе отваливаемся
                if (apn == null || eServer == null || password == null || login == null || timeOut == null) {
                    contextMain.showAlertDialog(getString(R.string.nonValidIniFile))
                    return
                }

                // запись в базу данных
                val preset = Enfora(0, fileName, apn, login, password, eServer, "", timeOut, "512")
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        contextMain.presetEnforaDao.upsert(preset, requireContext() as MainActivity)
                        contextMain.runOnUiThread {
                            // добавление в оперативнку
                            PresetsEnforaValue.presets[fileName] = preset
                            // обноление данныех
                            updataDataPersetEnforaAdapter()
                        }
                    }
                }
            }

        }

        fileName = ""

        // не одна секция не найдена
        if (networkM32Settings == null && networkACCB030CoreSettings == null && networkRM81Settings == null && networkEnforaSettings == null)
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

                            // убераем надпись что данных нет
                            binding.textNonDataPreset.visibility = View.GONE

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

                            // убераем надпись что данных нет
                            binding.textNonDataPresetEnfora.visibility = View.GONE

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

                            // убераем надпись что данных нет
                            binding.textNonDataPresetPm.visibility = View.GONE

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

    private fun validPreset(): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()

        // проверка на русские символы в серверах и apn
        if (!validDataSettingsDevice.serverValid(binding.inputSaveServer1.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputSaveAPN.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorRussionChar))
            return false
        } else if (!validDataSettingsDevice.tcpPortValid(binding.inputSavePort.text.toString().replace("\\s+".toRegex(), ""))) {
            contextMain.showAlertDialog(getString(R.string.errorTCPPORT))
            return false
        }

        // проверки на вaлидность 63 символа
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputSaveAPN.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorValidAPN))
            return false
        }
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputSaveServer1.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorValidIPDNS))
            return false
        }
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputSaveLogin.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorValidLogin))
            return false
        }
        if (!validDataSettingsDevice.charPROV_CHAR_MAXValid(binding.inputSavePassword.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorValidPassword))
            return false
        }

        return true
    }

    private fun validEnfora(): Boolean {
        // проверка валидности введенных данных
        val validDataSettingsDevice = ValidDataSettingsDevice()
        if (!validDataSettingsDevice.padtoValid(binding.inputSaveSizeBuffer.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorSizeBuffer))
            return false
        }
        if (!validDataSettingsDevice.padblkValid(binding.inputSaveTimeout.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorTimeOutEnfora))
            return false
        }

        // проверка на русские символы в серверах и apn
        if (!validDataSettingsDevice.serverValid(binding.inputSaveServer1.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputSaveServer2.text.toString()) ||
            !validDataSettingsDevice.serverValid(binding.inputSaveAPN.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorRussionChar))
            return false
        }


        // проверка логина и пароля
        val loginPassword: String = binding.inputSaveLogin.text.toString().replace(" ", "")  +
                "," + binding.inputSavePassword.text.toString().replace(" ", "")
        if (!validDataSettingsDevice.loginPasswordValid(loginPassword)) {
            contextMain.showAlertDialog(getString(R.string.errorLoginPassworsd))
            return false
        }

        // проверкка на валидность сервера 1 и сервера 2
        if (!validDataSettingsDevice.validServer(binding.inputSaveServer1.text.toString()) ||
            !validDataSettingsDevice.validServer(binding.inputSaveServer2.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorValidServer))
            return false
        }

        if (!validDataSettingsDevice.validAPNEnfora(binding.inputSaveAPN.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorValidAPNEnfora))
            return false
        }

        return true
    }

    private fun validPm(): Boolean {
        val validDataSettingsDevice = ValidDataSettingsDevice()

        // проверка на русские символы в серверах и apn
        if (!validDataSettingsDevice.serverValid(binding.inputSaveKeyNet.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorRussionChar))
            return false
        }

        // проверки на валидность POWER
        if (!validDataSettingsDevice.powerValid(binding.inputSavePower.text.toString()
                .replace("\\s+".toRegex(), ""))) {

            contextMain.showAlertDialog(getString(R.string.errorPOWER))
            return false

        } else if (!validDataSettingsDevice.validPM81KeyNet(binding.inputSaveKeyNet.text.toString())) {
            contextMain.showAlertDialog(getString(R.string.errorNETKEY))
            return false
        }

        return true
    }

    private fun showAlertDialog(text: String) {
        val context: Context = requireContext()
        if (context is MainActivity) {
            context.showAlertDialog(text)
        }
    }
}