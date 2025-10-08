package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant

class DefaultTimeRepositoryImpl(
    private val timeBlockDao: TimeBlockDao,
    private val statusUpdateDao: StatusUpdateDao,
    private val jobDao: JobDao
) : TimeRepository {

    override suspend fun upsertTimeBlock(timeBlock: TimeBlock) {
        timeBlockDao.upsert(timeBlock.toEntity())
    }

    override suspend fun deleteTimeBlock(id: Int) {
        timeBlockDao.delete(id)
    }

    override suspend fun getTimeBlockById(id: Int): TimeBlock? {
        return timeBlockDao.getById(id)?.toDomain()
    }

    override fun getAllTimeBlocks(): Flow<List<TimeBlock>> {
        return timeBlockDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTimeBlocksByDate(date: LocalDate): Flow<List<TimeBlock>> {
        return timeBlockDao.getByDate(date.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTimeBlocksByDateIncludingCrossDay(date: LocalDate): Flow<List<TimeBlock>> {
        return timeBlockDao.getByDateIncludingCrossDayBlocks(date.toString()).map { entities ->
            entities.map { it.toDomain() }.map { timeBlock ->
                // If this block crosses into the requested date, adjust it for display
                adjustTimeBlockForDate(timeBlock, date)
            }
        }
    }
    
    // Helper function to adjust time blocks that cross dates for proper display
    private fun adjustTimeBlockForDate(timeBlock: TimeBlock, targetDate: LocalDate): TimeBlock {
        val blockStartDate = timeBlock.startTime.date
        val blockEndDate = timeBlock.endTime?.date
        
        // If block starts on target date, no adjustment needed
        if (blockStartDate == targetDate) {
            return timeBlock
        }
        
        // If block started before target date but extends into it
        if (blockStartDate < targetDate) {
            val dayStart = kotlinx.datetime.LocalDateTime(targetDate, kotlinx.datetime.LocalTime(0, 0, 0))
            
            // If block is still active (no end time), show from start of target date
            if (timeBlock.endTime == null) {
                return timeBlock.copy(
                    startTime = dayStart,
                    duration = 0 // Will be calculated by UI for active blocks
                )
            }
            
            // If block ended on or after target date, show portion from start of target date
            if (blockEndDate != null && blockEndDate >= targetDate) {
                val startInstant = dayStart.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
                val endInstant = timeBlock.endTime!!.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
                val adjustedDuration = (endInstant.epochSeconds - startInstant.epochSeconds) * 1000L
                
                return timeBlock.copy(
                    startTime = dayStart,
                    duration = maxOf(0L, adjustedDuration)
                )
            }
        }
        
        return timeBlock
    }

    override fun getActiveTimeBlock(): Flow<TimeBlock?> {
        return timeBlockDao.getActiveBlock().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getActiveTimeBlockSync(): TimeBlock? {
        return timeBlockDao.getActiveBlock().first()?.toDomain()
    }

    override fun getVisibleJobs(): Flow<List<Job>> {
        return jobDao.getVisibleJobs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllJobs(): Flow<List<Job>> {
        return jobDao.getAllJobs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getJobById(id: String): Job? {
        return jobDao.getJobById(id)?.toDomain()
    }

    override suspend fun upsertJob(job: Job) {
        jobDao.upsertJob(job.toEntity())
    }

    override suspend fun deleteJob(job: Job) {
        jobDao.deleteJob(job.toEntity())
    }

    override suspend fun setMainJob(jobId: String) {
        jobDao.clearMainJob()
        jobDao.setMainJob(jobId)
    }

    // Helper function to filter out time blocks with non-existent jobs
    private suspend fun filterValidTimeBlocks(blocks: List<TimeBlock>): List<TimeBlock> {
        return blocks.filter { block ->
            getJobById(block.jobId) != null
        }
    }

    // Helper function to safely get job or return fallback
    private suspend fun getJobByIdSafe(jobId: String): Job {
        return getJobById(jobId) ?: Job(
            id = jobId,
            name = "Unknown Job",
            color = 0xFF808080L, // Gray color for unknown jobs
            isMain = false,
            hourlyRatePLN = null,
            isHidden = false
        )
    }

    override suspend fun getDailySummary(date: LocalDate): DailySummary {
        val allBlocks = getTimeBlocksByDate(date).first()
        val blocks = filterValidTimeBlocks(allBlocks)
        
        val jobBreakdown = blocks.groupBy { it.jobId }
            .mapValues { (jobId, blocks) ->
                val job = getJobByIdSafe(jobId)
                val totalHours = blocks.sumOf { it.getDurationInHours() }
                JobSummary(
                    jobId = jobId,
                    jobName = job.name,
                    totalHours = totalHours,
                    percentage = 0.0 // Will be calculated below
                )
            }
        
        val totalHours = blocks.sumOf { it.getDurationInHours() }
        
        // Calculate percentages
        val updatedJobBreakdown = jobBreakdown.mapValues { (_, summary) ->
            summary.copy(
                percentage = if (totalHours > 0) ((summary.totalHours / totalHours) * 100).toInt().toDouble() else 0.0
            )
        }
        
        return DailySummary(
            date = date,
            blocks = blocks,
            totalHours = totalHours,
            jobBreakdown = updatedJobBreakdown
        )
    }

    override suspend fun getWeeklyAnalytics(weekStart: LocalDate): WeeklyAnalytics {
        // Treat weekStart as the END date (selected date or today)
        val endDate = weekStart
        val startDate = endDate.minus(kotlinx.datetime.DatePeriod(days = 6)) // last 7 days
        val allBlocks = timeBlockDao.getByDateRange(startDate.toString(), endDate.toString()).first()
            .map { it.toDomain() }
        val blocks = filterValidTimeBlocks(allBlocks)

        val dailySummaries = mutableListOf<DailySummary>()
        for (i in 0 until 7) {
            val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
            val dailyBlocks = blocks.filter {
                it.startTime.date == date
            }

            val jobBreakdown = dailyBlocks.groupBy { it.jobId }
                .mapValues { (jobId, blocks) ->
                    val job = getJobByIdSafe(jobId)
                    val totalHours = blocks.sumOf { it.getDurationInHours() }
                    JobSummary(
                        jobId = jobId,
                        jobName = job.name,
                        totalHours = totalHours,
                        percentage = 0.0
                    )
                }

            val totalHours = dailyBlocks.sumOf { it.getDurationInHours() }

            // Calculate percentages
            val updatedJobBreakdown = jobBreakdown.mapValues { (_, summary) ->
                summary.copy(
                    percentage = if (totalHours > 0) ((summary.totalHours / totalHours) * 100).toInt().toDouble() else 0.0
                )
            }

            dailySummaries.add(DailySummary(
                date = date,
                blocks = dailyBlocks,
                totalHours = totalHours,
                jobBreakdown = updatedJobBreakdown
            ))
        }

        val totalHours = blocks.sumOf { it.getDurationInHours() }
        val averageDailyHours = totalHours / 7

        val jobBreakdown = blocks.groupBy { it.jobId }
            .mapValues { (jobId, blocks) ->
                val job = getJobByIdSafe(jobId)
                val jobTotalHours = blocks.sumOf { it.getDurationInHours() }
                val jobAverageDailyHours = jobTotalHours / 7
                JobAnalytics(
                    jobId = jobId,
                    jobName = job.name,
                    totalHours = jobTotalHours,
                    averageDailyHours = jobAverageDailyHours,
                    percentage = if (totalHours > 0) ((jobTotalHours / totalHours) * 100).toInt().toDouble() else 0.0
                )
            }

        return WeeklyAnalytics(
            weekStart = startDate,
            weekEnd = endDate,
            dailySummaries = dailySummaries,
            totalHours = totalHours,
            averageDailyHours = averageDailyHours,
            jobBreakdown = jobBreakdown
        )
    }

    override suspend fun getLast7DaysAnalytics(endDate: LocalDate): WeeklyAnalytics {
        val startDate = endDate.minus(kotlinx.datetime.DatePeriod(days = 6)) // last 7 days
        val allBlocks = timeBlockDao.getByDateRange(startDate.toString(), endDate.toString()).first()
            .map { it.toDomain() }
        val blocks = filterValidTimeBlocks(allBlocks)

        val dailySummaries = mutableListOf<DailySummary>()
        for (i in 0 until 7) {
            val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
            val dailyBlocks = blocks.filter {
                it.startTime.date == date
            }

            val jobBreakdown = dailyBlocks.groupBy { it.jobId }
                .mapValues { (jobId, blocks) ->
                    val job = getJobByIdSafe(jobId)
                    val totalHours = blocks.sumOf { it.getDurationInHours() }
                    JobSummary(
                        jobId = jobId,
                        jobName = job.name,
                        totalHours = totalHours,
                        percentage = 0.0
                    )
                }

            val totalHours = dailyBlocks.sumOf { it.getDurationInHours() }

            // Calculate percentages
            val updatedJobBreakdown = jobBreakdown.mapValues { (_, summary) ->
                summary.copy(
                    percentage = if (totalHours > 0) ((summary.totalHours / totalHours) * 100).toInt().toDouble() else 0.0
                )
            }

            dailySummaries.add(DailySummary(
                date = date,
                blocks = dailyBlocks,
                totalHours = totalHours,
                jobBreakdown = updatedJobBreakdown
            ))
        }

        // Filter working days (Mon-Fri) for pie chart calculations
        val workingDayBlocks = blocks.filter { block ->
            val dayOfWeek = block.startTime.date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5 // Monday(1) to Friday(5)
        }
        
        // Count working days in the period
        val workingDaysCount = (0 until 7).count { i ->
            val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
            val dayOfWeek = date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5
        }

        val totalHours = blocks.sumOf { it.getDurationInHours() }
        val workingDaysTotalHours = workingDayBlocks.sumOf { it.getDurationInHours() }
        val averageDailyHours = if (workingDaysCount > 0) workingDaysTotalHours / workingDaysCount else 0.0

        // Job breakdown for pie chart (based on working days only for average calculation)
        val jobBreakdown = workingDayBlocks.groupBy { it.jobId }
            .mapValues { (jobId, blocks) ->
                val job = getJobByIdSafe(jobId)
                val jobTotalHours = blocks.sumOf { it.getDurationInHours() }
                val jobAverageDailyHours = if (workingDaysCount > 0) jobTotalHours / workingDaysCount else 0.0
                JobAnalytics(
                    jobId = jobId,
                    jobName = job.name,
                    totalHours = jobTotalHours,
                    averageDailyHours = jobAverageDailyHours,
                    percentage = if (workingDaysTotalHours > 0) ((jobTotalHours / workingDaysTotalHours) * 100).toInt().toDouble() else 0.0
                )
            }

        return WeeklyAnalytics(
            weekStart = startDate,
            weekEnd = endDate,
            dailySummaries = dailySummaries, // All 7 days for bar chart
            totalHours = workingDaysTotalHours, // Working days total for pie chart
            averageDailyHours = averageDailyHours, // Working days average for pie chart
            jobBreakdown = jobBreakdown
        )
    }
    
    // Status Updates
    override suspend fun upsertStatusUpdate(statusUpdate: StatusUpdate) {
        statusUpdateDao.upsert(statusUpdate.toEntity())
    }

    override fun getStatusUpdatesByDate(date: LocalDate): Flow<List<StatusUpdate>> {
        return statusUpdateDao.getByDate(date.toString()).map { entities ->
            entities.mapNotNull { entity ->
                val job = getJobById(entity.jobId) ?: Job(
                    id = entity.jobId,
                    name = "Unknown Job",
                    color = 0xFF808080 // Gray color for unknown jobs
                )
                entity.toDomain(job.name)
            }
        }
    }

    override suspend fun getStatusUpdateByJobAndDate(jobId: String, date: LocalDate): StatusUpdate? {
        val entity = statusUpdateDao.getByJobAndDate(jobId, date.toString())
        return entity?.let { 
            val job = getJobByIdSafe(it.jobId)
            it.toDomain(job.name)
        }
    }

    override suspend fun deleteStatusUpdate(id: String) {
        statusUpdateDao.delete(id)
    }
    
    // Advanced Analytics Implementation
    override suspend fun getWeekAnalytics(endDate: LocalDate): WeekAnalytics {
        val startDate = endDate.minus(kotlinx.datetime.DatePeriod(days = 6))
        val allBlocks = timeBlockDao.getByDateRange(startDate.toString(), endDate.toString()).first()
            .map { it.toDomain() }
        val blocks = filterValidTimeBlocks(allBlocks)
        
        // Filter only working days (Monday-Friday)
        val workingDayBlocks = blocks.filter { block ->
            val dayOfWeek = block.startTime.date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5 // Monday(1) to Friday(5)
        }
        
        val workingDaysInPeriod = (0 until 7).count { i ->
            val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
            val dayOfWeek = date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5
        }
        
        val totalWorkingDayHours = workingDayBlocks.sumOf { it.getDurationInHours() }
        val averageDailyHours = if (workingDaysInPeriod > 0) totalWorkingDayHours / workingDaysInPeriod else 0.0
        
        // Calculate effectiveness rate
        val effectiveBlocks = workingDayBlocks.filter { it.effectiveness == Effectiveness.Productive }
        val effectivenessRate = if (workingDayBlocks.isNotEmpty()) {
            (effectiveBlocks.sumOf { it.getDurationInHours() } / totalWorkingDayHours) * 100
        } else 0.0
        
        // Job breakdown with peak hours analysis
        val jobBreakdown = workingDayBlocks.groupBy { it.jobId }
            .mapValues { (jobId, jobBlocks) ->
                val job = getJobByIdSafe(jobId)
                val jobTotalHours = jobBlocks.sumOf { it.getDurationInHours() }
                val jobAverageDailyHours = if (workingDaysInPeriod > 0) jobTotalHours / workingDaysInPeriod else 0.0
                val jobEffectiveBlocks = jobBlocks.filter { it.effectiveness == Effectiveness.Productive }
                val jobEffectivenessRate = if (jobBlocks.isNotEmpty()) {
                    (jobEffectiveBlocks.sumOf { it.getDurationInHours() } / jobTotalHours) * 100
                } else 0.0
                
                // Calculate peak working hours
                val peakHours = jobBlocks
                    .groupBy { it.startTime.hour }
                    .mapValues { (_, blocks) -> blocks.sumOf { it.getDurationInHours() } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)
                    .map { it.first }
                
                JobWeeklyAnalytics(
                    jobId = jobId,
                    jobName = job.name,
                    totalHours = jobTotalHours,
                    averageDailyHours = jobAverageDailyHours,
                    percentage = if (totalWorkingDayHours > 0) (jobTotalHours / totalWorkingDayHours) * 100 else 0.0,
                    effectivenessRate = jobEffectivenessRate,
                    peakWorkingHours = peakHours
                )
            }
        
        // Daily trends
        val dailyTrends = (0 until 7).map { i ->
            val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
            val dayBlocks = blocks.filter { it.startTime.date == date }
            val dayName = when (date.dayOfWeek.isoDayNumber) {
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                6 -> "Sat"
                7 -> "Sun"
                else -> "Unknown"
            }
            val isWorkingDay = date.dayOfWeek.isoDayNumber in 1..5
            
            val dayTotalHours = dayBlocks.sumOf { it.getDurationInHours() }
            val dayJobHours = dayBlocks.groupBy { it.jobId }
                .mapValues { (_, blocks) -> blocks.sumOf { it.getDurationInHours() } }
            
            val dayEffectiveBlocks = dayBlocks.filter { it.effectiveness == Effectiveness.Productive }
            val dayEffectivenessRate = if (dayBlocks.isNotEmpty()) {
                (dayEffectiveBlocks.sumOf { it.getDurationInHours() } / dayTotalHours) * 100
            } else 0.0
            
            DayTrendData(
                date = date,
                dayName = dayName,
                totalHours = dayTotalHours,
                jobHours = dayJobHours,
                effectivenessRate = dayEffectivenessRate,
                isWorkingDay = isWorkingDay
            )
        }
        
        return WeekAnalytics(
            weekStart = startDate,
            weekEnd = endDate,
            totalWorkingDayHours = totalWorkingDayHours,
            averageDailyHours = averageDailyHours,
            jobBreakdown = jobBreakdown,
            dailyTrends = dailyTrends,
            effectivenessRate = effectivenessRate
        )
    }
    
    override suspend fun getSixWeekAnalytics(endDate: LocalDate): SixWeekAnalytics {
        val startDate = endDate.minus(kotlinx.datetime.DatePeriod(days = 41)) // 6 weeks = 42 days
        val allBlocks = timeBlockDao.getByDateRange(startDate.toString(), endDate.toString()).first()
            .map { it.toDomain() }
        val blocks = filterValidTimeBlocks(allBlocks)
        
        // Filter only working days
        val workingDayBlocks = blocks.filter { block ->
            val dayOfWeek = block.startTime.date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5
        }
        
        val workingDaysInPeriod = (0 until 42).count { i ->
            val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
            val dayOfWeek = date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5
        }
        
        val totalWorkingDayHours = workingDayBlocks.sumOf { it.getDurationInHours() }
        val averageDailyHours = if (workingDaysInPeriod > 0) totalWorkingDayHours / workingDaysInPeriod else 0.0
        val averageWeeklyHours = if (workingDaysInPeriod > 0) totalWorkingDayHours / 6.0 else 0.0
        
        val effectiveBlocks = workingDayBlocks.filter { it.effectiveness == Effectiveness.Productive }
        val effectivenessRate = if (workingDayBlocks.isNotEmpty()) {
            (effectiveBlocks.sumOf { it.getDurationInHours() } / totalWorkingDayHours) * 100
        } else 0.0
        
        // Job breakdown with consistency scores
        val jobBreakdown = workingDayBlocks.groupBy { it.jobId }
            .mapValues { (jobId, jobBlocks) ->
                val job = getJobByIdSafe(jobId)
                val jobTotalHours = jobBlocks.sumOf { it.getDurationInHours() }
                val jobAverageDailyHours = if (workingDaysInPeriod > 0) jobTotalHours / workingDaysInPeriod else 0.0
                val jobAverageWeeklyHours = if (workingDaysInPeriod > 0) jobTotalHours / 6.0 else 0.0
                
                val jobEffectiveBlocks = jobBlocks.filter { it.effectiveness == Effectiveness.Productive }
                val jobEffectivenessRate = if (jobBlocks.isNotEmpty()) {
                    (jobEffectiveBlocks.sumOf { it.getDurationInHours() } / jobTotalHours) * 100
                } else 0.0
                
                // Calculate consistency score (inverse of variance)
                val dailyHours = (0 until 42).map { i ->
                    val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
                    val dayOfWeek = date.dayOfWeek.isoDayNumber
                    if (dayOfWeek in 1..5) {
                        jobBlocks.filter { it.startTime.date == date }.sumOf { it.getDurationInHours() }
                    } else 0.0
                }.filter { it > 0.0 }
                
                val mean = if (dailyHours.isNotEmpty()) dailyHours.average() else 0.0
                val variance = if (dailyHours.size > 1) {
                    dailyHours.map { (it - mean) * (it - mean) }.average()
                } else 0.0
                val consistencyScore = if (variance > 0) maxOf(0.0, 100.0 - (variance * 10)) else 100.0
                
                // Trend direction (simple slope calculation)
                val firstWeekHours = jobBlocks.filter { 
                    it.startTime.date >= startDate && it.startTime.date < startDate.plus(kotlinx.datetime.DatePeriod(days = 7))
                }.sumOf { it.getDurationInHours() }
                val lastWeekHours = jobBlocks.filter {
                    it.startTime.date >= endDate.minus(kotlinx.datetime.DatePeriod(days = 6)) && it.startTime.date <= endDate
                }.sumOf { it.getDurationInHours() }
                
                val trendDirection = when {
                    lastWeekHours > firstWeekHours * 1.1 -> TrendDirection.INCREASING
                    lastWeekHours < firstWeekHours * 0.9 -> TrendDirection.DECREASING
                    else -> TrendDirection.STABLE
                }
                
                JobSixWeekAnalytics(
                    jobId = jobId,
                    jobName = job.name,
                    totalHours = jobTotalHours,
                    averageDailyHours = jobAverageDailyHours,
                    averageWeeklyHours = jobAverageWeeklyHours,
                    percentage = if (totalWorkingDayHours > 0) (jobTotalHours / totalWorkingDayHours) * 100 else 0.0,
                    effectivenessRate = jobEffectivenessRate,
                    consistencyScore = consistencyScore,
                    trendDirection = trendDirection
                )
            }
        
        // Weekly trends
        val weeklyTrends = (0 until 6).map { weekIndex ->
            val weekStart = startDate.plus(kotlinx.datetime.DatePeriod(days = weekIndex * 7))
            val weekEnd = weekStart.plus(kotlinx.datetime.DatePeriod(days = 6))
            val weekBlocks = workingDayBlocks.filter { 
                it.startTime.date >= weekStart && it.startTime.date <= weekEnd
            }
            
            val weekWorkingDays = (0 until 7).count { day ->
                val date = weekStart.plus(kotlinx.datetime.DatePeriod(days = day))
                date.dayOfWeek.isoDayNumber in 1..5
            }
            
            val weekTotalHours = weekBlocks.sumOf { it.getDurationInHours() }
            val weekAverageDailyHours = if (weekWorkingDays > 0) weekTotalHours / weekWorkingDays else 0.0
            val weekJobHours = weekBlocks.groupBy { it.jobId }
                .mapValues { (_, blocks) -> blocks.sumOf { it.getDurationInHours() } }
            
            val weekEffectiveBlocks = weekBlocks.filter { it.effectiveness == Effectiveness.Productive }
            val weekEffectivenessRate = if (weekBlocks.isNotEmpty()) {
                (weekEffectiveBlocks.sumOf { it.getDurationInHours() } / weekTotalHours) * 100
            } else 0.0
            
            WeekTrendData(
                weekStart = weekStart,
                weekNumber = weekIndex + 1,
                totalHours = weekTotalHours,
                averageDailyHours = weekAverageDailyHours,
                jobHours = weekJobHours,
                effectivenessRate = weekEffectivenessRate
            )
        }
        
        return SixWeekAnalytics(
            startDate = startDate,
            endDate = endDate,
            totalWorkingDayHours = totalWorkingDayHours,
            averageDailyHours = averageDailyHours,
            averageWeeklyHours = averageWeeklyHours,
            jobBreakdown = jobBreakdown,
            weeklyTrends = weeklyTrends,
            effectivenessRate = effectivenessRate
        )
    }
    
    override suspend fun getSixMonthAnalytics(endDate: LocalDate): SixMonthAnalytics {
        val startDate = endDate.minus(kotlinx.datetime.DatePeriod(months = 6))
        val allBlocks = timeBlockDao.getByDateRange(startDate.toString(), endDate.toString()).first()
            .map { it.toDomain() }
        val blocks = filterValidTimeBlocks(allBlocks)
        
        // Filter only working days
        val workingDayBlocks = blocks.filter { block ->
            val dayOfWeek = block.startTime.date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5
        }
        
        val totalDays = (endDate.toEpochDays() - startDate.toEpochDays()).toInt()
        val workingDaysInPeriod = (0 until totalDays).count { i ->
            val date = startDate.plus(kotlinx.datetime.DatePeriod(days = i))
            val dayOfWeek = date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5
        }
        
        val totalWorkingDayHours = workingDayBlocks.sumOf { it.getDurationInHours() }
        val averageDailyHours = if (workingDaysInPeriod > 0) totalWorkingDayHours / workingDaysInPeriod else 0.0
        val averageWeeklyHours = averageDailyHours * 5.0 // 5 working days per week
        val averageMonthlyHours = averageWeeklyHours * 4.33 // ~4.33 weeks per month
        
        val effectiveBlocks = workingDayBlocks.filter { it.effectiveness == Effectiveness.Productive }
        val effectivenessRate = if (workingDayBlocks.isNotEmpty()) {
            (effectiveBlocks.sumOf { it.getDurationInHours() } / totalWorkingDayHours) * 100
        } else 0.0
        
        // Job breakdown with seasonal patterns
        val jobBreakdown = workingDayBlocks.groupBy { it.jobId }
            .mapValues { (jobId, jobBlocks) ->
                val job = getJobByIdSafe(jobId)
                val jobTotalHours = jobBlocks.sumOf { it.getDurationInHours() }
                val jobAverageDailyHours = if (workingDaysInPeriod > 0) jobTotalHours / workingDaysInPeriod else 0.0
                val jobAverageWeeklyHours = jobAverageDailyHours * 5.0
                val jobAverageMonthlyHours = jobAverageWeeklyHours * 4.33
                
                val jobEffectiveBlocks = jobBlocks.filter { it.effectiveness == Effectiveness.Productive }
                val jobEffectivenessRate = if (jobBlocks.isNotEmpty()) {
                    (jobEffectiveBlocks.sumOf { it.getDurationInHours() } / jobTotalHours) * 100
                } else 0.0
                
                // Seasonal patterns (Morning, Afternoon, Evening)
                val seasonalPatterns = listOf(
                    SeasonalPattern(
                        period = "Morning",
                        averageHours = jobBlocks.filter { it.startTime.hour < 12 }.sumOf { it.getDurationInHours() } / 6.0,
                        effectivenessRate = jobBlocks.filter { it.startTime.hour < 12 && it.effectiveness == Effectiveness.Productive }
                            .sumOf { it.getDurationInHours() } / maxOf(1.0, jobBlocks.filter { it.startTime.hour < 12 }
                            .sumOf { it.getDurationInHours() }) * 100
                    ),
                    SeasonalPattern(
                        period = "Afternoon",
                        averageHours = jobBlocks.filter { it.startTime.hour in 12..17 }.sumOf { it.getDurationInHours() } / 6.0,
                        effectivenessRate = jobBlocks.filter { it.startTime.hour in 12..17 && it.effectiveness == Effectiveness.Productive }
                            .sumOf { it.getDurationInHours() } / maxOf(1.0, jobBlocks.filter { it.startTime.hour in 12..17 }
                            .sumOf { it.getDurationInHours() }) * 100
                    ),
                    SeasonalPattern(
                        period = "Evening",
                        averageHours = jobBlocks.filter { it.startTime.hour > 17 }.sumOf { it.getDurationInHours() } / 6.0,
                        effectivenessRate = jobBlocks.filter { it.startTime.hour > 17 && it.effectiveness == Effectiveness.Productive }
                            .sumOf { it.getDurationInHours() } / maxOf(1.0, jobBlocks.filter { it.startTime.hour > 17 }
                            .sumOf { it.getDurationInHours() }) * 100
                    )
                )
                
                // Calculate consistency and trend
                val monthlyHours = (0 until 6).map { monthIndex ->
                    val monthStart = startDate.plus(kotlinx.datetime.DatePeriod(months = monthIndex))
                    val monthEnd = monthStart.plus(kotlinx.datetime.DatePeriod(months = 1)).minus(kotlinx.datetime.DatePeriod(days = 1))
                    jobBlocks.filter { it.startTime.date >= monthStart && it.startTime.date <= monthEnd }
                        .sumOf { it.getDurationInHours() }
                }
                
                val mean = if (monthlyHours.isNotEmpty()) monthlyHours.average() else 0.0
                val variance = if (monthlyHours.size > 1) {
                    monthlyHours.map { (it - mean) * (it - mean) }.average()
                } else 0.0
                val consistencyScore = if (variance > 0) maxOf(0.0, 100.0 - (variance / 10)) else 100.0
                
                val firstMonthHours = monthlyHours.firstOrNull() ?: 0.0
                val lastMonthHours = monthlyHours.lastOrNull() ?: 0.0
                val trendDirection = when {
                    lastMonthHours > firstMonthHours * 1.2 -> TrendDirection.INCREASING
                    lastMonthHours < firstMonthHours * 0.8 -> TrendDirection.DECREASING
                    else -> TrendDirection.STABLE
                }
                
                JobSixMonthAnalytics(
                    jobId = jobId,
                    jobName = job.name,
                    totalHours = jobTotalHours,
                    averageDailyHours = jobAverageDailyHours,
                    averageWeeklyHours = jobAverageWeeklyHours,
                    averageMonthlyHours = jobAverageMonthlyHours,
                    percentage = if (totalWorkingDayHours > 0) (jobTotalHours / totalWorkingDayHours) * 100 else 0.0,
                    effectivenessRate = jobEffectivenessRate,
                    consistencyScore = consistencyScore,
                    trendDirection = trendDirection,
                    seasonalPatterns = seasonalPatterns
                )
            }
        
        // Monthly trends
        val monthlyTrends = (0 until 6).map { monthIndex ->
            val monthStart = startDate.plus(kotlinx.datetime.DatePeriod(months = monthIndex))
            val monthEnd = monthStart.plus(kotlinx.datetime.DatePeriod(months = 1)).minus(kotlinx.datetime.DatePeriod(days = 1))
            val monthBlocks = workingDayBlocks.filter { it.startTime.date >= monthStart && it.startTime.date <= monthEnd }
            
            val monthWorkingDays = (0 until (monthEnd.plus(kotlinx.datetime.DatePeriod(days = 1)).toEpochDays() - monthStart.toEpochDays()).toInt()).count { day ->
                val date = monthStart.plus(kotlinx.datetime.DatePeriod(days = day))
                date.dayOfWeek.isoDayNumber in 1..5
            }
            
            val monthTotalHours = monthBlocks.sumOf { it.getDurationInHours() }
            val monthAverageDailyHours = if (monthWorkingDays > 0) monthTotalHours / monthWorkingDays else 0.0
            val monthAverageWeeklyHours = monthAverageDailyHours * 5.0
            val monthJobHours = monthBlocks.groupBy { it.jobId }
                .mapValues { (_, blocks) -> blocks.sumOf { it.getDurationInHours() } }
                
            val monthEffectiveBlocks = monthBlocks.filter { it.effectiveness == Effectiveness.Productive }
            val monthEffectivenessRate = if (monthBlocks.isNotEmpty()) {
                (monthEffectiveBlocks.sumOf { it.getDurationInHours() } / monthTotalHours) * 100
            } else 0.0
            
            val monthName = when (monthStart.monthNumber) {
                1 -> "Jan"
                2 -> "Feb"
                3 -> "Mar"
                4 -> "Apr"
                5 -> "May"
                6 -> "Jun"
                7 -> "Jul"
                8 -> "Aug"
                9 -> "Sep"
                10 -> "Oct"
                11 -> "Nov"
                12 -> "Dec"
                else -> "Unknown"
            }
            
            MonthTrendData(
                month = monthStart,
                monthName = monthName,
                totalHours = monthTotalHours,
                averageDailyHours = monthAverageDailyHours,
                averageWeeklyHours = monthAverageWeeklyHours,
                jobHours = monthJobHours,
                effectivenessRate = monthEffectivenessRate
            )
        }
        
        return SixMonthAnalytics(
            startDate = startDate,
            endDate = endDate,
            totalWorkingDayHours = totalWorkingDayHours,
            averageDailyHours = averageDailyHours,
            averageWeeklyHours = averageWeeklyHours,
            averageMonthlyHours = averageMonthlyHours,
            jobBreakdown = jobBreakdown,
            monthlyTrends = monthlyTrends,
            effectivenessRate = effectivenessRate
        )
    }
    
    override suspend fun getWorkPatternInsights(endDate: LocalDate): WorkPatternInsights {
        val startDate = endDate.minus(kotlinx.datetime.DatePeriod(months = 3)) // 3 months of data for insights
        val allBlocks = timeBlockDao.getByDateRange(startDate.toString(), endDate.toString()).first()
            .map { it.toDomain() }
        val blocks = filterValidTimeBlocks(allBlocks)
        
        // Filter only working days
        val workingDayBlocks = blocks.filter { block ->
            val dayOfWeek = block.startTime.date.dayOfWeek.isoDayNumber
            dayOfWeek in 1..5
        }
        
        if (workingDayBlocks.isEmpty()) {
            return WorkPatternInsights(
                peakProductivityHours = emptyList(),
                optimalWorkingDuration = 8.0,
                burnoutRiskScore = 0.0,
                workLifeBalanceScore = 100.0,
                multitaskingEfficiency = 100.0,
                consistencyScore = 100.0,
                recommendedImprovements = emptyList()
            )
        }
        
        // Peak productivity hours analysis
        val hourlyProductivity = workingDayBlocks
            .filter { it.effectiveness == Effectiveness.Productive }
            .groupBy { it.startTime.hour }
            .mapValues { (_, blocks) -> blocks.sumOf { it.getDurationInHours() } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        // Optimal working duration
        val dailyHours = workingDayBlocks.groupBy { it.startTime.date }
            .mapValues { (_, blocks) -> blocks.sumOf { it.getDurationInHours() } }
            .values
            .filter { it > 0 }
        
        val optimalDuration = if (dailyHours.isNotEmpty()) dailyHours.average() else 8.0
        
        // Burnout risk score (based on consistency and overwork)
        val averageDailyHours = dailyHours.average()
        val burnoutRisk = when {
            averageDailyHours > 12 -> 90.0
            averageDailyHours > 10 -> 70.0
            averageDailyHours > 9 -> 50.0
            averageDailyHours > 8 -> 30.0
            else -> 10.0
        }
        
        // Work-life balance score
        val weekendWork = blocks.filter { block ->
            val dayOfWeek = block.startTime.date.dayOfWeek.isoDayNumber
            dayOfWeek in 6..7 // Saturday and Sunday
        }.sumOf { it.getDurationInHours() }
        
        val totalHours = blocks.sumOf { it.getDurationInHours() }
        val workLifeBalance = maxOf(0.0, 100.0 - (weekendWork / totalHours * 200))
        
        // Multitasking efficiency (job switching correlation with effectiveness)
        val dailyJobSwitches = workingDayBlocks.groupBy { it.startTime.date }
            .mapValues { (_, dayBlocks) ->
                val sortedBlocks = dayBlocks.sortedBy { it.startTime }
                var switches = 0
                for (i in 1 until sortedBlocks.size) {
                    if (sortedBlocks[i].jobId != sortedBlocks[i-1].jobId) {
                        switches++
                    }
                }
                switches to dayBlocks.filter { it.effectiveness == Effectiveness.Productive }.sumOf { it.getDurationInHours() }
            }
        
        val multitaskingEfficiency = if (dailyJobSwitches.isNotEmpty()) {
            val avgSwitches = dailyJobSwitches.values.map { it.first }.average()
            val avgProductiveHours = dailyJobSwitches.values.map { it.second }.average()
            maxOf(0.0, 100.0 - (avgSwitches * 10)) // More switches = lower efficiency
        } else 100.0
        
        // Consistency score
        val mean = dailyHours.average()
        val variance = if (dailyHours.size > 1) {
            dailyHours.map { (it - mean) * (it - mean) }.average()
        } else 0.0
        val consistencyScore = if (variance > 0) maxOf(0.0, 100.0 - (variance * 5)) else 100.0
        
        // Recommended improvements
        val recommendations = mutableListOf<String>()
        if (burnoutRisk > 50) recommendations.add("Consider reducing daily working hours to prevent burnout")
        if (workLifeBalance < 70) recommendations.add("Improve work-life balance by limiting weekend work")
        if (multitaskingEfficiency < 70) recommendations.add("Reduce job switching to improve focus and productivity")
        if (consistencyScore < 60) recommendations.add("Try to maintain more consistent daily working patterns")
        if (hourlyProductivity.isNotEmpty()) {
            recommendations.add("Focus important tasks during peak hours: ${hourlyProductivity.joinToString(", ") { "${it}:00" }}")
        }
        
        return WorkPatternInsights(
            peakProductivityHours = hourlyProductivity,
            optimalWorkingDuration = optimalDuration,
            burnoutRiskScore = burnoutRisk,
            workLifeBalanceScore = workLifeBalance,
            multitaskingEfficiency = multitaskingEfficiency,
            consistencyScore = consistencyScore,
            recommendedImprovements = recommendations
        )
    }
} 