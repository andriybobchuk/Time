package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.Job
import com.andriybobchuk.time.time.domain.TimeRepository
import kotlinx.coroutines.flow.Flow

class GetAllJobsUseCase(
    private val repository: TimeRepository
) {
    operator fun invoke(): Flow<List<Job>> {
        return repository.getAllJobs()
    }
}

class UpsertJobUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(job: Job): Result<Unit> {
        return try {
            // Always upsert the job first
            repository.upsertJob(job)
            
            // If this job is marked as main, set it as main (this will clear other main jobs)
            if (job.isMain) {
                repository.setMainJob(job.id)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteJobUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(job: Job): Result<Unit> {
        return try {
            repository.deleteJob(job)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SetMainJobUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(jobId: String): Result<Unit> {
        return try {
            repository.setMainJob(jobId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}