package com.kumir.settingupdevices.usb

import android.app.Activity
import android.content.Context
import android.util.Log
import com.kumir.settingupdevices.diag.DiagFragmentInterface
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R
import com.kumir.settingupdevices.diag.DiagSiagnalIntarface
import com.kumir.settingupdevices.formaters.FormatDataProtocol
import com.kumir.settingupdevices.diag.UsbDiagPm
import com.kumir.settingupdevices.formaters.ValidDataIniFile
import com.kumir.testappusb.settings.ConstUsbSettings

class UsbCommandsProtocol {

    var flagWorkChackSignal: Boolean = false
    var flagWorkDiag: Boolean = false
    var flagWorkDiagPm: Boolean = false
    var flagWorkWrite: Boolean = false
    var flagWorkRead: Boolean = false
    var flagWorkOneWire: Boolean = false

    // флаг успешности последней операции записи
    var flagSuccessfullyWrite = true


    // потоки
    lateinit var threadChackSignalEnfora: Thread
    lateinit var threadDiag: Thread
    lateinit var threadDiagPm: Thread

    // список комманд которые не должны подвергаться форматированию
    private val listCommandNotFormater: List<String> = listOf(
        "AT\$FRIEND?",
        "AT\$PKG?",
        "AT\$EVENT?",
        "AT\$DRVLIST",
        "AT\$ABLIST",
        "AT\$ABVIEW"

    )

    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 90
        const val WAITING_FOR_THE_TEAMS_RESPONSE_FOR_SPEED: Long = 150

        const val MAX_CNT_EXPECTATION_SAND: Int = 30
        const val MAX_RATIO_EXPECTATION_NEW_SPEED: Int = 15

        const val CNT_SAND_COMMAND_OK: Int = 3


        // для поиска скорости
        private const val SPEED_INDEX_MAX = 9
        private const val SPEED_INDEX_MIN = 0

        private const val PARITY_INDEX_MAX = 2
        private const val PARITY_INDEX_MIN = 0

        private const val STOPBIT_INDEX_MAX = 1
        private const val STOPBIT_INDEX_MIN = 0

        private const val BITDATA_INDEX_MAX = 1
        private const val BITDATA_INDEX_MIN = 0


        // максимальная задержка для диагностики
        private const val MAX_TIMEOUT_DIAG = 20 // 54 сек

    }


    // метод для получения настроек устройства
    fun readSettingDevice(commands: List<String>, context: Context,
                          usbFragment: UsbFragment, speedFind: Boolean = false, flagReadAbonentsP101: Boolean = false,
                          flagM32D: Boolean = false) {

        val settingData: MutableMap<String, String> = mutableMapOf()
        var flagsSuccess: Boolean = true


        Thread {
            flagWorkRead = true
            if (context is MainActivity) {


                // открываем диалог с загрузочным меню
                (context as Activity).runOnUiThread {
                    context.openCloseLoadingView(true)
                    context.flagThreadSerialCommands = true // говорим что работает поток чтения
                }

                // прогресс на единицу то есть каждая команда сколько то процентов
                val progressUnit: Int = 100 / commands.size
                var prograss: Int = 0

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // если нужно нати скорость перед использованием то
                if (speedFind && !findSpeed(context)) {
                    // все скорости переброны и нет ответа ошибка
                    (context as Activity).runOnUiThread {
                        context.showAlertDialog(context.getString(R.string.errorFindSpeedSerial))
                    }
                    flagsSuccess = false
                }

                if (flagsSuccess) {

                    // итератор для п101
                    var cntCommandItemViewP101: Int = 0

                    // перебор всех команд и получение ответов устройства
                    outer@ for (command in commands) {

                        // прогресс продливается
                        prograss += progressUnit

                        // CNT_SAND_COMMAND_OK попытки на отправку
                        for (i in 1..CNT_SAND_COMMAND_OK) {
                            // очищение прошлых данных
                            context.curentData = ""

                            // если неудачно отправили то выход
                            if (!context.usb.writeDevice(command, false)) {
                                flagsSuccess = false
                                break@outer
                            }

                            // система получения ответа и ожидание полной отправки данных
                            if (!expectationSand(context)) {
                                val curentData: String = context.curentData
                                // вывод в загрузочное диалог информации
                                (context as Activity).runOnUiThread {
                                    context.printInfoTermAndLoaging(command + "\n", prograss)
                                    context.printInfoTermAndLoaging(curentData + "\n", prograss)
                                }
                                if (i == CNT_SAND_COMMAND_OK) {
                                    // достигнуто ваксимальное время и нет ответа ошибка
                                    (context as Activity).runOnUiThread {
                                        context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                                    }
                                    flagsSuccess = false
                                    break@outer
                                } else { // пропуск для того что бы запустить заного
                                    continue
                                }
                            }


                            if (context.curentData.isNotEmpty()) {
                                // нормализуем только если не входит в список команд которые не нужно нормализовать
                                if (!flagReadAbonentsP101 && command != context.getString(R.string.commandGetAbView)) {
                                    settingData[command] = if (command !in listCommandNotFormater)
                                        formatDataCommandsNormolize(context.curentData) else context.curentData
                                } else {
                                    if (command == context.getString(R.string.commandGetAbView)) {
                                        cntCommandItemViewP101++
                                        settingData[command + cntCommandItemViewP101.toString()] = if (command !in listCommandNotFormater)
                                            formatDataCommandsNormolize(context.curentData) else context.curentData
                                    }
                                }

                                val curentData: String = context.curentData
                                // вывод в загрузочное диалог информации
                                (context as Activity).runOnUiThread {
                                    context.printInfoTermAndLoaging(command, prograss)
                                    context.printInfoTermAndLoaging(curentData, prograss)
                                }

                            } else {
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(context.getString(R.string.identifyDeviceFailed))
                                }
                                flagsSuccess = false
                                break
                            }

                            // проверка на валидность принатых данных возможно нужно еще раз опрасить
                            // проверка принялись ли данные
                            if (!sandOkCommand(context, command)) {

                                // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                                if (i == CNT_SAND_COMMAND_OK) {
                                    // для того что бы успешно выходить если симка 1
                                    if (flagM32D && command.contains("SIM2")) break@outer
                                    (context as Activity).runOnUiThread {
                                        context.showAlertDialog(
                                            command + context.getString(R.string.errorSendDataRead)
                                        )
                                    }
                                    flagsSuccess = false
                                    break@outer
                                }
                            } else {
                                break
                            }
                        }
                    }
                }



                // включение ат команд
                // context.usb.flagAtCommandYesNo = true

                if (flagsSuccess) {
                    (context as Activity).runOnUiThread {
                        usbFragment.printSettingDevice(settingData)
                    }
                    // включение ат команд
                    context.usb.flagAtCommandYesNo = true
                }

                context.curentData = ""

                context.flagThreadSerialCommands = false // говорим что не работает поток чтения


                // окончание прогресса
                (context as Activity).runOnUiThread {
                    context.printInfoTermAndLoaging("", 100)
                }
            }
            flagWorkRead = false
        }.start()
    }

    fun writeSettingDevice(data: Map<String, String>, context: Context, usbFragment: UsbFragment,
                            saveFlag: Boolean = true, longSleepX: Int = 1, flagExit: Boolean = false,
                           flagRead: Boolean = false, flagCode1251: Boolean = false) {

        Thread {

            flagWorkWrite = true
            if (context is MainActivity){

                var flagError: Boolean = false

                // открываем диалог с загрузочным меню
                (context as Activity).runOnUiThread {
                    context.openCloseLoadingView(true)
                    context.flagThreadSerialCommands = true // говорим что аботает поток ввода
                }

                // прогресс на единицу то есть каждая команда сколько то процентов
                val progressUnit: Int = 100 / data.size
                var prograss: Int = 0

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // отправка всех настроек в устройство
                out@for ((key, value) in data) {

                    // прогресс продливается
                    prograss += progressUnit

                    // CNT_SAND_COMMAND_OK попытки на отправку
                    for (i in 1..CNT_SAND_COMMAND_OK) {
                        // очищение прошлых данных
                        context.curentData = ""
                        context.currentDataByteAll = byteArrayOf()

                        val dataSend: String = key + value

                        // если неудачно отправили то выход
                        if (!context.usb.writeDevice(dataSend, false, flagCode1251=flagCode1251)) {
                            flagError = true
                            break@out
                        }


                        // система получения ответа и ожидание полной отправки данных
                        if (!expectationSand(context)) {
                            if (i == CNT_SAND_COMMAND_OK) {
                                // достигнуто ваксимальное время и нет ответа ошибка
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                                }
                                flagError = true
                                flagWorkWrite = false
                                break@out
                            } else { // пропуск для того что бы хапустить заного
                                continue
                            }
                        }

                        // дополнительное услоие если не пришло не ERROR не OK
                        if (!context.curentData.contains(context.getString(R.string.error)) && !context.curentData.contains(
                                context.getString(R.string.okSand)
                            )
                        ) {
                            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE * longSleepX * longSleepX)
                        }

                        val curentData: String = context.curentData
                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            context.printInfoTermAndLoaging(key + value, prograss)
                            context.printInfoTermAndLoaging(curentData, prograss)
                        }


                        // проверка принялись ли данные
                        if (!sandOkCommand(context, key)) {
                            if (i == CNT_SAND_COMMAND_OK) {
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(
                                        key + value + context.getString(R.string.errorSendDataWrite)
                                    )
                                }
                                flagWorkWrite = false
                                flagError = true
                                break@out
                            } else {
                                continue
                            }
                        }

                        // проверка на ккоманды изменения скорости или настроек передачи
                        if (!commandNewSpeed(context, key, value)) {

                            // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                            if (i == CNT_SAND_COMMAND_OK) {
                                flagError = true
                                flagWorkWrite = false

                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(context.getString(R.string.errorSpeed))
                                }
                                break@out
                            }
                        } else {
                            break
                        }
                    }
                }

                flagSuccessfullyWrite = !flagError

                if (!flagError && saveFlag) {

                    // сохранение данных AT$LOAD
                    /*context.usb.writeDevice(context.getString(R.string.commandLoadSettings), false)
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)*/

                    // сохранение данных AT$SAVE
                    context.usb.writeDevice(context.getString(R.string.commandSaveSettings), false, flagCode1251=flagCode1251)
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                    if (flagExit) {
                        context.usb.writeDevice(context.getString(R.string.commandExitSettingsMode), false, flagCode1251=flagCode1251)
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                    }

                    usbFragment.readSettingStart()
                }

                if (flagRead) {
                    (context as Activity).runOnUiThread {
                        usbFragment.readSettingStart()
                    }
                }

                context.curentData = ""

                context.flagThreadSerialCommands = false // говорим что не работает поток ввода

                // включение ат команд
                context.usb.flagAtCommandYesNo = true

                // окончание прогресса
                (context as Activity).runOnUiThread {
                    context.printInfoTermAndLoaging("", 100)
                }
            }

            flagWorkWrite = false
        }.start()
    }

    // метод для получения настроек устройства
    fun readSignalEnfora(command: String, context: Context, diagSiagnalIntarface: DiagSiagnalIntarface) {
        flagWorkChackSignal = true


        threadChackSignalEnfora = Thread {
            if (context is MainActivity) {

                context.flagThreadSerialCommands = true // говорим что работает поток ввода

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // флаг для получения ip
                var flagIP = false

                out@while (flagWorkChackSignal) {

                    // задержка для предотвращения вылета
                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE*10)

                    // очищение прошлых данных
                    context.curentData = ""


                    val commandSand: String =
                        if (!flagIP) context.getString(R.string.commandGetIP)
                        else command

                    // если неудачно отправили то выход
                    if (!context.usb.writeDevice(commandSand, false)) {
                        (context as Activity).runOnUiThread {
                            // меняем текст кнопки
                            if (flagWorkChackSignal)
                                diagSiagnalIntarface.onErrorStopChackSignal()
                        }
                        break@out
                    }

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))

                            // меняем текст кнопки
                            if (flagWorkChackSignal)
                                diagSiagnalIntarface.onErrorStopChackSignal()
                        }
                        break@out
                    }

                    // нормаизируем данняе и разделяем
                    val data: List<String> = formatDataCommandsNormolize(context.curentData).split(",")

                    // после получения данных отправляем их на отображение
                    if (flagWorkChackSignal) {
                        try {
                            if (flagIP) { // ip уже отобразился
                                val signal: String = data[0]
                                val errors: String = data[1]

                                (context as Activity).runOnUiThread {
                                    diagSiagnalIntarface.onPrintSignal(signal, errors)
                                }
                            } else { // отображение ip
                                (context as Activity).runOnUiThread {
                                    diagSiagnalIntarface.onPrintIP(formatDataCommandsNormolize(context.curentData))
                                }
                                flagIP = true
                            }

                        } catch (e: Exception) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.notValidData))

                                // меняем текст кнопки
                                if (flagWorkChackSignal)
                                    diagSiagnalIntarface.onErrorStopChackSignal()
                            }
                            break@out
                        }
                    }
                }

                // подкл ат команд
                //context.usb.flagAtCommandYesNo = true

                context.curentData = ""

                context.flagThreadSerialCommands = false // говорим что не работает поток ввода

            }
        }
        // запуск потока проверки сигнала
        threadChackSignalEnfora.start()
    }

    // провидение дисгностики
    fun readDiag(command: String, end: String, context: Context, usbDiag: UsbDiag, diagFragmentInterface: DiagFragmentInterface) {
        flagWorkDiag = true

        threadDiag = Thread {
            var flagSucGetSerAndVersion :Boolean = true
            if (context is MainActivity) {
                context.curentData = ""

                context.flagThreadSerialCommands = true // говорим что работает поток ввода

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false

                // поток для контроля подключения во время диагностики
                val threadControlConnect = Thread {
                    while (context.usb.checkConnectToDevice()) {
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

                        // если диагностика закнчена то немедленно выходм
                        if (!flagWorkDiag) break
                    }

                    // если при выходи поток все еще работает то значит ошибка
                    if (flagWorkDiag) {
                        context.runOnUiThread {
                            usbDiag.noConnect()
                        }
                    }
                }
                threadControlConnect.start()

                var serialNumber = ""
                var vesionProgram = ""

                // чтение номера прошивки и серийного номера---------------

                // серийный номер ...
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""

                    if(!context.usb.writeDevice(context.getString(R.string.commandGetSerialNum))) {
                        flagSucGetSerAndVersion = false
                        break
                    }

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandGetSerialNum))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    command + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        serialNumber = formatDataCommandsNormolize(context.curentData)

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // верисия прошивки ...
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""


                    if(!context.usb.writeDevice(context.getString(R.string.commandGetVersionFirmware))) {
                        flagSucGetSerAndVersion = false
                        break
                    }

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandGetVersionFirmware))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    command + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        vesionProgram = formatDataCommandsNormolize(context.curentData)

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // очищение данных
                context.curentData = ""

                // --------------------------------------------------------

                if (flagSucGetSerAndVersion) {

                    // вывод версии прошивки и сериного нмера
                    (context as Activity).runOnUiThread {
                        diagFragmentInterface.printVerAndSernum(vesionProgram, serialNumber)
                    }

                    if(!context.usb.writeDevice(command, false)) {
                        flagWorkDiag = false
                    }
                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                            // вывод ошибок
                            usbDiag.printError()
                        }
                        flagWorkDiag = false
                    }
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, command)) {
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorVersionModem))

                            // вывод ошибок
                            usbDiag.printError()
                        }
                    }

                    // ожидание полной отправки данных (end - конец данных)
                    var timeMaxIndex: Int = 50
                    while (!context.curentData.contains(end) && flagWorkDiag) {
                        if (timeMaxIndex == 0) {
                            break
                        }
                        // тестововая часть кода
                        val dataInfo: String = context.curentData
                        (context as Activity).runOnUiThread {
                            usbDiag.printAllInfo(dataInfo)
                        }
                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                        timeMaxIndex++
                    }

                    // проверка работает ли поток
                    if (flagWorkDiag) {
                        // вывод данных и ожидание для отображения
                        val dataInfo: String = context.curentData
                        (context as Activity).runOnUiThread {
                            usbDiag.printAllInfo(dataInfo)
                        }
                    }


                    Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE*5)

                    // отчистка данных
                    context.curentData = ""

                    // ожидание данных и вывод до тех пор пока флаг отключения не сработает
                    while (flagWorkDiag) {

                        if (expectationSand(context, MAX_TIMEOUT_DIAG, true)) {
                            val dataOperators: String = context.curentData
                            (context as Activity).runOnUiThread {
                                usbDiag.printAllOperator(dataOperators)
                            }
                        } else {
                            if (flagWorkDiag) {
                                flagWorkDiag = false
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                                    usbDiag.printError()
                                }
                            }
                        }

                        // отчистка данных
                        context.curentData = ""
                    }
                } else {
                    context.runOnUiThread {
                        // вывод ошибок
                        usbDiag.printError()
                    }
                }


                // отчистка данных
                context.curentData = ""

                context.flagThreadSerialCommands = false

                flagWorkDiag = false
            }
        }

        // старт потока
        threadDiag.start()
    }


    // проведение диагностики pm81
    fun readDiagPm(context: Context, usbDiagPm: UsbDiagPm, diagFragmentInterface: DiagFragmentInterface, netKey: String,
                    range: String) {
        flagWorkDiagPm = true

        threadDiagPm = Thread {
            var flagSucGetSerAndVersion :Boolean = true
            if (context is MainActivity) {
                context.curentData = ""

                context.flagThreadSerialCommands = true // говорим что работает поток ввода

                // отключение ат команд
                context.usb.flagAtCommandYesNo = false


                var serialNumber = ""
                var vesionProgram = ""

                // чтение номера прошивки и серийного номера---------------

                // серийный номер ...
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""

                    context.usb.writeDevice(context.getString(R.string.commandGetSerialNum))

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandGetSerialNum))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandGetSerialNum) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        serialNumber = formatDataCommandsNormolize(context.curentData)

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // верисия прошивки ...
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandGetVersionFirmware))

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandGetVersionFirmware))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandGetVersionFirmware) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        vesionProgram = formatDataCommandsNormolize(context.curentData)

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // чтение ключа
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandGetNetKey))

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandGetNetKey))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandGetNetKey) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        usbDiagPm.keyNet = formatDataCommandsNormolize(context.curentData)

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // чтение диапозона
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandGetRange))

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandGetRange))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandGetRange) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        // попытка преобразовать в алидные данные
                        try {
                            usbDiagPm.range = formatDataCommandsNormolize(context.curentData).trim().toInt()
                            flagSucGetSerAndVersion = true
                            break
                        } catch (e: Exception) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandGetRange) + context.getString(R.string.nonValidData)
                                )
                            }
                            flagSucGetSerAndVersion = false
                            break
                        }
                    }
                }

                // чтение режима работы
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandGetMode))

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandGetMode))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandGetMode) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        val validDataIniFile = ValidDataIniFile()
                        usbDiagPm.mode = formatDataCommandsNormolize(context.curentData)

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                //  назначения ключа доступа
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandSetNetKey) + netKey)

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandSetNetKey))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandSetNetKey) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                //  назначения диапозона
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandSetRange) + range)

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandSetRange))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandSetRange) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {

                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // переключение в режим монитора
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandSetMode) +
                            context.getString(R.string.devmodeMONITOR), false)

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandSetMode))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandSetMode) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // сохранение и выход для дальнешей работы
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandSaveSettings))

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandSaveSettings))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandSaveSettings) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        flagSucGetSerAndVersion = true
                        break
                    }
                }


                // сохранение и выход для дальнешей работы
                for (i in 0..CNT_SAND_COMMAND_OK) {
                    context.curentData = ""
                    context.usb.writeDevice(context.getString(R.string.commandExitSettingsMode))

                    // система получения ответа и ожидание полной отправки данных
                    if (!expectationSand(context)) {

                        // достигнуто ваксимальное время и нет ответа ошибка
                        (context as Activity).runOnUiThread {
                            context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                        }

                        // все попытки израсходываны
                        if (i == CNT_SAND_COMMAND_OK) {
                            flagSucGetSerAndVersion = false
                        }
                    }
                    // проверка на валидность принатых данных возможно нужно еще раз опрасить
                    // проверка принялись ли данные
                    if (!sandOkCommand(context, context.getString(R.string.commandSaveSettings))) {

                        // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                        if (i == CNT_SAND_COMMAND_OK) {
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(
                                    context.getString(R.string.commandSaveSettings) + context.getString(R.string.errorSendDataRead)
                                )
                            }
                            flagSucGetSerAndVersion = false
                        }
                    } else {
                        flagSucGetSerAndVersion = true
                        break
                    }
                }

                // вывод версии прошивки и сериного нмера
                (context as Activity).runOnUiThread {
                    diagFragmentInterface.printVerAndSernum(vesionProgram, serialNumber)
                }

                // очищение данных
                context.curentData = ""

                while (flagWorkDiagPm && flagSucGetSerAndVersion) {
                    // ожидание полной отправки данных
                    var timeUpdate: Int = 0
                    var dataLangth: Int = context.curentData.length
                    while (flagWorkDiagPm) {
                        timeUpdate++
                        if (timeUpdate % 220 == 0) {  // каждые 50 сек проверка появилась ли что то
                            if (context.curentData.length > dataLangth) {
                                dataLangth = context.curentData.length
                                break
                            }
                            else {
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                                }
                                flagWorkDiagPm = false
                                break
                            }
                        }

                        Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
                    }

                    // проверка работает ли поток
                    if (flagWorkDiagPm) {
                        // вывод данных и ожидание для отображения
                        val dataInfo: String = context.curentData
                        (context as Activity).runOnUiThread {
                            usbDiagPm.printAll(dataInfo)
                        }
                    }

                    // отчистка данных
                    context.curentData = ""
                }

                // отчистка данных
                context.curentData = ""

                context.flagThreadSerialCommands = false

                (context as Activity).runOnUiThread {
                    usbDiagPm.stopDiag()
                }

                flagWorkDiagPm = false
            }
        }

        threadDiagPm.start()
    }

    private fun commandNewSpeed(context: MainActivity, key: String, value: String): Boolean {
        val formatDataProtocol = FormatDataProtocol()

        // проверка на ккоманды изменения скорости или настроек передачи
        when(key) {
            context.getString(R.string.commandSetSpeed) -> {
                val speedIndex: Int = formatDataProtocol.getSpeedIndax(value)
                // дополнительная задержка в случае если скорость слишком мала
                val sleepForMinSpeed = if (speedIndex < 5) (MAX_RATIO_EXPECTATION_NEW_SPEED - speedIndex + 1) else 1

                context.usb.onSerialSpeed(speedIndex)
                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE * sleepForMinSpeed)
            }
            context.getString(R.string.commandSetFormatParity) -> {
                try {
                    // получения format и четности
                    val formatAndParity: List<String> = value.split(",")

                    // преобразование farmat в настроки битов данных и стоп биты а так же наличие четности
                    val setPortList = formatDataProtocol.reCalculateFormat(formatAndParity[0])

                    // применение настроек
                    context.usb.onSerialStopBits(setPortList[1])
                    context.usb.onSelectUumBit(setPortList[0] == 0)

                    // если есть четность то устанавливаем нужную
                    if (setPortList[2] == 1) {
                        if (formatAndParity[1].toInt() == 0) {
                            context.usb.onSerialParity(2) // утсновка odd
                        } else {
                            context.usb.onSerialParity(1) // установкаа even
                        }
                    }
                } catch (e: Exception) {
                    return false
                }
                Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
            }
            context.getString(R.string.commandSetDeffoltSetting) -> {

                // сброс на настроки т к модем переключился на деффолтные настроки
                context.usb.onSerialParity(0)
                context.usb.onSelectUumBit(true)
                context.usb.onSerialStopBits(0)
                context.usb.onSerialSpeed(9)
            }
        }
        return true
    }


    // поиск скорости
    private fun findSpeed(context: MainActivity): Boolean {
        // ищем нужную скорость для общения
        for (bitData in BITDATA_INDEX_MIN..BITDATA_INDEX_MAX) {
            for (stopBit in STOPBIT_INDEX_MIN..STOPBIT_INDEX_MAX) {
                for (parity in PARITY_INDEX_MIN..PARITY_INDEX_MAX) {
                    for (speed in SPEED_INDEX_MAX downTo SPEED_INDEX_MIN) {
                        // прверка есть ли подлючение
                        if (context.usb.checkConnectToDevice()) {
                            context.usb.onSelectUumBit(bitData == 0)
                            context.usb.onSerialParity(parity)
                            context.usb.onSerialStopBits(stopBit)
                            context.usb.onSerialSpeed(speed)

                            // вывод в загрузочное диалог информации
                            (context as Activity).runOnUiThread {
                                context.printInfoTermAndLoaging(
                                    speed.toString() + parity.toString() +
                                            stopBit.toString() + bitData.toString() + "\n", 0)
                            }

                            // отправка тестовой команды
                            context.usb.writeDevice(context.getString(R.string.commandSpeedFind), true)

                            // дополнительная задержка в случае если скорость слишком мала
                            val sleepForMinSpeed = if (speed < 5) (MAX_RATIO_EXPECTATION_NEW_SPEED - speed + 1) else 1
                            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE_FOR_SPEED * sleepForMinSpeed)

                            // если скорость найдена то выходим
                            if (context.curentData.contains(context.getString(R.string.okSand))) {
                                return true
                            }
                        } else {
                            return false
                        }
                    }
                }
            }
        }
        return false
    }


    // система получения ответа и ожидание полной отправки данных
    private fun expectationSand(context: MainActivity, kTime: Int = 1, diag: Boolean = false): Boolean {


        // Ожидание что устройство отправит хоть что то максимум ждет 500 мс
        var maxCntIter: Int = MAX_CNT_EXPECTATION_SAND
        while (context.curentData.isEmpty()) {

            // если режим диагностики то
            if (diag && !flagWorkDiag) {
                return false
            }

            if (maxCntIter == 0) {
                return false
            }
            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE * kTime)
            maxCntIter--
        }
        // ожидание что бы все данные были отправлены
        var cnt: Int = context.curentData.length
        while (context.curentData.isNotEmpty()) {

            // формула tmpTimeout := (((1 * (8 + 2 + 1)) * 1000) / speed) + 10;
            Thread.sleep((110000 / context.usb.speedList[ ConstUsbSettings.speedIndex ] + 10).toLong())

            // если режим диагностики то
            if (diag && !flagWorkDiag) {
                return false
            }

            val newCnt = context.curentData.length
            if (cnt == newCnt) {
                return true
            } else {
                cnt = newCnt
            }
        }
        return false
    }

    private fun sandOkCommand(context: MainActivity, command: String = ""): Boolean {

        // проверка на пустоту в данных     ИСПРАВЛЕНИЕ БАГА С НЕ ПРОЧИТАНЫМ СЕРИНЫМ НОМЕРОМ
        if (command == context.getString(R.string.commandGetSerialNum) && formatDataCommandsNormolize(context.curentData).isEmpty()) {
            return false
        }

        if (context.curentData.contains(context.getString(R.string.okSand))) {
            return true
        }

        // проверка принялись ли данные
        if (context.curentData.isEmpty() ||
            context.curentData.contains(context.getString(R.string.error)) ||
            !context.curentData.contains(context.getString(R.string.okSand))) {

            // если команда не входит в список команд которые не должны давать ответа то ерорим все
            if (command != context.getString(R.string.commandSetResetModem) &&
                command != context.getString(R.string.commandSetFormatParity) &&
                command != context.getString(R.string.commandGetAdLoad) &&
                command != context.getString(R.string.commandSetDriverMode)) {
                return false
            }

        }

        // проверка на команду загрузки драйвера
        if (command == context.getString(R.string.commandSetDriverMode)) {
            if (context.currentDataByteAll.isEmpty() || context.currentDataByteAll[0] != 0x15.toByte()) {
                /*
                Log.d("XModemSender", context.currentDataByteAll[0].toString())
                Log.d("XModemSender", context.currentDataByteAll.joinToString { "%02x".format(it) })
                */
                return false
            }
        }

        return true
    }


    fun calculateFormat(bitsData: Int, stopBits: Int, parity: Int): Int {
        return when {
            bitsData == 0 && stopBits == 1 && parity == 0 -> 1 // 8 данных, 2 стопа, без четности
            bitsData == 0 && stopBits == 0 && parity == 1 -> 2 // 8 данных, 1 стоп, с четностью
            bitsData == 0 && stopBits == 0 && parity == 0 -> 3 // 8 данных, 1 стоп, без четности
            bitsData == 1 && stopBits == 1 && parity == 0 -> 4 // 7 данных, 2 стопа, без четности
            bitsData == 1 && stopBits == 0 && parity == 1 -> 5 // 7 данных, 1 стоп, с четностью
            bitsData == 1 && stopBits == 0 && parity == 0 -> 6 // 7 данных, 1 стоп, без четности
            else -> 0
        }
    }




    private fun formatDataCommandsNormolize(data: String): String {
        if (!data.contains(":")) return ""
        return data.substringAfter(":").substringBefore("\n").trim()
    }

}