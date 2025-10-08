package com.andriybobchuk.time.time.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

enum class Effectiveness {
    Productive,
    Meetings
}

data class TimeBlock(
    val id: Int = 0,
    val jobId: String,
    val jobName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val duration: Long? = null, // in milliseconds
    val effectiveness: Effectiveness? = null, // new field
    val description: String? = null // description field for details
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
    val color: Long,
    val isMain: Boolean = false,
    val hourlyRatePLN: Double? = null,
    val isHidden: Boolean = false
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

data class StatusUpdate(
    val id: String,
    val jobId: String,
    val jobName: String,
    val date: LocalDate,
    val statusText: String,
    val lastUpdated: LocalDateTime
)

// Advanced Analytics Models
data class AdvancedAnalytics(
    val weekAnalytics: WeekAnalytics? = null,
    val sixWeekAnalytics: SixWeekAnalytics? = null,
    val sixMonthAnalytics: SixMonthAnalytics? = null,
    val workPatternInsights: WorkPatternInsights? = null
)

data class WeekAnalytics(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalWorkingDayHours: Double,
    val averageDailyHours: Double, // excluding weekends
    val jobBreakdown: Map<String, JobWeeklyAnalytics>,
    val dailyTrends: List<DayTrendData>,
    val effectivenessRate: Double
)

data class SixWeekAnalytics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalWorkingDayHours: Double,
    val averageDailyHours: Double, // excluding weekends
    val averageWeeklyHours: Double,
    val jobBreakdown: Map<String, JobSixWeekAnalytics>,
    val weeklyTrends: List<WeekTrendData>,
    val effectivenessRate: Double
)

data class SixMonthAnalytics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalWorkingDayHours: Double,
    val averageDailyHours: Double, // excluding weekends
    val averageWeeklyHours: Double,
    val averageMonthlyHours: Double,
    val jobBreakdown: Map<String, JobSixMonthAnalytics>,
    val monthlyTrends: List<MonthTrendData>,
    val effectivenessRate: Double
)

data class JobWeeklyAnalytics(
    val jobId: String,
    val jobName: String,
    val totalHours: Double,
    val averageDailyHours: Double,
    val percentage: Double,
    val effectivenessRate: Double,
    val peakWorkingHours: List<Int> // hours of day (0-23) when most productive
)

data class JobSixWeekAnalytics(
    val jobId: String,
    val jobName: String,
    val totalHours: Double,
    val averageDailyHours: Double,
    val averageWeeklyHours: Double,
    val percentage: Double,
    val effectivenessRate: Double,
    val consistencyScore: Double, // variance in daily hours
    val trendDirection: TrendDirection
)

data class JobSixMonthAnalytics(
    val jobId: String,
    val jobName: String,
    val totalHours: Double,
    val averageDailyHours: Double,
    val averageWeeklyHours: Double,
    val averageMonthlyHours: Double,
    val percentage: Double,
    val effectivenessRate: Double,
    val consistencyScore: Double,
    val trendDirection: TrendDirection,
    val seasonalPatterns: List<SeasonalPattern>
)

data class DayTrendData(
    val date: LocalDate,
    val dayName: String,
    val totalHours: Double,
    val jobHours: Map<String, Double>, // jobId -> hours
    val effectivenessRate: Double,
    val isWorkingDay: Boolean
)

data class WeekTrendData(
    val weekStart: LocalDate,
    val weekNumber: Int,
    val totalHours: Double,
    val averageDailyHours: Double,
    val jobHours: Map<String, Double>, // jobId -> total hours
    val effectivenessRate: Double
)

data class MonthTrendData(
    val month: LocalDate, // first day of month
    val monthName: String,
    val totalHours: Double,
    val averageDailyHours: Double,
    val averageWeeklyHours: Double,
    val jobHours: Map<String, Double>, // jobId -> total hours
    val effectivenessRate: Double
)

data class WorkPatternInsights(
    val peakProductivityHours: List<Int>, // hours of day (0-23)
    val optimalWorkingDuration: Double, // in hours per day
    val burnoutRiskScore: Double, // 0-100
    val workLifeBalanceScore: Double, // 0-100
    val multitaskingEfficiency: Double, // correlation between job switching and effectiveness
    val consistencyScore: Double, // variance in daily working patterns
    val recommendedImprovements: List<String>
)

data class SeasonalPattern(
    val period: String, // "Morning", "Afternoon", "Evening"
    val averageHours: Double,
    val effectivenessRate: Double
)

enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE
}

// Enhanced time period selection
enum class AnalyticsPeriod {
    WEEK,
    SIX_WEEKS,
    SIX_MONTHS
}

// Detailed breakdown for selected periods
data class PeriodDetails(
    val period: AnalyticsPeriod,
    val selectedDate: LocalDate?,
    val selectedWeek: Int?,
    val selectedMonth: String?,
    val totalHours: Double,
    val averageDailyHours: Double,
    val jobBreakdowns: List<JobDetailBreakdown>,
    val effectivenessBreakdown: EffectivenessBreakdown
)

data class JobDetailBreakdown(
    val jobId: String,
    val jobName: String,
    val totalHours: Double,
    val averageDailyHours: Double,
    val percentage: Double,
    val effectivenessRate: Double
)

data class EffectivenessBreakdown(
    val productiveHours: Double,
    val meetingHours: Double,
    val productivePercentage: Double,
    val averageSessionDuration: Double
) 