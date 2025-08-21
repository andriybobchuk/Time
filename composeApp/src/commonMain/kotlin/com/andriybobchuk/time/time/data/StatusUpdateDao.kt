package com.andriybobchuk.time.time.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusUpdateDao {
    @Upsert
    suspend fun upsert(statusUpdate: StatusUpdateEntity)

    @Query("SELECT * FROM status_updates WHERE date = :date")
    fun getByDate(date: String): Flow<List<StatusUpdateEntity>>

    @Query("SELECT * FROM status_updates WHERE id = :id")
    suspend fun getById(id: String): StatusUpdateEntity?

    @Query("SELECT * FROM status_updates WHERE jobId = :jobId AND date = :date LIMIT 1")
    suspend fun getByJobAndDate(jobId: String, date: String): StatusUpdateEntity?

    @Query("DELETE FROM status_updates WHERE id = :id")
    suspend fun delete(id: String)
}