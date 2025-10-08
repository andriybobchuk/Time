package com.andriybobchuk.time.time.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Upsert
    suspend fun upsertJob(job: JobEntity)
    
    @Delete
    suspend fun deleteJob(job: JobEntity)
    
    @Query("SELECT * FROM jobs WHERE isHidden = 0")
    fun getVisibleJobs(): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs")
    fun getAllJobs(): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getJobById(id: String): JobEntity?
    
    @Query("UPDATE jobs SET isMain = 0")
    suspend fun clearMainJob()
    
    @Query("UPDATE jobs SET isMain = 1 WHERE id = :jobId")
    suspend fun setMainJob(jobId: String)
}