package com.andriybobchuk.time.time.domain

import kotlinx.datetime.LocalDate

// Models for the redesigned analytics

data class EightWeekTrend(
    val weeks: List<WeekData>,
    val jobs: List<Job>
)

data class WeekData(
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val jobHours: Map<String, Double>, // jobId -> hours
    val totalHours: Double,
    val isSelected: Boolean = false
)

data class WeekDetails(
    val weekData: WeekData,
    val effectiveHours: Map<String, Double>, // jobId -> effective hours
    val contextSwitchHours: Double,
    val totalTrackedTime: Double, // effective + context switch
    val averageDailyHours: Map<String, Double>, // jobId -> avg daily
    val effectivenessRate: Map<String, Double> // jobId -> effectiveness %
)

data class ContextSwitchAnalysis(
    val totalContextSwitchTime: Double,
    val totalEffectiveTime: Double,
    val totalTrackedTime: Double,
    val contextSwitchPercentage: Double
)