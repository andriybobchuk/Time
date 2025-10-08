package com.andriybobchuk.time.time.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: Long,
    val isMain: Boolean = false,
    val hourlyRatePLN: Double? = null,
    val isHidden: Boolean = false
)