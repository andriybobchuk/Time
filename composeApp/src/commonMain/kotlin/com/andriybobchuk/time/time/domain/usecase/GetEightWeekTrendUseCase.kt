package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.*
import kotlin.math.ceil

class GetEightWeekTrendUseCase(
    private val repository: TimeRepository
) {
    operator fun invoke(): Flow<EightWeekTrend> = flow {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val jobs = repository.getVisibleJobs().first()
        
        // Calculate 8 weeks starting from the most recent Monday
        val currentWeekMonday = today.minus(DatePeriod(days = (today.dayOfWeek.isoDayNumber - 1)))
        val weeks = mutableListOf<WeekData>()
        
        for (weekOffset in 7 downTo 0) {
            val weekStart = currentWeekMonday.minus(DatePeriod(days = weekOffset * 7))
            val weekEnd = weekStart.plus(DatePeriod(days = 6))
            
            // Calculate the actual week number (1-52) for this week
            val weekNumber = getWeekNumber(weekStart)
            
            // Get all time blocks for this week
            val weekBlocks = mutableListOf<TimeBlock>()
            var currentDate = weekStart
            while (currentDate <= weekEnd) {
                val dayBlocks = repository.getTimeBlocksByDate(currentDate).first()
                weekBlocks.addAll(dayBlocks)
                currentDate = currentDate.plus(DatePeriod(days = 1))
            }
            
            // Calculate hours per job
            val jobHours = mutableMapOf<String, Double>()
            var totalHours = 0.0
            
            for (job in jobs) {
                val jobBlocks = weekBlocks.filter { it.jobId == job.id }
                val hours = jobBlocks.sumOf { it.getDurationInHours() }
                if (hours > 0) {
                    jobHours[job.id] = hours
                    totalHours += hours
                }
            }
            
            weeks.add(
                WeekData(
                    weekNumber = weekNumber,
                    startDate = weekStart,
                    endDate = weekEnd,
                    jobHours = jobHours,
                    totalHours = totalHours,
                    isSelected = weekOffset == 0 // Select current week by default
                )
            )
        }
        
        emit(EightWeekTrend(weeks = weeks, jobs = jobs))
    }
    
    private fun getWeekNumber(date: LocalDate): Int {
        val startOfYear = LocalDate(date.year, 1, 1)
        val dayOfYear = date.dayOfYear
        val startOfYearDayOfWeek = startOfYear.dayOfWeek.isoDayNumber
        
        // Calculate week number (ISO 8601 standard)
        val weekNumber = ceil((dayOfYear - startOfYearDayOfWeek + 7) / 7.0).toInt()
        return weekNumber.coerceIn(1, 52)
    }
    
    private fun getWeekdaysInWeek(startDate: LocalDate, endDate: LocalDate): Int {
        var count = 0
        var currentDate = startDate
        
        while (currentDate <= endDate) {
            if (currentDate.dayOfWeek != DayOfWeek.SATURDAY && currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
                count++
            }
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        return count
    }
}

class GetWeekDetailsUseCase(
    private val repository: TimeRepository,
    private val calculateContextSwitchTimeUseCase: CalculateContextSwitchTimeUseCase
) {
    suspend operator fun invoke(weekData: WeekData): WeekDetails {
        val jobs = repository.getVisibleJobs().first()
        
        // Get all time blocks for this week
        val weekBlocks = mutableListOf<TimeBlock>()
        var currentDate = weekData.startDate
        while (currentDate <= weekData.endDate) {
            val dayBlocks = repository.getTimeBlocksByDate(currentDate).first()
            weekBlocks.addAll(dayBlocks)
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        // Calculate effective hours and effectiveness per job
        val effectiveHours = mutableMapOf<String, Double>()
        val effectivenessRate = mutableMapOf<String, Double>()
        val averageDailyHours = mutableMapOf<String, Double>()
        
        for (job in jobs) {
            val jobBlocks = weekBlocks.filter { it.jobId == job.id }
            
            // Calculate effective hours (productive blocks only)
            val productiveHours = jobBlocks
                .filter { it.effectiveness == Effectiveness.Productive }
                .sumOf { it.getDurationInHours() }
            
            val totalJobHours = jobBlocks.sumOf { it.getDurationInHours() }
            
            effectiveHours[job.id] = productiveHours
            effectivenessRate[job.id] = if (totalJobHours > 0) (productiveHours / totalJobHours) * 100 else 0.0
            
            // Calculate average only for weekdays (Monday-Friday)
            val weekdays = getWeekdaysInWeek(weekData.startDate, weekData.endDate)
            averageDailyHours[job.id] = if (weekdays > 0) totalJobHours / weekdays else 0.0
        }
        
        // Calculate context switch time
        val contextAnalysis = calculateContextSwitchTimeUseCase(weekData.startDate, weekData.endDate)
        
        return WeekDetails(
            weekData = weekData,
            effectiveHours = effectiveHours,
            contextSwitchHours = contextAnalysis.totalContextSwitchTime,
            totalTrackedTime = contextAnalysis.totalTrackedTime,
            averageDailyHours = averageDailyHours,
            effectivenessRate = effectivenessRate
        )
    }
    
    private fun getWeekdaysInWeek(startDate: LocalDate, endDate: LocalDate): Int {
        var count = 0
        var currentDate = startDate
        
        while (currentDate <= endDate) {
            if (currentDate.dayOfWeek != DayOfWeek.SATURDAY && currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
                count++
            }
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        return count
    }
}