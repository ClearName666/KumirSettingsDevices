package com.kumir.settingupdevices.modems

import android.content.Context
import com.kumir.settingupdevices.R

// класс в котором функции которые возвращяют
// данные которые нужно перезаписать (для капризных модемов)
class ModemDataW(val context: Context) {

    // возвращяет только те данные которые нужно перезаписать
    fun getEnfora1318DataWrite(data: Map<String, String>,
                               dataCustomMap: Map<String, String>): MutableMap<String, String> {
        val dataWrite: MutableMap<String, String> = mutableMapOf()

        // записываемые данные
        var server1: String = context.getString(R.string.defaultHelpCheckSERVER1)
        var server2: String = context.getString(R.string.defaultHelpCheckSERVER2)
        var apn: String = context.getString(R.string.defaultAPN)
        var timeOut: String = context.getString(R.string.defaultSetPadTimeout)
        var sizeBuffer: String = context.getString(R.string.defaultSetPadBlockSize)
        var loginPassword: String = ""
        var flagCustom: Boolean = false

        if (dataCustomMap.isNotEmpty()) {
            server1 = dataCustomMap[context.getString(R.string.commandServer1EnforaOrM31)].toString()
            server2 = dataCustomMap[context.getString(R.string.commandServer2EnforaOrM31)].toString()
            apn = dataCustomMap[context.getString(R.string.commandGetApnEnforaM31)].toString()
            timeOut = dataCustomMap[context.getString(R.string.commandGetPadTimeout)].toString()
            sizeBuffer = dataCustomMap[context.getString(R.string.commandGetPadBlockSize)].toString()
            loginPassword = dataCustomMap[context.getString(R.string.commandGetUsernamePassword)].toString()
            flagCustom = true
        }

        if (flagCustom/* && data[context.getString(R.string.commandGetUsernamePassword)]?.
                contains(loginPassword) == false && loginPassword.isNotEmpty()*/) {

            dataWrite[context.getString(R.string.commandSetConfigurePPPWithLogin)] = "1"
            dataWrite[context.getString(R.string.commandSetUsernamePassword)] =
                "1,\"$loginPassword\",1"
        } else {
            if (data[context.getString(R.string.commandGetApnEnforaM31)]?.contains("0") == false
                    && loginPassword.isNotEmpty()) {
                dataWrite[context.getString(R.string.commandSetConfigurePPPWithLogin)] = "0"
            }
        }

        // сервер 1 -----------------------------------------
        if (flagCustom || data[context.getString(R.string.commandServer1EnforaOrM31)]?.
            contains(server1) == false) {

            if (!flagCustom) {
                dataWrite[context.getString(R.string.commandSetDestinationAddress)] =
                    context.getString(R.string.defaultSERVER1)
            } else {
                dataWrite[context.getString(R.string.commandSetDestinationAddress)] = "\"$server1\",0"
            }

        }

        // сервер 2 ------------------------------------------
        if (flagCustom || data[context.getString(R.string.commandServer2EnforaOrM31)]?.
            contains(server1) == false || data[context.getString(R.string.commandServer2EnforaOrM31)]?.contains(server2) == false) {

            // добавление команд сброса AT$FRIEND
            for (itemFRIEND in 1..10) {
                dataWrite[context.getString(R.string.commandSetFriend) + itemFRIEND.toString() + context.getString(R.string.defaultFriend)] = ""
            }

            // добавления нужных друзей
            if (!flagCustom) {
                dataWrite[context.getString(R.string.commandSetFriend) + context.getString(R.string.defaultSetFriend1)] = ""
                dataWrite[context.getString(R.string.commandSetFriend) + context.getString(R.string.defaultSetFriend2)] = ""
            } else {
                dataWrite[context.getString(R.string.commandSetFriend) + "1,1,\"$server1\""] = ""
                dataWrite[context.getString(R.string.commandSetFriend) + "2,1,\"$server2\""] = ""
            }

        }

        // apn
        if (flagCustom || data[context.getString(R.string.commandGetApnEnforaM31)]?.
            contains(apn) == false) {

            if (!flagCustom) {
                dataWrite[context.getString(R.string.commandSetDefinePDPContext)] =
                    context.getString(R.string.defaultDefinePDPContext)
            } else {
                dataWrite[context.getString(R.string.commandSetDefinePDPContext)] =
                    "1,\"IP\",\"$apn\",\"\",0,0"
            }
        }

        // tcpport
        if (data[context.getString(R.string.commandGetTcpPortEnforaM31)]?.
            contains(context.getString(R.string.defaultTCPPORT)) == false) {

            dataWrite[context.getString(R.string.commandSetSourcePort)] =
                context.getString(R.string.defaultTCPPORT)
        }

        // login and passw..
        if (data[context.getString(R.string.commandGetLoginPasswordEnforaM31)]?.
            contains(context.getString(R.string.defaultConfigurePPP)) == false) {

            dataWrite[context.getString(R.string.commandSetConfigurePPP)] =
                context.getString(R.string.defaultConfigurePPP)
        }

        // AT%CGAATT
        if (data[context.getString(R.string.commandGetDisableAutoAttach)]?.
            contains(context.getString(R.string.defaultDisableAutoAttach)) == false) {

            dataWrite[context.getString(R.string.commandSetDisableAutoAttach)] =
                context.getString(R.string.defaultDisableAutoAttach)
        }

        // AT$AREG
        if (data[context.getString(R.string.commandGetAutoRegistration)]?.
            contains(context.getString(R.string.defaultAutoRegistration)) == false) {

            dataWrite[context.getString(R.string.commandSetAutoRegistration)] =
                context.getString(R.string.defaultAutoRegistration)
        }

        // AT$HOSTIF
        if (data[context.getString(R.string.commandGetConfigureHostInterface)]?.
            contains(context.getString(R.string.defaultConfigureHostInterface)) == false) {

            dataWrite[context.getString(R.string.commandSetConfigureHostInterface)] =
                context.getString(R.string.defaultConfigureHostInterface)
        }

        // AT$PADBLK
        if (flagCustom || data[context.getString(R.string.commandGetPadBlockSize)]?.
            contains(sizeBuffer) == false) {

            if (!flagCustom) {
                dataWrite[context.getString(R.string.commandSetPadBlockSize)] =
                    context.getString(R.string.defaultSetPadBlockSize)
            } else {
                dataWrite[context.getString(R.string.commandSetPadBlockSize)] = sizeBuffer
            }

        }

        // AT$PADTO
        if (flagCustom || data[context.getString(R.string.commandGetPadTimeout)]?.
            contains(timeOut) == false) {

            if (!flagCustom) {
                dataWrite[context.getString(R.string.commandSetPadTimeout)] =
                    context.getString(R.string.defaultSetPadTimeout)
            } else {
                dataWrite[context.getString(R.string.commandSetPadTimeout)] =
                    timeOut
            }

        }

        // AT$WAKEUP
        if (data[context.getString(R.string.commandGetConfigureWakeup)]?.
            contains(context.getString(R.string.defaultConfigureWakeup)) == false) {

            dataWrite[context.getString(R.string.commandSetConfigureWakeup)] =
                context.getString(R.string.defaultConfigureWakeup)
        }

        // AT$ACKTM
        if (data[context.getString(R.string.commandGetConfigureAck)]?.
            contains(context.getString(R.string.defaultReadConfigureAck)) == false) {

            dataWrite[context.getString(R.string.commandSetConfigureAck)] =
                context.getString(R.string.defaultConfigureAck)
        }

        // AT$PADCMD
        if (data[context.getString(R.string.commandGetExecutePadCommand)]?.
            contains(context.getString(R.string.defaultReadExecutePadCommand)) == false) {

            dataWrite[context.getString(R.string.commandSetExecutePadCommand)] =
                context.getString(R.string.defaultExecutePadCommand)
        }

        // AT$ACTIVE
        if (data[context.getString(R.string.commandGetActivatePadConnection)]?.
            contains(context.getString(R.string.defaultActivatePadConnection)) == false) {

            dataWrite[context.getString(R.string.commandSetActivatePadConnection)] =
                context.getString(R.string.defaultActivatePadConnection)
        }

        // AT$CONNTO
        if (data[context.getString(R.string.commandGetConnectionTimeoutEnfora)]?.
            contains(context.getString(R.string.defaultSetConnectionTimeout)) == false) {

            dataWrite[context.getString(R.string.commandSetConnectionTimeoutEnfora)] =
                context.getString(R.string.defaultSetConnectionTimeout)
        }

        // AT$IDLETO
        if (data[context.getString(R.string.commandGetIdleTimeout)]?.
            contains(context.getString(R.string.defaultSetIdleTimeout)) == false) {

            dataWrite[context.getString(R.string.commandSetIdleTimeout)] =
                context.getString(R.string.defaultSetIdleTimeout)
        }

        // AT$NETMON
        if (data[context.getString(R.string.commandGetNetworkMonitor)]?.
            contains(context.getString(R.string.defaultReadNetworkMonitor)) == false) {

            dataWrite[context.getString(R.string.commandSetNetworkMonitor)] =
                context.getString(R.string.defaultNetworkMonitor)
        }

        /*// AT$STOATEV
        if (data[context.getString(R.string.commandGetStoreAtEvents)]?.
            contains(context.getString(R.string.defaultStoreAtEvents)) == false) {

            dataWrite[context.getString(R.string.commandSetStoreAtEvents)] =
                context.getString(R.string.defaultStoreAtEvents)
        }*/

        // AT$EVTIM1
        if (data[context.getString(R.string.commandGetEventTimer)]?.
            contains(context.getString(R.string.defaultSetEventTimer)) == false) {

            dataWrite[context.getString(R.string.commandSetEventTimer)] =
                context.getString(R.string.defaultSetEventTimer)
        }

        // AT$IOCFG
        /*if (data[context.getString(R.string.commandGetConfigureGPIO)]?.
            contains(context.getString(R.string.defaultConfigureGPIO)) == false) {*/

            dataWrite[context.getString(R.string.commandSetConfigureGPIO)] =
                context.getString(R.string.defaultConfigureGPIO)
        /*}*/

        // AT$IOGPA
        /*if (data[context.getString(R.string.commandGetGPIOValue)]?.
            contains(context.getString(R.string.defaultSetGPIOValue)) == false) {*/

            dataWrite[context.getString(R.string.commandSetGPIOValue)] =
                context.getString(R.string.defaultSetGPIOValue)
        /*}*/

        // EVENT
        val eventsList: Map<String, Boolean> = eventValidator(data[context.getString(R.string.commandGetEvent)]!!)

        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent1))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent1)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent2))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent2)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent3))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent3)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent4))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent4)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent5))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent5)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent6))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent6)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent7))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent7)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent8))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent8)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent9))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent9)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent10))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent10)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent11))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent11)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent12))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent12)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent13))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent13)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent14))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent14)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent15))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent15)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent16))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent16)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent17))) {
            dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent17)] = ""
        }
        if (!eventsList.containsKey(context.getString(R.string.defaultConfigureEvent18))) {
                dataWrite[context.getString(R.string.commandSetEvent) + context.getString(R.string.defaultConfigureEvent8)] = ""
        }

        return dataWrite
    }


    // функция для получения стуруктурированых events
    private fun eventValidator(events: String): Map<String, Boolean> {
        val eventsList: MutableMap<String, Boolean> = mutableMapOf()

        // разделенныу \n event для того что бы перебрать их в цикле и преобразовать
        val eventsSplit: List<String> = events.split("\n").drop(1)

        // перебор всех строк
        for (event in eventsSplit) {
            val eventData: List<String> = event.split("\\s+".toRegex())

            try {
                val eventResult: String = "${eventData[1].dropLast(1)}," +
                        "${eventData[2]},${eventData[3]},${eventData[4]},${eventData[5]}"

                eventsList[eventResult] = true
            } catch (e: Exception) {
                // при ошибки возвращяем пустой масив
                // return mapOf()
            }
        }
        /*if (context is MainActivity) {
            context.showAlertDialog(eventsList.toString())
        }*/

        return eventsList
    }
}