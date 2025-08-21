package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.StatusUpdate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

fun StatusUpdateEntity.toDomain(jobName: String): StatusUpdate {
    return StatusUpdate(
        id = id,
        jobId = jobId,
        jobName = jobName,
        date = LocalDate.parse(date),
        statusText = statusText,
        lastUpdated = LocalDateTime.parse(lastUpdated)
    )
}

fun StatusUpdate.toEntity(): StatusUpdateEntity {
    return StatusUpdateEntity(
        id = id,
        jobId = jobId,
        date = date.toString(),
        statusText = statusText,
        lastUpdated = lastUpdated.toString()
    )
}