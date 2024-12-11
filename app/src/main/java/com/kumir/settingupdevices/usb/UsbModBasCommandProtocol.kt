package com.kumir.settingupdevices.usb

import android.app.Activity
import android.content.Context
import com.kumir.settingupdevices.MainActivity
import com.kumir.settingupdevices.R


class UsbModBasCommandProtocol {

    companion object {
        const val WAITING_FOR_THE_TEAMS_RESPONSE: Long = 3

        const val MAX_CNT_EXPECTATION_SAND: Int = 140

        const val CNT_SAND_COMMAND_OK: Int = 3

        const val CNT_BYTE_DROP: Int = 1
        const val CNT_BYTE_DROP_PORT: Int = 2
        const val CNT_BYTE_DROP_LAST: Int = 2

        const val CNT_DATA_PORT: Int = 4
    }



    // метод для получения настроек устройства
    fun readSettingDevice(commands: List<ByteArray>, context: Context, usbFragment: UsbFragment) {
        val settingData: MutableMap<String, String> = mutableMapOf()

        Thread {
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

                // перебор всех команд и получение ответов устройства
                end@for (command in commands) {
                    // прогресс продливается
                    prograss += progressUnit

                    for (item in 0..CNT_SAND_COMMAND_OK) {
                        // очищение прошлых данных
                        context.currentDataByteAll = byteArrayOf()

                        if(!context.usb.writeDevice("", false, command)) {
                            break@end
                        }



                        // система получения ответа и ожидание полной отправки данных
                        if (!expectationSand(context)) {

                            // достигнуто ваксимальное время и нет ответа ошибка
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                            }
                            break@end
                        }



                        // вывод в диалог
                        val curentData: String = context.currentDataByteAll.dropLast(CNT_BYTE_DROP_LAST).
                            drop(CNT_BYTE_DROP).toString()

                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            context.printInfoTermAndLoaging("command: ${command}\n", prograss)
                            context.printInfoTermAndLoaging("answer: ${curentData}\n", prograss)
                        }



                        // проверка на валидность принатых данных возможно нужно еще раз опрасить
                        // проверка принялись ли данные
                        if (!sandOkCommand(context.currentDataByteAll)) {

                            // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                            if (item == CNT_SAND_COMMAND_OK) {
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(
                                        command.toString() + context.getString(R.string.errorSendDataRead)
                                    )
                                }
                                break@end
                            }
                        } else {
                            // если команда чтения портов то команда + номер порта, иначе просто команда
                            if (context.currentDataByteAll.isNotEmpty())
                                if (context.currentDataByteAll.size > CNT_DATA_PORT)
                                    settingData[context.currentDataByteAll[0].toString() +
                                        context.currentDataByteAll[1].toString()] =
                                            context.currentDataByteAll.
                                                drop(CNT_BYTE_DROP_PORT).dropLast(CNT_BYTE_DROP_LAST).
                                                    toString()
                                else
                                    settingData[context.currentDataByteAll[0].toString()] =
                                        context.currentDataByteAll[1].toString()
                            break
                        }
                    }

                    // очищение буфера
                    context.currentDataByteAll = byteArrayOf()
                }



                context.curentData = ""
                context.currentDataByteAll = byteArrayOf()

                context.flagThreadSerialCommands = false // говорим что не работает поток чтения

                // окончание прогресса
                (context as Activity).runOnUiThread {
                    context.printInfoTermAndLoaging("", 100)

                    // проверка подключения что бы не позволять приложению открывать доступ к кнопке
                    if (context.usb.checkConnectToDevice())
                        usbFragment.printSettingDevice(settingData)

                }
            }
        }.start()
    }

    // метод для получения настроек устройства
    @OptIn(ExperimentalStdlibApi::class)
    fun writeSettingDevice(commands: List<ByteArray>, context: Context) {

        Thread {
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

                // перебор всех команд и получение ответов устройства
                end@for (command in commands) {
                    // прогресс продливается
                    prograss += progressUnit

                    for (item in 0..CNT_SAND_COMMAND_OK) {
                        // очищение прошлых данных
                        context.currentDataByteAll = byteArrayOf()


                        if(!context.usb.writeDevice("", false, command)) {
                            break@end
                        }



                        // система получения ответа и ожидание полной отправки данных
                        if (!expectationSand(context)) {

                            // достигнуто ваксимальное время и нет ответа ошибка
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                            }
                            break@end
                        }



                        // вывод в диалог
                        val currentData: ByteArray = context.currentDataByteAll

                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            context.printInfoTermAndLoaging("command: ${command.toHexString()}\n", prograss)
                            context.printInfoTermAndLoaging("answer: ${currentData.toHexString()}\n", prograss)
                        }



                        // проверка на валидность принатых данных возможно нужно еще раз опрасить
                        // проверка принялись ли данные
                        if (!sandOkCommandWrite(context.currentDataByteAll)) {

                            // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                            if (item == CNT_SAND_COMMAND_OK) {
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(
                                        command.toString() + context.getString(R.string.errorSendDataWrite)
                                    )
                                }
                                break@end
                            }
                        } else break // успешно выходим из цикла попыток отправки
                    }
                    // очищение буфера
                    context.currentDataByteAll = byteArrayOf()
                }

                /*// включение ат команд
                context.usb.flagAtCommandYesNo = true*/

                context.curentData = ""
                context.currentDataByteAll = byteArrayOf()

                context.flagThreadSerialCommands = false // говорим что не работает поток чтения

                // окончание прогресса
                (context as Activity).runOnUiThread {
                    context.printInfoTermAndLoaging("", 100)
                }
            }
        }.start()
    }


    private fun sandOkCommand(byteArray: ByteArray): Boolean {

        // проверка принялись ли данные
        // контрольная сумма
        val crc = CRC16Modbus()
        crc.update(byteArray)
        val checkSum = crc.crcBytes

        if (checkSum[0] != 0x00.toByte()) {
            return false
        }
        return true
    }

    private fun sandOkCommandWrite(byteArray: ByteArray): Boolean {
        // проверкка на ошибки
        return !(!sandOkCommand(byteArray) || byteArray[2] != 0xAA.toByte())
    }


    // система получения ответа и ожидание полной отправки данных
    private fun expectationSand(context: MainActivity): Boolean {

        // Ожидание что устройство отправит хоть что то
        var maxCntIter: Int = MAX_CNT_EXPECTATION_SAND
        while (context.currentDataByteAll.isEmpty()) {

            if (maxCntIter == 0) {
                return false
            }
            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
            maxCntIter--
        }

        // ожидание что бы все данные были отправлены
        var cnt: Int = context.currentDataByteAll.size
        while (context.currentDataByteAll.isNotEmpty()) {
            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

            val newCnt = context.currentDataByteAll.size
            if (cnt == newCnt) {
                return true
            } else {
                cnt = newCnt
            }
        }
        return false
    }

}

