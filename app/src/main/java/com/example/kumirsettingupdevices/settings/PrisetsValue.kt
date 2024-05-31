package com.example.kumirsettingupdevices.settings

import com.example.kumirsettingupdevices.model.recyclerModel.Priset

// класс хранит в себе все существующие присеты
class PrisetsValue {
    companion object {
        val prisets: MutableMap<String, Priset> = mutableMapOf(
            "По умолчанию" to Priset("По умолчанию","kumir.dv",
                "6500", "172.27.0.15", "", ""),
            "Открытый «Билайн»" to Priset("Открытый «Билайн»", "internet",
                "6500", "knet.kumir-resurs.ru", "beeline", "beeline"),
            "Открытый «Мегафон»" to Priset("Открытый «Мегафон»", "internet",
                "6500", "knet.kumir-resurs.ru", "gdata", "gdata"),
            "Открытый «МТС»" to Priset("Открытый «МТС»", "internet.mts.ru",
                "6500", "knet.kumir-resurs.ru", "mts", "mts"),
            "Открытый «Ростелеком»" to Priset("Открытый «Ростелеком»", "internet.rtk.ru",
                "6500", "knet.kumir-resurs.ru", "", ""),
        )
    }

}