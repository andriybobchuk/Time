package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.usecase.GetLast7DaysAnalyticsUseCase
import com.andriybobchuk.time.time.domain.usecase.GetJobsUseCase
import com.andriybobchuk.time.time.domain.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val getLast7DaysAnalyticsUseCase: GetLast7DaysAnalyticsUseCase,
    private val getJobsUseCase: GetJobsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _state.value
    )

    init {
        loadLast7DaysAnalytics()
        loadJobs()
    }

    private fun loadLast7DaysAnalytics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val analytics = getLast7DaysAnalyticsUseCase()
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
    
    private fun loadJobs() {
        viewModelScope.launch {
            getJobsUseCase().collect { jobs ->
                _state.update { it.copy(jobs = jobs) }
            }
        }
    }
} 