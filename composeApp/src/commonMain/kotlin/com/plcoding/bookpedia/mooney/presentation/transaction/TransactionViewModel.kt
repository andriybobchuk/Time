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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    //val state: StateFlow<AccountState> = _uiState
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
        observeTransactionsJob = repository
            .getAllTransactions()
            .filterNotNull()
            .onEach { transactions ->
                val sorted = transactions
                    .filterNotNull()
                    .sortedByDescending { it.date }

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

//    fun addTransaction(input: Transaction) {
//        val account = repository.getAccountById(input.account.id) ?: return
//        val subcategory = repository.getCategoryById(input.subcategory.id) ?: return
//
//        val transaction = Transaction(
//            id = input.id ?: Random.nextInt(),
//            amount = input.amount,
//            account = account,
//            subcategory = subcategory
//        )
//
//        if (input.id != null) {
//            repository.updateTransaction(transaction)
//        } else {
//            repository.addTransaction(transaction)
//        }
//
//        loadTransactions()
//    }


//    fun updateTransaction(transaction: Transaction) {
//        repository.updateTransaction(transaction)
//        loadTransactions()
//        loadTotal()
//    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
           // loadTransactions()
            observeTransactions()
            loadTotal()
        }
    }
}


//fun List<Transaction>.toUiTransactions(rates: ExchangeRates): List<Transaction> {
//    return map { tx ->
//        val baseCurrency = GlobalConfig.baseCurrency
//
//        val rate = if (tx.account.currency != baseCurrency)
//            rates.convert(1.0, tx.account.currency, baseCurrency)
//        else null
//
//        val baseAmount = if (rate != null)
//            rates.convert(tx.amount, tx.account.currency, baseCurrency)
//        else tx.amount
//
//        Transaction(
//            id = tx.id,
//            subcategory = tx.subcategory,
//            amount = tx.amount,
//            account = t,
//            emoji = tx.subcategory.resolveEmoji(),
//            subcategoryTitle = tx.subcategory.title,
//            amountOriginal = tx.amount,
//            originalCurrency = tx.account.currency,
//            amountInBaseCurrency = baseAmount,
//            exchangeRate = rate,
//            accountTitle = tx.account.title
//        )
//    }
//}
