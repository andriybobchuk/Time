package com.andriybobchuk.mooney.mooney.presentation.transaction


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.presentation.account.UiAccount
import com.andriybobchuk.mooney.mooney.presentation.account.toUiAccounts
import com.andriybobchuk.mooney.mooney.presentation.analytics.MonthKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class TransactionState(
    val selectedMonth: MonthKey = MonthKey.current(),
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
            observeTransactions(_uiState.value.selectedMonth)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value
        )

    fun onMonthSelected(month: MonthKey) {
        _uiState.update { it.copy(selectedMonth = month) }
        observeTransactions(month)
    }
    private fun observeTransactions(month: MonthKey) {
        observeTransactionsJob?.cancel()

        val start = month.firstDay()
        val end = month.firstDayOfNextMonth()


        observeTransactionsJob = repository
            .getAllTransactions()
            .filterNotNull()
            .map { transactions ->
                transactions.filterNotNull().filter {
                    it.date >= start && it.date < end
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
        }.sumOf {
           // it.amount
            GlobalConfig.testExchangeRates.convert(it.amount, it.account.currency, GlobalConfig.baseCurrency)
        }

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
            observeTransactions(_uiState.value.selectedMonth)
            loadTotal()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
           // loadTransactions()
            observeTransactions(_uiState.value.selectedMonth)
            loadTotal()
        }
    }
}
