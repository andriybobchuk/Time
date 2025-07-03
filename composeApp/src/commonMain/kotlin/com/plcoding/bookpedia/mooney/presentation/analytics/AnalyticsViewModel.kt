package com.plcoding.bookpedia.mooney.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.bookpedia.mooney.data.GlobalConfig
import com.plcoding.bookpedia.mooney.domain.CategoryType
import com.plcoding.bookpedia.mooney.domain.CoreRepository
import com.plcoding.bookpedia.mooney.domain.Currency
import com.plcoding.bookpedia.mooney.domain.Transaction
import com.plcoding.bookpedia.mooney.presentation.formatWithCommas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AnalyticsViewModel(
    private val repository: CoreRepository,
) : ViewModel() {
    private val baseCurrency: Currency = GlobalConfig.baseCurrency
    private val calculators: List<AnalyticsMetricCalculator> = listOf(
        RevenueCalculator(),
        TaxesCalculator(),
        OperatingCostsCalculator(),
        NetIncomeCalculator(),
        BurnRateCalculator()
    )

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state

    init {
        loadMetricsForMonth(_state.value.selectedMonth)
    }

    fun onMonthSelected(month: MonthKey) {
        _state.update { it.copy(selectedMonth = month) }
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

            val metrics = calculators.map { it.calculate(transactions, month, baseCurrency) }

            _state.update {
                it.copy(metrics = metrics, isLoading = false)
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
        transactions: List<Transaction>,
        month: MonthKey,
        baseCurrency: Currency
    ): AnalyticsMetric
}

data class AnalyticsState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val metrics: List<AnalyticsMetric> = emptyList(),
    //    val currency: Currency = GlobalConfig.baseCurrency,
    val isLoading: Boolean = false
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

class RevenueCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency): AnalyticsMetric {
        val revenue = transactions
            .filter { it.subcategory.type == CategoryType.INCOME }
            .sumOf { it.amount }
        return AnalyticsMetric("Revenue", format(revenue, baseCurrency))
    }
}

class TaxesCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency): AnalyticsMetric {
        val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }
        val taxes = transactions.filter {
            it.subcategory.title.contains("ZUS", ignoreCase = true) ||
                    it.subcategory.title.contains("PIT", ignoreCase = true)
        }.sumOf { it.amount }

        val subtitle = if (revenue > 0) "${percentage(taxes, revenue)} of revenue" else "–"
        return AnalyticsMetric("Taxes", format(taxes, baseCurrency), subtitle)
    }
}

class OperatingCostsCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency): AnalyticsMetric {
        val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }
        val expenses = transactions.filter {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }.sumOf { it.amount }

        val subtitle = if (revenue > 0) "${percentage(expenses, revenue)} of revenue" else "–"
        return AnalyticsMetric("Operating Costs", format(expenses, baseCurrency), subtitle)
    }
}

class NetIncomeCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency): AnalyticsMetric {
        val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }
        val taxes = transactions.filter {
            it.subcategory.title.contains("ZUS") || it.subcategory.title.contains("PIT")
        }.sumOf { it.amount }
        val expenses = transactions.filter {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }.sumOf { it.amount }

        val netIncome = revenue - taxes - expenses
        val subtitle = if (revenue > 0) "${percentage(netIncome, revenue)} of revenue" else "–"
        return AnalyticsMetric("Net Income", format(netIncome, baseCurrency), subtitle)
    }
}

class BurnRateCalculator : AnalyticsMetricCalculator {
    override suspend fun calculate(transactions: List<Transaction>, month: MonthKey, baseCurrency: Currency): AnalyticsMetric {
        val revenue = transactions.filter { it.subcategory.type == CategoryType.INCOME }.sumOf { it.amount }
        val expenses = transactions.filter {
            it.subcategory.type == CategoryType.EXPENSE &&
                    !it.subcategory.title.contains("ZUS") &&
                    !it.subcategory.title.contains("PIT")
        }.sumOf { it.amount }

        val burnRate = if (expenses > 0) expenses / 30 else 0.0
        //val burnRate = if (expenses > 0) expenses / month.firstDay().lengthOfMonth() else 0.0 TODO
        val subtitle = if (revenue > 0) "${percentage(burnRate, revenue)} of revenue per day" else "–"
        return AnalyticsMetric("Burn Rate", format(burnRate, baseCurrency), subtitle)
    }
}

