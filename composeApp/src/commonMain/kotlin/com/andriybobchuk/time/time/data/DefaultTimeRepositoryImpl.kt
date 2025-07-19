package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.DailySummary
import com.andriybobchuk.time.time.domain.Job
import com.andriybobchuk.time.time.domain.JobAnalytics
import com.andriybobchuk.time.time.domain.JobSummary
import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.TimeRepository
import com.andriybobchuk.time.time.domain.WeeklyAnalytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class DefaultTimeRepositoryImpl(
    private val timeBlockDao: TimeBlockDao
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

    override suspend fun getDailySummary(date: LocalDate): DailySummary {
        val blocks = getTimeBlocksByDate(date).first()
        
        val jobBreakdown = blocks.groupBy { it.jobId }
            .mapValues { (jobId, blocks) ->
                val job = getJobById(jobId)!!
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
        val weekEnd = weekStart.plus(kotlinx.datetime.DatePeriod(days = 6)) // 7 days total (0-6)
        val blocks = timeBlockDao.getByDateRange(weekStart.toString(), weekEnd.toString()).first()
            .map { it.toDomain() }
        
        val dailySummaries = mutableListOf<DailySummary>()
        for (i in 0 until 7) {
            val date = weekStart.plus(kotlinx.datetime.DatePeriod(days = i))
            val dailyBlocks = blocks.filter { 
                it.startTime.date == date 
            }
            
            val jobBreakdown = dailyBlocks.groupBy { it.jobId }
                .mapValues { (jobId, blocks) ->
                    val job = getJobById(jobId)!!
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
                val job = getJobById(jobId)!!
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
            weekStart = weekStart,
            weekEnd = weekEnd,
            dailySummaries = dailySummaries,
            totalHours = totalHours,
            averageDailyHours = averageDailyHours,
            jobBreakdown = jobBreakdown
        )
    }
} 