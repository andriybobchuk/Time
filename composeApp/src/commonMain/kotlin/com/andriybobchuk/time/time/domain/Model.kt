package com.andriybobchuk.time.time.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

data class TimeBlock(
    val id: Int = 0,
    val jobId: String,
    val jobName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val duration: Long? = null // in milliseconds
) {
    fun isActive(): Boolean = endTime == null
    
    fun calculateDuration(): Long {
        val end = endTime ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val startInstant = startTime.toInstant(TimeZone.currentSystemDefault())
        val endInstant = end.toInstant(TimeZone.currentSystemDefault())
        return (endInstant - startInstant).inWholeMilliseconds
    }
    
    fun getDurationInHours(): Double {
        return calculateDuration() / (1000.0 * 60 * 60)
    }
    
    fun getFormattedDuration(): String {
        val hours = getDurationInHours()
        return when {
            hours < 1 -> "${(hours * 60).toInt()}m"
            hours == hours.toInt().toDouble() -> "${hours.toInt()}h"
            else -> {
                val wholeHours = hours.toInt()
                val minutes = ((hours - wholeHours) * 60).toInt()
                if (minutes == 0) "${wholeHours}h" else "${wholeHours}h ${minutes}m"
            }
        }
    }
}

data class Job(
    val id: String,
    val name: String,
    val color: Int
)

data class DailySummary(
    val date: LocalDate,
    val blocks: List<TimeBlock>,
    val totalHours: Double,
    val jobBreakdown: Map<String, JobSummary>
)

data class JobSummary(
    val jobId: String,
    val jobName: String,
    val totalHours: Double,
    val percentage: Double
)

data class WeeklyAnalytics(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val dailySummaries: List<DailySummary>,
    val totalHours: Double,
    val averageDailyHours: Double,
    val jobBreakdown: Map<String, JobAnalytics>
)

data class JobAnalytics(
    val jobId: String,
    val jobName: String,
    val totalHours: Double,
    val averageDailyHours: Double,
    val percentage: Double
) 