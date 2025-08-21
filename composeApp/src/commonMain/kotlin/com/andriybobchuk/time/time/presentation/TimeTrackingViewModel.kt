package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.usecase.DeleteTimeBlockUseCase
import com.andriybobchuk.time.time.domain.usecase.GetActiveTimeBlockUseCase
import com.andriybobchuk.time.time.domain.usecase.GetDailySummaryUseCase
import com.andriybobchuk.time.time.domain.usecase.GetJobsUseCase
import com.andriybobchuk.time.time.domain.usecase.GetStatusUpdatesUseCase
import com.andriybobchuk.time.time.domain.usecase.GetTimeBlocksUseCase
import com.andriybobchuk.time.time.domain.usecase.StartTimeTrackingUseCase
import com.andriybobchuk.time.time.domain.usecase.StopTimeTrackingUseCase
import com.andriybobchuk.time.time.domain.usecase.UpsertStatusUpdateUseCase
import com.andriybobchuk.time.time.domain.usecase.UpsertTimeBlockUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
    private val upsertTimeBlockUseCase: UpsertTimeBlockUseCase,
    private val getStatusUpdatesUseCase: GetStatusUpdatesUseCase,
    private val upsertStatusUpdateUseCase: UpsertStatusUpdateUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TimeTrackingState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _state.value
    )

    private var observeTimeBlocksJob: Job? = null
    private var observeActiveBlockJob: Job? = null
    private var observeStatusUpdatesJob: Job? = null
    private var periodicRefreshJob: Job? = null
    private var durationTickerJob: Job? = null
    private var isScreenVisible = false

    init {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        _state.update { it.copy(selectedDate = currentDate) }
        
        loadJobs()
        observeTimeBlocks(currentDate)
        observeActiveTimeBlock()
        observeStatusUpdates(currentDate)
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
            is TimeTrackingAction.AddTimeBlock -> addTimeBlock(action.jobId, action.startTime, action.endTime, action.effectiveness, action.description)
            
            // Status Updates actions
            is TimeTrackingAction.ShowStatusUpdatesSheet -> showStatusUpdatesSheet()
            is TimeTrackingAction.HideStatusUpdatesSheet -> hideStatusUpdatesSheet()
            is TimeTrackingAction.UpdateStatusText -> updateStatusText(action.jobId, action.text)
            is TimeTrackingAction.SaveStatusUpdates -> saveStatusUpdates()
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
                
                // Start or stop duration ticker based on whether there's an active block
                if (activeBlock != null && isScreenVisible) {
                    startDurationTicker()
                } else {
                    stopDurationTicker()
                }
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
        observeStatusUpdates(date)
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
    
    private fun addTimeBlock(jobId: String, startTime: kotlinx.datetime.LocalDateTime, endTime: kotlinx.datetime.LocalDateTime, effectiveness: com.andriybobchuk.time.time.domain.Effectiveness?, description: String?) {
        viewModelScope.launch {
            try {
                val job = getJobsUseCase().find { it.id == jobId }
                if (job != null) {
                    val timeBlock = TimeBlock(
                        jobId = jobId,
                        jobName = job.name,
                        startTime = startTime,
                        endTime = endTime,
                        effectiveness = effectiveness,
                        description = description
                    )
                    upsertTimeBlockUseCase(timeBlock)
                    hideAddSheet()
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    // Status Updates methods
    private fun observeStatusUpdates(date: LocalDate) {
        observeStatusUpdatesJob?.cancel()
        
        observeStatusUpdatesJob = getStatusUpdatesUseCase(date)
            .onEach { statusUpdates ->
                _state.update { 
                    it.copy(
                        statusUpdates = statusUpdates,
                        statusUpdateTexts = statusUpdates.associate { update ->
                            update.jobId to update.statusText
                        }
                    ) 
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun showStatusUpdatesSheet() {
        // Initialize status text map with current values for each job
        val currentTexts = state.value.statusUpdateTexts.toMutableMap()
        state.value.jobs.forEach { job ->
            if (!currentTexts.containsKey(job.id)) {
                currentTexts[job.id] = ""
            }
        }
        _state.update { 
            it.copy(
                showStatusUpdatesSheet = true,
                statusUpdateTexts = currentTexts
            )
        }
    }
    
    private fun hideStatusUpdatesSheet() {
        _state.update { it.copy(showStatusUpdatesSheet = false) }
    }
    
    private fun updateStatusText(jobId: String, text: String) {
        val updatedTexts = state.value.statusUpdateTexts.toMutableMap()
        updatedTexts[jobId] = text
        _state.update { it.copy(statusUpdateTexts = updatedTexts) }
    }
    
    private fun saveStatusUpdates() {
        viewModelScope.launch {
            try {
                val currentDate = state.value.selectedDate
                val statusTexts = state.value.statusUpdateTexts
                
                statusTexts.forEach { (jobId, text) ->
                    if (text.isNotBlank()) {
                        upsertStatusUpdateUseCase(jobId, currentDate, text.trim())
                    }
                }
                
                hideStatusUpdatesSheet()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    // Lifecycle-aware periodic refresh methods
    fun onScreenVisible() {
        isScreenVisible = true
        startPeriodicRefresh()
        
        // Start duration ticker if there's an active time block
        if (state.value.activeTimeBlock != null) {
            startDurationTicker()
        }
    }
    
    fun onScreenHidden() {
        isScreenVisible = false
        stopPeriodicRefresh()
        stopDurationTicker()
    }
    
    private fun startPeriodicRefresh() {
        // Cancel any existing refresh job
        periodicRefreshJob?.cancel()
        
        periodicRefreshJob = viewModelScope.launch {
            while (isScreenVisible) {
                delay(1.minutes) // Update every minute
                if (isScreenVisible) {
                    refreshData()
                }
            }
        }
    }
    
    private fun stopPeriodicRefresh() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
    }
    
    private suspend fun refreshData() {
        try {
            // Refresh daily summary for current selected date
            val currentSelectedDate = state.value.selectedDate
            loadDailySummary(currentSelectedDate)
            
            // The time blocks and active time block will be automatically updated
            // through their respective Flow observations, so no manual refresh needed
            
        } catch (e: Exception) {
            // Silently handle refresh errors to avoid disrupting user experience
            // The existing data will remain valid
        }
    }
    
    private fun startDurationTicker() {
        // Cancel any existing ticker job
        durationTickerJob?.cancel()
        
        durationTickerJob = viewModelScope.launch {
            // Update immediately first
            if (isScreenVisible && state.value.activeTimeBlock != null) {
                val currentTime = Clock.System.now().toEpochMilliseconds()
                _state.update { it.copy(durationTicker = currentTime) }
            }
            
            // Then update every minute
            while (isScreenVisible && state.value.activeTimeBlock != null) {
                delay(1.minutes)
                if (isScreenVisible && state.value.activeTimeBlock != null) {
                    // Update ticker to trigger recomposition of active time block UI
                    val currentTime = Clock.System.now().toEpochMilliseconds()
                    _state.update { it.copy(durationTicker = currentTime) }
                }
            }
        }
    }
    
    private fun stopDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPeriodicRefresh()
        stopDurationTicker()
        observeTimeBlocksJob?.cancel()
        observeActiveBlockJob?.cancel()
        observeStatusUpdatesJob?.cancel()
    }
} 