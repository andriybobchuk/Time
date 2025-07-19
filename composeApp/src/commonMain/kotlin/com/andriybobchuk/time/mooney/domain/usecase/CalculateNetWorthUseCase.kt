package com.andriybobchuk.time.mooney.domain.usecase

import com.andriybobchuk.time.mooney.domain.Currency
import com.andriybobchuk.time.mooney.domain.ExchangeRates
import com.andriybobchuk.time.mooney.presentation.account.UiAccount

class CalculateNetWorthUseCase(
    private val exchangeRates: ExchangeRates
) {
    data class NetWorthResult(
        val totalNetWorth: Double,
        val currency: Currency
    )
    
    operator fun invoke(
        accounts: List<UiAccount?>,
        selectedCurrency: Currency,
        baseCurrency: Currency
    ): NetWorthResult {
        val totalPln = accounts.filterNotNull().sumOf { it.baseCurrencyAmount }

        val converted = if (selectedCurrency != baseCurrency) {
            exchangeRates.convert(
                amount = totalPln,
                from = baseCurrency,
                to = selectedCurrency
            )
        } else {
            totalPln
        }

        return NetWorthResult(
            totalNetWorth = converted,
            currency = selectedCurrency
        )
    }
} 