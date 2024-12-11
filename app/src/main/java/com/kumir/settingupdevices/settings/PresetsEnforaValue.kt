package com.kumir.settingupdevices.settings

import com.kumir.settingupdevices.dataBasePreset.Enfora

class PresetsEnforaValue {
    companion object {
        val presets: MutableMap<String, Enfora> = mutableMapOf(
            "По умолчанию" to Enfora(0, "По умолчанию", "kumir.dv",
                "", "", "172.27.0.15", "", "3", "512"),
            "Открытый «Билайн»" to Enfora(0, "Открытый «Билайн»", "internet",
                "", "", "knet.kumir-resurs.ru", "", "3", "512"),
            "Открытый «Мегафон»" to Enfora(0, "Открытый «Мегафон»", "internet",
                "", "", "knet.kumir-resurs.ru", "", "3", "512"),
            "Открытый «МТС»" to Enfora(0, "Открытый «МТС»", "internet.mts.ru",
                "", "", "knet.kumir-resurs.ru", "mts", "3", "512"),
            "Открытый «Ростелеком»" to Enfora(0, "Открытый «Ростелеком»", "internet.rtk.ru",
                "", "", "knet.kumir-resurs.ru", "", "3", "512"),
        )
    }
}