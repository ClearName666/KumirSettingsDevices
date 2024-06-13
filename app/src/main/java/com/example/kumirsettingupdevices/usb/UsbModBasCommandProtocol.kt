package com.example.kumirsettingupdevices.usb

import android.app.Activity
import android.content.Context
import com.example.kumirsettingupdevices.MainActivity
import com.example.kumirsettingupdevices.R
import com.example.kumirsettingupdevices.usb.UsbModBasCommandProtocol.Companion.CNT_SAND_COMMAND_OK
import com.example.kumirsettingupdevices.usb.UsbModBasCommandProtocol.Companion.MAX_CNT_EXPECTATION_SAND
import com.example.kumirsettingupdevices.usb.UsbModBasCommandProtocol.Companion.WAITING_FOR_THE_TEAMS_RESPONSE


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
                        context.curentDataByte = byteArrayOf()

                        context.usb.writeDevice("", false, command)



                        // система получения ответа и ожидание полной отправки данных
                        if (!expectationSand(context)) {

                            // достигнуто ваксимальное время и нет ответа ошибка
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                            }
                            break@end
                        }



                        // вывод в диалог
                        val curentData: String = context.curentDataByte.dropLast(CNT_BYTE_DROP_LAST).
                            drop(CNT_BYTE_DROP).toString()

                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            context.printInfoTermAndLoaging("command: ${command}\n", prograss)
                            context.printInfoTermAndLoaging("answer: ${curentData}\n", prograss)
                        }



                        // проверка на валидность принатых данных возможно нужно еще раз опрасить
                        // проверка принялись ли данные
                        if (!sandOkCommand(context.curentDataByte)) {

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
                            if (context.curentDataByte.isNotEmpty())
                                if (context.curentDataByte.size > CNT_DATA_PORT)
                                    settingData[context.curentDataByte[0].toString() +
                                        context.curentDataByte[1].toString()] =
                                            context.curentDataByte.
                                                drop(CNT_BYTE_DROP_PORT).dropLast(CNT_BYTE_DROP_LAST).
                                                    toString()
                                else
                                    settingData[context.curentDataByte[0].toString()] =
                                        context.curentDataByte[1].toString()
                            break
                        }
                    }

                    // очищение буфера
                    context.curentDataByte = byteArrayOf()
                }



                context.curentData = ""
                context.curentDataByte = byteArrayOf()

                context.flagThreadSerialCommands = false // говорим что не работает поток чтения

                // окончание прогресса
                (context as Activity).runOnUiThread {
                    context.printInfoTermAndLoaging("", 100)
                    usbFragment.printSettingDevice(settingData)

                    /*if (settingData.isNotEmpty()) {
                        // включение ат команд
                        context.usb.flagAtCommandYesNo = true
                    }*/
                }
            }
        }.start()
    }

    // метод для получения настроек устройства
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
                        context.curentDataByte = byteArrayOf()

                        context.usb.writeDevice("", false, command)



                        // система получения ответа и ожидание полной отправки данных
                        if (!expectationSand(context)) {

                            // достигнуто ваксимальное время и нет ответа ошибка
                            (context as Activity).runOnUiThread {
                                context.showAlertDialog(context.getString(R.string.errorTimeOutSand))
                            }
                            break@end
                        }



                        // вывод в диалог
                        val curentData: String = context.curentDataByte.dropLast(
                            CNT_BYTE_DROP_LAST
                        ).
                        drop(CNT_BYTE_DROP).toString()

                        // вывод в загрузочное диалог информации
                        (context as Activity).runOnUiThread {
                            context.printInfoTermAndLoaging("command: ${command}\n", prograss)
                            context.printInfoTermAndLoaging("answer: ${curentData}\n", prograss)
                        }



                        // проверка на валидность принатых данных возможно нужно еще раз опрасить
                        // проверка принялись ли данные
                        if (!sandOkCommandWrite(context.curentDataByte)) {

                            // если CNT_SAND_COMMAND_OK попытка не сработала то выбрасываемся
                            if (item == CNT_SAND_COMMAND_OK) {
                                (context as Activity).runOnUiThread {
                                    context.showAlertDialog(
                                        command.toString() + context.getString(R.string.errorSendDataWrite)
                                    )
                                }
                                break@end
                            }
                        }
                    }

                    // очищение буфера
                    context.curentDataByte = byteArrayOf()
                }

                /*// включение ат команд
                context.usb.flagAtCommandYesNo = true*/

                context.curentData = ""
                context.curentDataByte = byteArrayOf()

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
        while (context.curentDataByte.isEmpty()) {

            if (maxCntIter == 0) {
                return false
            }
            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)
            maxCntIter--
        }

        // ожидание что бы все данные были отправлены
        var cnt: Int = context.curentDataByte.size
        while (context.curentDataByte.isNotEmpty()) {
            Thread.sleep(WAITING_FOR_THE_TEAMS_RESPONSE)

            val newCnt = context.curentDataByte.size
            if (cnt == newCnt) {
                return true
            } else {
                cnt = newCnt
            }
        }
        return false
    }

}

