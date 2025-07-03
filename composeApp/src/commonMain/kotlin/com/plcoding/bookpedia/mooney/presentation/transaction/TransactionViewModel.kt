package com.recallit.transactions.presentation

import UiAccount
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.bookpedia.mooney.data.GlobalConfig
import com.plcoding.bookpedia.mooney.domain.Category
import com.plcoding.bookpedia.mooney.domain.CategoryType
import com.plcoding.bookpedia.mooney.domain.CoreRepository
import com.plcoding.bookpedia.mooney.domain.Currency
import com.plcoding.bookpedia.mooney.domain.Transaction
import com.plcoding.bookpedia.mooney.domain.toEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import toUiAccounts

data class TransactionState(
    val transactions: List<Transaction?> = emptyList(),
    val accounts: List<UiAccount?> = emptyList(),
    val categories: List<Category> = emptyList(),
    val total: Double = 0.0,
    val totalCurrency: Currency = GlobalConfig.baseCurrency,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)
class TransactionViewModel(
    private val repository: CoreRepository
) : ViewModel() {

    private var observeTransactionsJob: Job? = null

    private val _uiState = MutableStateFlow(TransactionState())
    val state = _uiState
        .onStart {
            observeTransactions()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    private fun observeTransactions() {
        observeTransactionsJob?.cancel()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startOfMonth = LocalDate(today.year, today.month, 1)

        val (nextMonthYear, nextMonth) = if (today.month == Month.DECEMBER) {
            today.year + 1 to Month.JANUARY
        } else {
            today.year to Month.values()[today.month.ordinal + 1]
        }
        val startOfNextMonth = LocalDate(nextMonthYear, nextMonth, 1)

        observeTransactionsJob = repository
            .getAllTransactions()
            .filterNotNull()
            .map { transactions ->
                transactions.filterNotNull().filter {
                    it.date >= startOfMonth && it.date < startOfNextMonth
                }
            }
            .onEach { filteredTransactions ->
                val sorted = filteredTransactions.sortedByDescending { it.date }
                _uiState.update { it.copy(transactions = sorted) }
                loadTotal()
            }
            .launchIn(viewModelScope)
    }


    private val availableCurrencies = GlobalConfig.testExchangeRates.rates.keys.toList()
    private var selectedCurrencyIndex = 0
    private var selectedCurrency = GlobalConfig.baseCurrency

    init {
        loadDataForBottomSheet()
        //loadTotal()
    }

    private fun loadTotal() {
        val totalPln = _uiState.value.transactions.filterNotNull().filter {
            it.subcategory.type == CategoryType.EXPENSE && !it.subcategory.title.contains("ZUS") && !it.subcategory.title.contains("PIT")
        }.sumOf { it.amount }

        val converted = if (selectedCurrency != GlobalConfig.baseCurrency) {
            GlobalConfig.testExchangeRates.convert(
                totalPln,
                from = GlobalConfig.baseCurrency,
                to = selectedCurrency
            )
        } else totalPln

        _uiState.update {
            it.copy(
                total = converted,
                totalCurrency = selectedCurrency
            )
        }
    }

    private fun loadDataForBottomSheet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllAccounts().collect { list ->
                    val categories = repository.getAllCategories()
                    _uiState.update {
                        it.copy(
                            accounts = list.toUiAccounts(GlobalConfig.testExchangeRates),
                            categories = categories,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isError = true, isLoading = false) }
            }
        }
    }

    fun onTotalCurrencyClick() {
        selectedCurrencyIndex = (selectedCurrencyIndex + 1) % availableCurrencies.size
        selectedCurrency = availableCurrencies[selectedCurrencyIndex]
        loadTotal()
    }

    fun upsertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.upsertTransaction(transaction)
            //loadTransactions()
            observeTransactions()
            loadTotal()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
           // loadTransactions()
            observeTransactions()
            loadTotal()
        }
    }
}
