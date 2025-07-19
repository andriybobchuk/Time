package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.usecase.DeleteTimeBlockUseCase
import com.andriybobchuk.time.time.domain.usecase.GetActiveTimeBlockUseCase
import com.andriybobchuk.time.time.domain.usecase.GetDailySummaryUseCase
import com.andriybobchuk.time.time.domain.usecase.GetJobsUseCase
import com.andriybobchuk.time.time.domain.usecase.GetTimeBlocksUseCase
import com.andriybobchuk.time.time.domain.usecase.StartTimeTrackingUseCase
import com.andriybobchuk.time.time.domain.usecase.StopTimeTrackingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TimeTrackingViewModel(
    private val getTimeBlocksUseCase: GetTimeBlocksUseCase,
    private val getActiveTimeBlockUseCase: GetActiveTimeBlockUseCase,
    private val getJobsUseCase: GetJobsUseCase,
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val startTimeTrackingUseCase: StartTimeTrackingUseCase,
    private val stopTimeTrackingUseCase: StopTimeTrackingUseCase,
    private val deleteTimeBlockUseCase: DeleteTimeBlockUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TimeTrackingState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _state.value
    )

    private var observeTimeBlocksJob: Job? = null
    private var observeActiveBlockJob: Job? = null

    init {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        _state.update { it.copy(selectedDate = currentDate) }
        
        loadJobs()
        observeTimeBlocks(currentDate)
        observeActiveTimeBlock()
    }

    fun onAction(action: TimeTrackingAction) {
        when (action) {
            is TimeTrackingAction.StartTracking -> startTracking(action.jobId)
            is TimeTrackingAction.StopTracking -> stopTracking()
            is TimeTrackingAction.SelectDate -> selectDate(action.date)
            is TimeTrackingAction.DeleteTimeBlock -> deleteTimeBlock(action.id)
        }
    }

    private fun loadJobs() {
        val jobs = getJobsUseCase()
        _state.update { it.copy(jobs = jobs) }
    }

    private fun observeTimeBlocks(date: LocalDate) {
        observeTimeBlocksJob?.cancel()
        
        observeTimeBlocksJob = getTimeBlocksUseCase(date)
            .onEach { blocks ->
                _state.update { it.copy(timeBlocks = blocks) }
                loadDailySummary(date)
            }
            .launchIn(viewModelScope)
    }

    private fun observeActiveTimeBlock() {
        observeActiveBlockJob?.cancel()
        
        observeActiveBlockJob = getActiveTimeBlockUseCase()
            .onEach { activeBlock ->
                _state.update { it.copy(activeTimeBlock = activeBlock) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadDailySummary(date: LocalDate) {
        viewModelScope.launch {
            try {
                val summary = getDailySummaryUseCase(date)
                _state.update { it.copy(dailySummary = summary) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun startTracking(jobId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = startTimeTrackingUseCase(jobId)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                },
                onFailure = { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = exception.message ?: "Failed to start tracking"
                        ) 
                    }
                }
            )
        }
    }

    private fun stopTracking() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = stopTimeTrackingUseCase()
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                },
                onFailure = { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = exception.message ?: "Failed to stop tracking"
                        ) 
                    }
                }
            )
        }
    }

    private fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        observeTimeBlocks(date)
    }

    private fun deleteTimeBlock(id: Int) {
        viewModelScope.launch {
            try {
                deleteTimeBlockUseCase(id)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
} 