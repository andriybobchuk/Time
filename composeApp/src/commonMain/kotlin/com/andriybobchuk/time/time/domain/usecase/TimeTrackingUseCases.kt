package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.Job
import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.TimeRepository
import com.andriybobchuk.time.time.domain.WeeklyAnalytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class StartTimeTrackingUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(jobId: String): Result<TimeBlock> {
        return try {
            val job = repository.getJobById(jobId) 
                ?: return Result.failure(IllegalArgumentException("Job not found"))
            
            val activeBlock = repository.getActiveTimeBlock().firstOrNull()
            if (activeBlock != null) {
                return Result.failure(IllegalStateException("Already tracking time for ${activeBlock.jobName}"))
            }
            
            val newBlock = TimeBlock(
                jobId = job.id,
                jobName = job.name,
                startTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            
            repository.upsertTimeBlock(newBlock)
            Result.success(newBlock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class StopTimeTrackingUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(): Result<TimeBlock?> {
        return try {
            val activeBlock = repository.getActiveTimeBlock().firstOrNull()
            if (activeBlock == null) {
                return Result.success(null)
            }
            
            val endTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val updatedBlock = activeBlock.copy(
                endTime = endTime,
                duration = activeBlock.calculateDuration()
            )
            
            repository.upsertTimeBlock(updatedBlock)
            Result.success(updatedBlock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetTimeBlocksUseCase(
    private val repository: TimeRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<TimeBlock>> {
        return repository.getTimeBlocksByDate(date)
    }
}

class GetActiveTimeBlockUseCase(
    private val repository: TimeRepository
) {
    operator fun invoke(): Flow<TimeBlock?> {
        return repository.getActiveTimeBlock()
    }
}

class GetJobsUseCase(
    private val repository: TimeRepository
) {
    operator fun invoke(): List<Job> {
        return repository.getJobs()
    }
}

class GetDailySummaryUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(date: LocalDate) = repository.getDailySummary(date)
}

class GetWeeklyAnalyticsUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(weekStart: LocalDate) = repository.getWeeklyAnalytics(weekStart)
}

class GetLast7DaysAnalyticsUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(): WeeklyAnalytics {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return repository.getLast7DaysAnalytics(today)
    }
}

class DeleteTimeBlockUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(id: Int) {
        repository.deleteTimeBlock(id)
    }
}

class UpsertTimeBlockUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(timeBlock: TimeBlock) {
        repository.upsertTimeBlock(timeBlock)
    }
} 