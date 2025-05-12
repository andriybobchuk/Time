package com.plcoding.bookpedia.mooney.data

import com.plcoding.bookpedia.mooney.domain.Currency
import com.plcoding.bookpedia.mooney.domain.ExchangeRates

object GlobalConfig {
    val baseCurrency = Currency.PLN

    val testExchangeRates = ExchangeRates(
        rates = mapOf(
            Currency.PLN to 1.0,     // base
            Currency.USD to 0.265,    // 1 PLN = 0.35 USD
            Currency.EUR to 0.22,     // 1 PLN = 0.22 EUR
            Currency.UAH to 11.0
        )
    )
}