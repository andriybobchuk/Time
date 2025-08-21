package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.StatusUpdate
import com.andriybobchuk.time.time.domain.TimeRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class UpsertStatusUpdateUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(jobId: String, date: LocalDate, statusText: String): Result<Unit> {
        return try {
            val job = repository.getJobById(jobId) ?: return Result.failure(
                Exception("Job with id $jobId not found")
            )
            
            val statusUpdate = StatusUpdate(
                id = "${jobId}_${date}",
                jobId = jobId,
                jobName = job.name,
                date = date,
                statusText = statusText,
                lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            
            repository.upsertStatusUpdate(statusUpdate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}