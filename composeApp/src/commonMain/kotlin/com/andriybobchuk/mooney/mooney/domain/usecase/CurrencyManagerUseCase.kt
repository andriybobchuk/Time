package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates

class CurrencyManagerUseCase(
    private val exchangeRates: ExchangeRates,
    private val baseCurrency: Currency
) {
    private val availableCurrencies = exchangeRates.rates.keys.toList()
    private var selectedCurrencyIndex = 0
    private var selectedCurrency = baseCurrency

    fun getCurrentCurrency(): Currency = selectedCurrency

    fun getAvailableCurrencies(): List<Currency> = availableCurrencies

    fun cycleToNextCurrency(): Currency {
        selectedCurrencyIndex = (selectedCurrencyIndex + 1) % availableCurrencies.size
        selectedCurrency = availableCurrencies[selectedCurrencyIndex]
        return selectedCurrency
    }

    fun resetToBaseCurrency() {
        selectedCurrencyIndex = availableCurrencies.indexOf(baseCurrency)
        selectedCurrency = baseCurrency
    }
} 