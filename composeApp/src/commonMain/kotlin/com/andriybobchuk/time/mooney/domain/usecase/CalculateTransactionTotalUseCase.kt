package com.andriybobchuk.time.mooney.domain.usecase

import com.andriybobchuk.time.mooney.domain.CategoryType
import com.andriybobchuk.time.mooney.domain.Currency
import com.andriybobchuk.time.mooney.domain.ExchangeRates
import com.andriybobchuk.time.mooney.domain.Transaction

class CalculateTransactionTotalUseCase(
    private val exchangeRates: ExchangeRates
) {
    data class TotalResult(
        val total: Double,
        val currency: Currency
    )
    
    operator fun invoke(
        transactions: List<Transaction?>,
        selectedCurrency: Currency,
        baseCurrency: Currency
    ): TotalResult {
        val totalPln = transactions.filterNotNull().filter {
            it.subcategory.type == CategoryType.EXPENSE && 
            !it.subcategory.title.contains("ZUS") && 
            !it.subcategory.title.contains("PIT")
        }.sumOf {
            exchangeRates.convert(it.amount, it.account.currency, baseCurrency)
        }

        val converted = if (selectedCurrency != baseCurrency) {
            exchangeRates.convert(
                totalPln,
                from = baseCurrency,
                to = selectedCurrency
            )
        } else totalPln

        return TotalResult(
            total = converted,
            currency = selectedCurrency
        )
    }
} 