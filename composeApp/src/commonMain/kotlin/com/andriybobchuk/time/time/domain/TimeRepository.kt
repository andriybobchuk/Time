package com.andriybobchuk.time.time.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TimeRepository {
    // Time Blocks
    suspend fun upsertTimeBlock(timeBlock: TimeBlock)
    suspend fun deleteTimeBlock(id: Int)
    suspend fun getTimeBlockById(id: Int): TimeBlock?
    fun getAllTimeBlocks(): Flow<List<TimeBlock>>
    fun getTimeBlocksByDate(date: LocalDate): Flow<List<TimeBlock>>
    fun getTimeBlocksByDateIncludingCrossDay(date: LocalDate): Flow<List<TimeBlock>>
    fun getActiveTimeBlock(): Flow<TimeBlock?>
    suspend fun getActiveTimeBlockSync(): TimeBlock?
    
    // Jobs (hardcoded for now)
    fun getJobs(): List<Job>
    fun getJobById(id: String): Job?
    
    // Analytics
    suspend fun getDailySummary(date: LocalDate): DailySummary
    suspend fun getWeeklyAnalytics(weekStart: LocalDate): WeeklyAnalytics
    suspend fun getLast7DaysAnalytics(endDate: LocalDate): WeeklyAnalytics
    
    // Status Updates
    suspend fun upsertStatusUpdate(statusUpdate: StatusUpdate)
    fun getStatusUpdatesByDate(date: LocalDate): Flow<List<StatusUpdate>>
    suspend fun getStatusUpdateByJobAndDate(jobId: String, date: LocalDate): StatusUpdate?
    suspend fun deleteStatusUpdate(id: String)
} 