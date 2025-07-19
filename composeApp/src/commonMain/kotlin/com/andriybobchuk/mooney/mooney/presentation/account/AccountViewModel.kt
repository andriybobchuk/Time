package com.andriybobchuk.mooney.mooney.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountViewModel(
    private val repository: CoreRepository
) : ViewModel() {

    private var observeAccountsJob: Job? = null

    private val _uiState = MutableStateFlow(AccountState())

    //val state: StateFlow<AccountState> = _uiState
    val state = _uiState
        .onStart {
            observeAccounts()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun observeAccounts() {
        observeAccountsJob?.cancel()
        observeAccountsJob = repository
            .getAllAccounts()
            .onEach { accounts ->
                _uiState.update {
                    it.copy(
                        accounts = accounts.toUiAccounts(GlobalConfig.testExchangeRates)
                    )
                }
                loadAccountsAndWorth()
            }
            .launchIn(viewModelScope)
    }

    private val availableCurrencies = GlobalConfig.testExchangeRates.rates.keys.toList()
    private var selectedCurrencyIndex = 0
    private var selectedCurrency = GlobalConfig.baseCurrency

    private suspend fun loadAccountsAndWorth() = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isLoading = true) }

        try {
            repository.getAllAccounts().collect { list ->
                _uiState.update {
                    it.copy(
                        accounts = list.toUiAccounts(GlobalConfig.testExchangeRates),
                        isLoading = false
                    )
                }
                // Update net worth after accounts loaded
                updateTotalNetWorth()
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isError = true, isLoading = false) }
        }
    }

    private fun updateTotalNetWorth() {

        val totalPln = state.value.accounts.filterNotNull().sumOf { it.baseCurrencyAmount }

        val converted = if (selectedCurrency != GlobalConfig.baseCurrency) {
            GlobalConfig.testExchangeRates.convert(
                amount = totalPln,
                from = GlobalConfig.baseCurrency,
                to = selectedCurrency
            )
        } else {
            totalPln
        }

        _uiState.update {
            it.copy(
                totalNetWorth = converted,
                totalNetWorthCurrency = selectedCurrency
            )
        }

    }

    fun onNetWorthLabelClick() {
        selectedCurrencyIndex = (selectedCurrencyIndex + 1) % availableCurrencies.size
        selectedCurrency = availableCurrencies[selectedCurrencyIndex]
        updateTotalNetWorth()
    }

    fun upsertAccount(
        id: Int,
        title: String,
        emoji: String,
        amount: Double,
        currency: Currency
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = Account(id, title, amount, currency, emoji)

            try {
                repository.upsertAccount(account)
                //loadAccountsAndWorth()
                observeAccounts()
            } catch (e: Exception) {
            }
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAccount(id)
                // loadAccountsAndWorth()
            } catch (e: Exception) {
            }
        }
    }
}


data class UiAccount(
    val id: Int,
    val title: String,
    val emoji: String,
    val originalAmount: Double,
    val originalCurrency: Currency,
    val baseCurrencyAmount: Double,
    val exchangeRate: Double?
)


data class AccountState(
    val accounts: List<UiAccount?> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val totalNetWorthCurrency: Currency = GlobalConfig.baseCurrency,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)


fun List<Account?>.toUiAccounts(
    rates: ExchangeRates
): List<UiAccount?> {
    return this.map { account ->
        if (account?.currency == GlobalConfig.baseCurrency) {
            UiAccount(
                id = account.id,
                title = account.title,
                emoji = account.emoji,
                originalAmount = account.amount,
                originalCurrency = account.currency,
                baseCurrencyAmount = account.amount,
                exchangeRate = null
            )
        } else {
            account?.let {
                val rate = rates.convert(1.0, account.currency, GlobalConfig.baseCurrency)
                val converted = rates.convert(account.amount, account.currency, GlobalConfig.baseCurrency)
                UiAccount(
                    id = account.id,
                    title = account.title,
                    emoji = account.emoji,
                    originalAmount = account.amount,
                    originalCurrency = account.currency,
                    baseCurrencyAmount = converted,
                    exchangeRate = rate
                )
            }
        }
    }
}

fun List<UiAccount>.toAccounts(): List<Account> {
    return this.map { uiAccount ->
        Account(
            id = uiAccount.id,
            title = uiAccount.title,
            emoji = uiAccount.emoji,
            amount = uiAccount.originalAmount,
            currency = uiAccount.originalCurrency
        )
    }
}
