package com.andriybobchuk.mooney.mooney.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.ExchangeRates
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.presentation.formatWithCommas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class AnalyticsState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val totalRevenuePlnForMonth: Double = 0.0,
    val transactionsForMonth: List<Transaction?> = emptyList(),
    val metrics: List<AnalyticsMetric> = emptyList(),
    val topCategories: List<TopCategorySummary> = emptyList(),
    val isLoading: Boolean = false
)

class AnalyticsViewModel(
    private val repository: CoreRepository,
) : ViewModel() {
    private val baseCurrency: Currency = GlobalConfig.baseCurrency
    private val calculators: List<AnalyticsMetricCalculator> = listOf(
        RevenueCalculator(GlobalConfig.testExchangeRates),
        TaxesCalculator(),
        OperatingCostsCalculator(),
        NetIncomeCalculator(),
        //BurnRateCalculator()
    )

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state

    init {
        // calculateTotalRevenuePlnForMonth(_state.value.selectedMonth)
        loadMetricsForMonth(_state.value.selectedMonth)
    }

    // todo do not delete
//    private fun calculateTotalRevenuePlnForMonth(month: MonthKey) {
//
//        val totalRevenuePln = _state.value.transactionsForMonth.filterNotNull().filter {
//            it.subcategory.type == CategoryType.INCOME
//        }.sumOf {
//            GlobalConfig.testExchangeRates.convert(it.amount, it.account.currency, baseCurrency)
//        }
//
//        _state.update {
//            it.copy(totalRevenuePlnForMonth = totalRevenuePln)
//        }
//    }

    fun onMonthSelected(month: MonthKey) {
        _state.update { it.copy(selectedMonth = month) }
        // calculateTotalRevenuePlnForMonth(_state.value.selectedMonth)
        loadMetricsForMonth(month)
    }

    private fun loadMetricsForMonth(month: MonthKey) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val start = month.firstDay()
            val end = month.firstDayOfNextMonth()


            val transactions = repository
                .getAllTransactions()
                .first()
                .filterNotNull()
                .filter { it.date >= start && it.date < end }
            _state.update { it.copy(transactionsForMonth = transactions) }

            // todo: extract to function calculateTotalRevenuePlnForMonth
            val totalRevenuePln = _state.value.transactionsForMonth.filterNotNull().filter {
                it.subcategory.type == CategoryType.INCOME
            }.sumOf {
                GlobalConfig.testExchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }
            _state.update {
                it.copy(totalRevenuePlnForMonth = totalRevenuePln)
            }

            val metrics = calculators.map { it.calculate(totalRevenuePln, transactions, month, baseCurrency) }


            ////// CAtegories //////
            //////////
            val exchangeRates = GlobalConfig.testExchangeRates

//            val totalRevenue = transactions.sumConverted(baseCurrency, exchangeRates) {
//                it.subcategory.type == CategoryType.INCOME
//            }
            val totalCostsPln = transactions.filter {
                it.subcategory.type == CategoryType.EXPENSE && !it.subcategory.title.contains("ZUS") && !it.subcategory.title.contains("PIT")
            }.sumOf {
                //it.amount
                GlobalConfig.testExchangeRates.convert(it.amount, it.account.currency, baseCurrency)
            }

            val categorySums: Map<Category, Double> = transactions
                .filter { it.subcategory.type == CategoryType.EXPENSE }
                .map { tx ->

                    val general = if (tx.subcategory.isGeneralCategory()) {
                        tx.subcategory
                    } else if (tx.subcategory.isTypeCategory()) {
                        tx.subcategory
                    } else {
                        tx.subcategory.parent!!
                    }

                    val converted = exchangeRates.convert(tx.amount, tx.account.currency, baseCurrency)
                    general to converted
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, amounts) -> amounts.sum() }

            val topCategories = categorySums
                .entries
                .sortedByDescending { it.value }
                .map { (category, totalAmount) ->
                    TopCategorySummary(
                        category = category,
                        amount = totalAmount,
                        formatted = format(totalAmount, baseCurrency),
                        percentOfRevenue = percentage(totalAmount, totalCostsPln)
                    )
                }

            _state.update {
                it.copy(
                    metrics = metrics,
                    topCategories = topCategories,
                    isLoading = false
                )
            }
        }
    }
}


fun format(amount: Double, currency: Currency): String =
    "${amount.formatWithCommas()} ${currency.symbol}"


fun percentage(part: Double, total: Double): String =
    if (total == 0.0) "–" else (part / total * 100).formatWithCommas()

data class AnalyticsMetric(
    val title: String,
    val value: String,
    val subtitle: String? = null
)

interface AnalyticsMetricCalculator {
    suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric
}

data class TopCategorySummary(
    val category: Category,
    val amount: Double,
    val formatted: String,
    val percentOfRevenue: String
)

data class MonthKey(val year: Int, val month: Int) {
    fun toDisplayString(): String = "${monthName(month)} $year"

    fun firstDay(): LocalDate = LocalDate(year, month, 1)

    fun firstDayOfNextMonth(): LocalDate {
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        return LocalDate(nextYear, nextMonth, 1)
    }

    private fun monthName(m: Int): String = when (m) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Invalid"
    }

    companion object {
        fun current(): MonthKey {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return MonthKey(now.year, now.monthNumber)
        }
    }
}

class RevenueCalculator(
    private val exchangeRates: ExchangeRates
) : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,

        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric {
//        val revenue = transactions
//            .sumConverted(baseCurrency, exchangeRates) {
//                it.subcategory.type == CategoryType.INCOME
//            }
//
//        val revenue =

        return AnalyticsMetric("Revenue", format(revenue, baseCurrency))
    }
}

fun List<Transaction>.sumConverted(
    baseCurrency: Currency,
    exchangeRates: ExchangeRates,
    filter: (Transaction) -> Boolean
): Double {
    return this
        .asSequence()
        .filter(filter)
        .sumOf { exchangeRates.convert(it.amount, it.account.currency, baseCurrency) }
}

class TaxesCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric {


        //  val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }


        val taxes = transactions.sumConverted(baseCurrency, GlobalConfig.testExchangeRates) {
            it.subcategory.title.contains("ZUS", ignoreCase = true) ||
                    it.subcategory.title.contains("PIT", ignoreCase = true)
        }

        val subtitle = if (revenue > 0) "${percentage(taxes, revenue)}% of revenue" else "–"
        return AnalyticsMetric("Taxes", format(taxes, baseCurrency), subtitle)
    }
}

class OperatingCostsCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric {
        // val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }
        val expenses = transactions.sumConverted(baseCurrency, GlobalConfig.testExchangeRates) {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }

        val subtitle = if (revenue > 0) "${percentage(expenses, revenue)}% of revenue" else "–"
        return AnalyticsMetric("Operating Costs", format(expenses, baseCurrency), subtitle)
    }
}

class NetIncomeCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency
    ): AnalyticsMetric {
        // val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }
        val taxes = transactions.filter {
            it.subcategory.title.contains("ZUS") || it.subcategory.title.contains("PIT")
        }.sumOf { it.amount }
        val expenses = transactions.sumConverted(baseCurrency, GlobalConfig.testExchangeRates) {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }

        val netIncome = revenue - taxes - expenses
        val subtitle = if (revenue > 0) "${percentage(netIncome, revenue)}% revenue" else "–"
        return AnalyticsMetric("Net Income", format(netIncome, baseCurrency), subtitle)
    }
}

class BurnRateCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(
        revenue: Double,
        transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency
    ): AnalyticsMetric {
        //  val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }
        val expenses = transactions.sumConverted(baseCurrency, GlobalConfig.testExchangeRates) {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }

        val burnRate = if (expenses > 0) expenses / 30 else 0.0
        //val burnRate = if (expenses > 0) expenses / month.firstDay().lengthOfMonth() else 0.0 TODO
        val subtitle = if (revenue > 0) "${percentage(burnRate, revenue)}% revenue/day" else "–"
        return AnalyticsMetric("Burn Rate", format(burnRate, baseCurrency), subtitle)
    }
}

