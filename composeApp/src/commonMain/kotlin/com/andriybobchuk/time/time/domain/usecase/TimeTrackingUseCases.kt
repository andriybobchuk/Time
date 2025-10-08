package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.*
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
        return repository.getTimeBlocksByDateIncludingCrossDay(date)
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
    operator fun invoke(): Flow<List<Job>> {
        return repository.getVisibleJobs()
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

// Advanced Analytics Use Cases
class GetWeekAnalyticsUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(): WeekAnalytics {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return repository.getWeekAnalytics(today)
    }
}

class GetSixWeekAnalyticsUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(): SixWeekAnalytics {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return repository.getSixWeekAnalytics(today)
    }
}

class GetSixMonthAnalyticsUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(): SixMonthAnalytics {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return repository.getSixMonthAnalytics(today)
    }
}

class GetWorkPatternInsightsUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(): WorkPatternInsights {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return repository.getWorkPatternInsights(today)
    }
}

class GetAdvancedAnalyticsUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(): AdvancedAnalytics {
        val weekAnalytics = repository.getWeekAnalytics(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
        val sixWeekAnalytics = repository.getSixWeekAnalytics(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
        val sixMonthAnalytics = repository.getSixMonthAnalytics(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
        val workPatternInsights = repository.getWorkPatternInsights(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
        
        return AdvancedAnalytics(
            weekAnalytics = weekAnalytics,
            sixWeekAnalytics = sixWeekAnalytics,
            sixMonthAnalytics = sixMonthAnalytics,
            workPatternInsights = workPatternInsights
        )
    }
} 