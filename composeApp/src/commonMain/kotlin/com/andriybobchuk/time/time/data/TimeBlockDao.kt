package com.andriybobchuk.time.time.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeBlockDao {
    @Upsert
    suspend fun upsert(timeBlock: TimeBlockEntity)

    @Query("DELETE FROM time_blocks WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM time_blocks WHERE id = :id")
    suspend fun getById(id: Int): TimeBlockEntity?

    @Query("SELECT * FROM time_blocks ORDER BY startTime DESC")
    fun getAll(): Flow<List<TimeBlockEntity>>

    @Query("SELECT * FROM time_blocks WHERE date(startTime) = :date ORDER BY startTime DESC")
    fun getByDate(date: String): Flow<List<TimeBlockEntity>>
    
    @Query("""
        SELECT * FROM time_blocks 
        WHERE (date(startTime) = :date) 
           OR (date(startTime) < :date AND (endTime IS NULL OR date(endTime) >= :date))
        ORDER BY startTime DESC
    """)
    fun getByDateIncludingCrossDayBlocks(date: String): Flow<List<TimeBlockEntity>>

    @Query("SELECT * FROM time_blocks WHERE endTime IS NULL LIMIT 1")
    fun getActiveBlock(): Flow<TimeBlockEntity?>

    @Query("SELECT * FROM time_blocks WHERE date(startTime) >= :startDate AND date(startTime) <= :endDate ORDER BY startTime DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<TimeBlockEntity>>
} 