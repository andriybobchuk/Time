package com.andriybobchuk.time.time.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_updates")
data class StatusUpdateEntity(
    @PrimaryKey val id: String, // jobId_date format (e.g., "rivian_2024-01-15")
    val jobId: String,
    val date: String, // ISO date format (YYYY-MM-DD)
    val statusText: String,
    val lastUpdated: String // ISO timestamp
)