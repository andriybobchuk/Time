package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.*
import com.andriybobchuk.time.time.domain.usecase.GetEightWeekTrendUseCase
import com.andriybobchuk.time.time.domain.usecase.GetWeekDetailsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RedesignedAnalyticsState(
    val eightWeekTrend: EightWeekTrend? = null,
    val selectedWeekIndex: Int = 7, // Default to current week (last in list)
    val weekDetails: WeekDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class RedesignedAnalyticsAction {
    data class SelectWeek(val weekIndex: Int) : RedesignedAnalyticsAction()
    object Refresh : RedesignedAnalyticsAction()
}

class RedesignedAnalyticsViewModel(
    private val getEightWeekTrendUseCase: GetEightWeekTrendUseCase,
    private val getWeekDetailsUseCase: GetWeekDetailsUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(RedesignedAnalyticsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _state.value
    )
    
    init {
        loadAnalytics()
    }
    
    fun onAction(action: RedesignedAnalyticsAction) {
        when (action) {
            is RedesignedAnalyticsAction.SelectWeek -> selectWeek(action.weekIndex)
            is RedesignedAnalyticsAction.Refresh -> loadAnalytics()
        }
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                getEightWeekTrendUseCase().collect { trend ->
                    _state.update { 
                        it.copy(
                            eightWeekTrend = trend,
                            isLoading = false
                        ) 
                    }
                    
                    // Load details for the selected week
                    val selectedWeek = trend.weeks.getOrNull(_state.value.selectedWeekIndex)
                    if (selectedWeek != null) {
                        loadWeekDetails(selectedWeek)
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
    
    private fun selectWeek(weekIndex: Int) {
        val trend = _state.value.eightWeekTrend ?: return
        val selectedWeek = trend.weeks.getOrNull(weekIndex) ?: return
        
        _state.update { 
            it.copy(
                selectedWeekIndex = weekIndex,
                eightWeekTrend = trend.copy(
                    weeks = trend.weeks.mapIndexed { index, week ->
                        week.copy(isSelected = index == weekIndex)
                    }
                )
            )
        }
        
        loadWeekDetails(selectedWeek)
    }
    
    private fun loadWeekDetails(weekData: WeekData) {
        viewModelScope.launch {
            try {
                val details = getWeekDetailsUseCase(weekData)
                _state.update { it.copy(weekDetails = details) }
            } catch (e: Exception) {
                // Silently handle error for details
            }
        }
    }
}