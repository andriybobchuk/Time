package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.AnalyticsPeriod
import com.andriybobchuk.time.time.domain.PeriodDetails
import com.andriybobchuk.time.time.domain.JobDetailBreakdown
import com.andriybobchuk.time.time.domain.EffectivenessBreakdown
import com.andriybobchuk.time.time.domain.usecase.GetWeekAnalyticsUseCase
import com.andriybobchuk.time.time.domain.usecase.GetSixWeekAnalyticsUseCase
import com.andriybobchuk.time.time.domain.usecase.GetSixMonthAnalyticsUseCase
import com.andriybobchuk.time.time.domain.usecase.GetWorkPatternInsightsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AdvancedAnalyticsViewModel(
    private val getWeekAnalyticsUseCase: GetWeekAnalyticsUseCase,
    private val getSixWeekAnalyticsUseCase: GetSixWeekAnalyticsUseCase,
    private val getSixMonthAnalyticsUseCase: GetSixMonthAnalyticsUseCase,
    private val getWorkPatternInsightsUseCase: GetWorkPatternInsightsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AdvancedAnalyticsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _state.value
    )

    init {
        loadAnalytics(AnalyticsPeriod.WEEK)
        loadWorkPatternInsights()
    }

    fun onAction(action: AnalyticsAction) {
        when (action) {
            is AnalyticsAction.SelectPeriod -> selectPeriod(action.period)
            is AnalyticsAction.SelectDateForDetails -> selectDateForDetails(action.date)
            is AnalyticsAction.SelectWeekForDetails -> selectWeekForDetails(action.weekNumber)
            is AnalyticsAction.SelectMonthForDetails -> selectMonthForDetails(action.monthName)
            is AnalyticsAction.ClearSelection -> clearSelection()
            is AnalyticsAction.Refresh -> refresh()
        }
    }

    private fun selectPeriod(period: AnalyticsPeriod) {
        _state.update { it.copy(selectedPeriod = period, selectedPeriodDetails = null) }
        loadAnalytics(period)
    }

    private fun loadAnalytics(period: AnalyticsPeriod) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                when (period) {
                    AnalyticsPeriod.WEEK -> {
                        val analytics = getWeekAnalyticsUseCase()
                        _state.update { 
                            it.copy(
                                weekAnalytics = analytics,
                                isLoading = false
                            ) 
                        }
                    }
                    AnalyticsPeriod.SIX_WEEKS -> {
                        val analytics = getSixWeekAnalyticsUseCase()
                        _state.update { 
                            it.copy(
                                sixWeekAnalytics = analytics,
                                isLoading = false
                            ) 
                        }
                    }
                    AnalyticsPeriod.SIX_MONTHS -> {
                        val analytics = getSixMonthAnalyticsUseCase()
                        _state.update { 
                            it.copy(
                                sixMonthAnalytics = analytics,
                                isLoading = false
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load analytics"
                    ) 
                }
            }
        }
    }

    private fun loadWorkPatternInsights() {
        viewModelScope.launch {
            try {
                val insights = getWorkPatternInsightsUseCase()
                _state.update { it.copy(workPatternInsights = insights) }
            } catch (e: Exception) {
                // Silently handle insights loading error, it's supplementary data
            }
        }
    }

    private fun selectDateForDetails(date: LocalDate) {
        val currentState = _state.value
        val weekAnalytics = currentState.weekAnalytics
        
        if (weekAnalytics != null && currentState.selectedPeriod == AnalyticsPeriod.WEEK) {
            val selectedDay = weekAnalytics.dailyTrends.find { it.date == date }
            if (selectedDay != null) {
                val details = PeriodDetails(
                    period = AnalyticsPeriod.WEEK,
                    selectedDate = date,
                    selectedWeek = null,
                    selectedMonth = null,
                    totalHours = selectedDay.totalHours,
                    averageDailyHours = selectedDay.totalHours, // Single day
                    jobBreakdowns = selectedDay.jobHours.map { (jobId, hours) ->
                        val jobAnalytics = weekAnalytics.jobBreakdown[jobId]
                        JobDetailBreakdown(
                            jobId = jobId,
                            jobName = jobAnalytics?.jobName ?: "Unknown",
                            totalHours = hours,
                            averageDailyHours = hours,
                            percentage = if (selectedDay.totalHours > 0) (hours / selectedDay.totalHours) * 100 else 0.0,
                            effectivenessRate = selectedDay.effectivenessRate
                        )
                    },
                    effectivenessBreakdown = EffectivenessBreakdown(
                        productiveHours = selectedDay.totalHours * (selectedDay.effectivenessRate / 100.0),
                        meetingHours = selectedDay.totalHours * (1.0 - selectedDay.effectivenessRate / 100.0),
                        productivePercentage = selectedDay.effectivenessRate,
                        averageSessionDuration = if (selectedDay.jobHours.isNotEmpty()) selectedDay.totalHours / selectedDay.jobHours.size else 0.0
                    )
                )
                _state.update { it.copy(selectedPeriodDetails = details) }
            }
        }
    }

    private fun selectWeekForDetails(weekNumber: Int) {
        val currentState = _state.value
        val sixWeekAnalytics = currentState.sixWeekAnalytics
        
        if (sixWeekAnalytics != null && currentState.selectedPeriod == AnalyticsPeriod.SIX_WEEKS) {
            val selectedWeek = sixWeekAnalytics.weeklyTrends.find { it.weekNumber == weekNumber }
            if (selectedWeek != null) {
                val details = PeriodDetails(
                    period = AnalyticsPeriod.SIX_WEEKS,
                    selectedDate = null,
                    selectedWeek = weekNumber,
                    selectedMonth = null,
                    totalHours = selectedWeek.totalHours,
                    averageDailyHours = selectedWeek.averageDailyHours,
                    jobBreakdowns = selectedWeek.jobHours.map { (jobId, hours) ->
                        val jobAnalytics = sixWeekAnalytics.jobBreakdown[jobId]
                        JobDetailBreakdown(
                            jobId = jobId,
                            jobName = jobAnalytics?.jobName ?: "Unknown",
                            totalHours = hours,
                            averageDailyHours = selectedWeek.averageDailyHours,
                            percentage = if (selectedWeek.totalHours > 0) (hours / selectedWeek.totalHours) * 100 else 0.0,
                            effectivenessRate = selectedWeek.effectivenessRate
                        )
                    },
                    effectivenessBreakdown = EffectivenessBreakdown(
                        productiveHours = selectedWeek.totalHours * (selectedWeek.effectivenessRate / 100.0),
                        meetingHours = selectedWeek.totalHours * (1.0 - selectedWeek.effectivenessRate / 100.0),
                        productivePercentage = selectedWeek.effectivenessRate,
                        averageSessionDuration = if (selectedWeek.jobHours.isNotEmpty()) selectedWeek.totalHours / selectedWeek.jobHours.size else 0.0
                    )
                )
                _state.update { it.copy(selectedPeriodDetails = details) }
            }
        }
    }

    private fun selectMonthForDetails(monthName: String) {
        val currentState = _state.value
        val sixMonthAnalytics = currentState.sixMonthAnalytics
        
        if (sixMonthAnalytics != null && currentState.selectedPeriod == AnalyticsPeriod.SIX_MONTHS) {
            val selectedMonth = sixMonthAnalytics.monthlyTrends.find { it.monthName == monthName }
            if (selectedMonth != null) {
                val details = PeriodDetails(
                    period = AnalyticsPeriod.SIX_MONTHS,
                    selectedDate = null,
                    selectedWeek = null,
                    selectedMonth = monthName,
                    totalHours = selectedMonth.totalHours,
                    averageDailyHours = selectedMonth.averageDailyHours,
                    jobBreakdowns = selectedMonth.jobHours.map { (jobId, hours) ->
                        val jobAnalytics = sixMonthAnalytics.jobBreakdown[jobId]
                        JobDetailBreakdown(
                            jobId = jobId,
                            jobName = jobAnalytics?.jobName ?: "Unknown",
                            totalHours = hours,
                            averageDailyHours = selectedMonth.averageDailyHours,
                            percentage = if (selectedMonth.totalHours > 0) (hours / selectedMonth.totalHours) * 100 else 0.0,
                            effectivenessRate = selectedMonth.effectivenessRate
                        )
                    },
                    effectivenessBreakdown = EffectivenessBreakdown(
                        productiveHours = selectedMonth.totalHours * (selectedMonth.effectivenessRate / 100.0),
                        meetingHours = selectedMonth.totalHours * (1.0 - selectedMonth.effectivenessRate / 100.0),
                        productivePercentage = selectedMonth.effectivenessRate,
                        averageSessionDuration = if (selectedMonth.jobHours.isNotEmpty()) selectedMonth.totalHours / selectedMonth.jobHours.size else 0.0
                    )
                )
                _state.update { it.copy(selectedPeriodDetails = details) }
            }
        }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedPeriodDetails = null) }
    }

    private fun refresh() {
        loadAnalytics(_state.value.selectedPeriod)
        loadWorkPatternInsights()
    }
}