package com.kumir.settingupdevices.settings

import com.kumir.settingupdevices.model.recyclerModel.Priset

// класс хранит в себе все существующие присеты
class PrisetsValue {
    companion object {
        val prisets: MutableMap<String, Priset> = mutableMapOf(
            "По умолчанию" to Priset("По умолчанию", 0, "kumir.dv",
                "6500", "172.27.0.15", "", ""),
            "Открытый «Билайн»" to Priset("Открытый «Билайн»", 0, "internet",
                "6500", "knet.kumir-resurs.ru", "beeline", "beeline"),
            "Открытый «Мегафон»" to Priset("Открытый «Мегафон»", 0, "internet",
                "6500", "knet.kumir-resurs.ru", "gdata", "gdata"),
            "Открытый «МТС»" to Priset("Открытый «МТС»", 0, "internet.mts.ru",
                "6500", "knet.kumir-resurs.ru", "mts", "mts"),
            "Открытый «Ростелеком»" to Priset("Открытый «Ростелеком»", 0, "internet.rtk.ru",
                "6500", "knet.kumir-resurs.ru", "", ""),
        )
    }

}