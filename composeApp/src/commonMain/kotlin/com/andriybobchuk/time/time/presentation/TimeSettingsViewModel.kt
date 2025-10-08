package com.andriybobchuk.time.time.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriybobchuk.time.time.domain.Job
import com.andriybobchuk.time.time.domain.usecase.GetAllJobsUseCase
import com.andriybobchuk.time.time.domain.usecase.UpsertJobUseCase
import com.andriybobchuk.time.time.domain.usecase.DeleteJobUseCase
import com.andriybobchuk.time.time.domain.usecase.SetMainJobUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TimeSettingsState(
    val jobs: List<Job> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddEditSheet: Boolean = false,
    val editingJob: Job? = null,
    val showDeleteConfirmation: Job? = null,
    val selectedJobForDetails: Job? = null
)

sealed class TimeSettingsAction {
    object ShowAddJobSheet : TimeSettingsAction()
    data class ShowEditJobSheet(val job: Job) : TimeSettingsAction()
    object HideSheets : TimeSettingsAction()
    data class SaveJob(
        val id: String?,
        val name: String,
        val color: Long,
        val isMain: Boolean,
        val hourlyRatePLN: Double?,
        val isHidden: Boolean
    ) : TimeSettingsAction()
    data class DeleteJob(val job: Job) : TimeSettingsAction()
    data class ConfirmDeleteJob(val job: Job) : TimeSettingsAction()
    object CancelDelete : TimeSettingsAction()
    data class ToggleJobVisibility(val job: Job) : TimeSettingsAction()
    data class SetMainJob(val jobId: String) : TimeSettingsAction()
    data class ShowJobDetails(val job: Job) : TimeSettingsAction()
    object HideJobDetails : TimeSettingsAction()
    object ClearError : TimeSettingsAction()
}

class TimeSettingsViewModel(
    private val getAllJobsUseCase: GetAllJobsUseCase,
    private val upsertJobUseCase: UpsertJobUseCase,
    private val deleteJobUseCase: DeleteJobUseCase,
    private val setMainJobUseCase: SetMainJobUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(TimeSettingsState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _state.value
    )
    
    init {
        loadJobs()
    }
    
    fun onAction(action: TimeSettingsAction) {
        when (action) {
            is TimeSettingsAction.ShowAddJobSheet -> showAddJobSheet()
            is TimeSettingsAction.ShowEditJobSheet -> showEditJobSheet(action.job)
            is TimeSettingsAction.HideSheets -> hideSheets()
            is TimeSettingsAction.SaveJob -> saveJob(
                action.id,
                action.name,
                action.color,
                action.isMain,
                action.hourlyRatePLN,
                action.isHidden
            )
            is TimeSettingsAction.DeleteJob -> requestDeleteJob(action.job)
            is TimeSettingsAction.ConfirmDeleteJob -> deleteJob(action.job)
            is TimeSettingsAction.CancelDelete -> cancelDelete()
            is TimeSettingsAction.ToggleJobVisibility -> toggleJobVisibility(action.job)
            is TimeSettingsAction.SetMainJob -> setMainJob(action.jobId)
            is TimeSettingsAction.ShowJobDetails -> showJobDetails(action.job)
            is TimeSettingsAction.HideJobDetails -> hideJobDetails()
            is TimeSettingsAction.ClearError -> clearError()
        }
    }
    
    private fun loadJobs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                getAllJobsUseCase().collect { jobs ->
                    _state.update { 
                        it.copy(
                            jobs = jobs,
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load jobs"
                    ) 
                }
            }
        }
    }
    
    private fun showAddJobSheet() {
        _state.update { 
            it.copy(
                showAddEditSheet = true,
                editingJob = null
            ) 
        }
    }
    
    private fun showEditJobSheet(job: Job) {
        _state.update { 
            it.copy(
                showAddEditSheet = true,
                editingJob = job
            ) 
        }
    }
    
    private fun hideSheets() {
        _state.update { 
            it.copy(
                showAddEditSheet = false,
                editingJob = null
            ) 
        }
    }
    
    private fun saveJob(
        id: String?,
        name: String,
        color: Long,
        isMain: Boolean,
        hourlyRatePLN: Double?,
        isHidden: Boolean
    ) {
        viewModelScope.launch {
            val job = Job(
                id = id ?: "job_${Random.nextLong()}",
                name = name,
                color = color,
                isMain = isMain,
                hourlyRatePLN = hourlyRatePLN,
                isHidden = isHidden
            )
            
            upsertJobUseCase(job).onSuccess {
                hideSheets()
            }.onFailure { e ->
                _state.update { 
                    it.copy(error = e.message ?: "Failed to save job") 
                }
            }
        }
    }
    
    private fun requestDeleteJob(job: Job) {
        _state.update { it.copy(showDeleteConfirmation = job) }
    }
    
    private fun deleteJob(job: Job) {
        viewModelScope.launch {
            deleteJobUseCase(job).onSuccess {
                cancelDelete()
            }.onFailure { e ->
                _state.update { 
                    it.copy(error = e.message ?: "Failed to delete job") 
                }
                cancelDelete()
            }
        }
    }
    
    private fun cancelDelete() {
        _state.update { it.copy(showDeleteConfirmation = null) }
    }
    
    private fun toggleJobVisibility(job: Job) {
        viewModelScope.launch {
            val updatedJob = job.copy(isHidden = !job.isHidden)
            upsertJobUseCase(updatedJob).onFailure { e ->
                _state.update { 
                    it.copy(error = e.message ?: "Failed to toggle job visibility") 
                }
            }
        }
    }
    
    private fun setMainJob(jobId: String) {
        viewModelScope.launch {
            setMainJobUseCase(jobId).onFailure { e ->
                _state.update { 
                    it.copy(error = e.message ?: "Failed to set main job") 
                }
            }
        }
    }
    
    private fun showJobDetails(job: Job) {
        _state.update { it.copy(selectedJobForDetails = job) }
    }
    
    private fun hideJobDetails() {
        _state.update { it.copy(selectedJobForDetails = null) }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}