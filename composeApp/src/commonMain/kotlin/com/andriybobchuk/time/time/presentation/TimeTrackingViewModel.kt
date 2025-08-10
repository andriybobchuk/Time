package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.usecase.DeleteTimeBlockUseCase
import com.andriybobchuk.time.time.domain.usecase.GetActiveTimeBlockUseCase
import com.andriybobchuk.time.time.domain.usecase.GetDailySummaryUseCase
import com.andriybobchuk.time.time.domain.usecase.GetJobsUseCase
import com.andriybobchuk.time.time.domain.usecase.GetTimeBlocksUseCase
import com.andriybobchuk.time.time.domain.usecase.StartTimeTrackingUseCase
import com.andriybobchuk.time.time.domain.usecase.StopTimeTrackingUseCase
import com.andriybobchuk.time.time.domain.usecase.UpsertTimeBlockUseCase
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
    private val deleteTimeBlockUseCase: DeleteTimeBlockUseCase,
    private val upsertTimeBlockUseCase: UpsertTimeBlockUseCase
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
            is TimeTrackingAction.CancelTracking -> cancelTracking()
            is TimeTrackingAction.StopTrackingWithEffectiveness -> stopTrackingWithEffectiveness(action.effectiveness)
            is TimeTrackingAction.SelectDate -> selectDate(action.date)
            is TimeTrackingAction.DeleteTimeBlock -> deleteTimeBlock(action.id)
            is TimeTrackingAction.EditTimeBlock -> showEditSheet(action.timeBlock)
            is TimeTrackingAction.ShowAddSheet -> showAddSheet()
            is TimeTrackingAction.HideEditSheet -> hideEditSheet()
            is TimeTrackingAction.HideAddSheet -> hideAddSheet()
            is TimeTrackingAction.UpdateTimeBlock -> updateTimeBlock(action.timeBlock)
            is TimeTrackingAction.AddTimeBlock -> addTimeBlock(action.jobId, action.startTime, action.endTime, action.effectiveness)
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

    private fun stopTrackingWithEffectiveness(effectiveness: com.andriybobchuk.time.time.domain.Effectiveness) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val activeBlock = state.value.activeTimeBlock
            if (activeBlock == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val endTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val updatedBlock = activeBlock.copy(
                endTime = endTime,
                duration = activeBlock.calculateDuration(),
                effectiveness = effectiveness
            )
            try {
                upsertTimeBlockUseCase(updatedBlock)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun cancelTracking() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val activeBlock = state.value.activeTimeBlock
            if (activeBlock == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            try {
                deleteTimeBlockUseCase(activeBlock.id)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to cancel tracking"
                    ) 
                }
            }
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
    
    private fun showEditSheet(timeBlock: TimeBlock) {
        _state.update { it.copy(showEditSheet = true, editingTimeBlock = timeBlock) }
    }
    
    private fun showAddSheet() {
        _state.update { it.copy(showAddSheet = true) }
    }
    
    private fun hideEditSheet() {
        _state.update { it.copy(showEditSheet = false, editingTimeBlock = null) }
    }
    
    private fun hideAddSheet() {
        _state.update { it.copy(showAddSheet = false) }
    }
    
    private fun updateTimeBlock(timeBlock: TimeBlock) {
        viewModelScope.launch {
            try {
                upsertTimeBlockUseCase(timeBlock)
                hideEditSheet()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun addTimeBlock(jobId: String, startTime: kotlinx.datetime.LocalDateTime, endTime: kotlinx.datetime.LocalDateTime, effectiveness: com.andriybobchuk.time.time.domain.Effectiveness?) {
        viewModelScope.launch {
            try {
                val job = getJobsUseCase().find { it.id == jobId }
                if (job != null) {
                    val timeBlock = TimeBlock(
                        jobId = jobId,
                        jobName = job.name,
                        startTime = startTime,
                        endTime = endTime,
                        effectiveness = effectiveness
                    )
                    upsertTimeBlockUseCase(timeBlock)
                    hideAddSheet()
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
} 