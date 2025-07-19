package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.usecase.GetWeeklyAnalyticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

class AnalyticsViewModel(
    private val getWeeklyAnalyticsUseCase: GetWeeklyAnalyticsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _state.value
    )

    init {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weekStart = getWeekStart(currentDate)
        _state.update { it.copy(selectedWeekStart = weekStart) }
        
        loadWeeklyAnalytics(weekStart)
    }

    fun onAction(action: AnalyticsAction) {
        when (action) {
            is AnalyticsAction.SelectWeek -> selectWeek(action.weekStart)
        }
    }

    private fun selectWeek(weekStart: LocalDate) {
        _state.update { it.copy(selectedWeekStart = weekStart) }
        loadWeeklyAnalytics(weekStart)
    }

    private fun loadWeeklyAnalytics(weekStart: LocalDate) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val analytics = getWeeklyAnalyticsUseCase(weekStart)
                _state.update { 
                    it.copy(
                        weeklyAnalytics = analytics,
                        isLoading = false
                    ) 
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

    private fun getWeekStart(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.isoDayNumber
        val daysToSubtract = if (dayOfWeek == 1) 0 else dayOfWeek - 1
        return date.minus(DatePeriod(days = daysToSubtract))
    }
} 