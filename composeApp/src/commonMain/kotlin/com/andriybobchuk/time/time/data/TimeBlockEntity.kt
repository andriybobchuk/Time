package com.andriybobchuk.time.time.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_blocks")
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: String,
    val jobName: String,
    val startTime: String, // ISO string format
    val endTime: String?, // ISO string format
    val duration: Long?, // in milliseconds
    val effectiveness: String? = null // new field for persistence
) 