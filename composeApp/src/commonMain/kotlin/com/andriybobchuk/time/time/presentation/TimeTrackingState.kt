package com.andriybobchuk.time.time.presentation

import com.andriybobchuk.time.time.domain.DailySummary
import com.andriybobchuk.time.time.domain.Job
import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.WeeklyAnalytics
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class TimeTrackingState(
    val selectedDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val timeBlocks: List<TimeBlock> = emptyList(),
    val activeTimeBlock: TimeBlock? = null,
    val jobs: List<Job> = emptyList(),
    val dailySummary: DailySummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showEditSheet: Boolean = false,
    val editingTimeBlock: TimeBlock? = null,
    val showAddSheet: Boolean = false,
    val durationTicker: Long = 0L // Updates every minute to trigger recomposition for active blocks
)

data class AnalyticsState(
    val weeklyAnalytics: WeeklyAnalytics? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface TimeTrackingAction {
    data class StartTracking(val jobId: String) : TimeTrackingAction
    data object StopTracking : TimeTrackingAction
    data object CancelTracking : TimeTrackingAction
    data class SelectDate(val date: LocalDate) : TimeTrackingAction
    data class DeleteTimeBlock(val id: Int) : TimeTrackingAction
    data class EditTimeBlock(val timeBlock: TimeBlock) : TimeTrackingAction
    data object ShowAddSheet : TimeTrackingAction
    data object HideEditSheet : TimeTrackingAction
    data object HideAddSheet : TimeTrackingAction
    data class UpdateTimeBlock(val timeBlock: TimeBlock) : TimeTrackingAction
    data class AddTimeBlock(val jobId: String, val startTime: kotlinx.datetime.LocalDateTime, val endTime: kotlinx.datetime.LocalDateTime, val effectiveness: com.andriybobchuk.time.time.domain.Effectiveness? = null, val description: String? = null) : TimeTrackingAction
    data class StopTrackingWithEffectiveness(val effectiveness: com.andriybobchuk.time.time.domain.Effectiveness) : TimeTrackingAction
} 