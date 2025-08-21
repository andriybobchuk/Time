package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.DailySummary
import com.andriybobchuk.time.time.domain.Job
import com.andriybobchuk.time.time.domain.JobAnalytics
import com.andriybobchuk.time.time.domain.JobSummary
import com.andriybobchuk.time.time.domain.StatusUpdate
import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.TimeRepository
import com.andriybobchuk.time.time.domain.WeeklyAnalytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

class DefaultTimeRepositoryImpl(
    private val timeBlockDao: TimeBlockDao,
    private val statusUpdateDao: StatusUpdateDao
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

    override fun getActiveTimeBlock(): Flow<TimeBlock?> {
        return timeBlockDao.getActiveBlock().map { entity ->
            entity?.toDomain()
        }
    }

    override fun getJobs(): List<Job> = TimeDataSource.jobs

    override fun getJobById(id: String): Job? {
        return TimeDataSource.jobs.find { it.id == id }
    }

    // Helper function to filter out time blocks with non-existent jobs
    private fun filterValidTimeBlocks(blocks: List<TimeBlock>): List<TimeBlock> {
        return blocks.filter { block ->
            getJobById(block.jobId) != null
        }
    }

    // Helper function to safely get job or return fallback
    private fun getJobByIdSafe(jobId: String): Job {
        return getJobById(jobId) ?: Job(
            id = jobId,
            name = "Unknown Job",
            color = 0xFF808080 // Gray color for unknown jobs
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
            entities.map { entity ->
                val job = getJobByIdSafe(entity.jobId)
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
} 