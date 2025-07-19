package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.TimeBlock
import kotlinx.datetime.LocalDateTime

fun TimeBlockEntity.toDomain(): TimeBlock {
    return TimeBlock(
        id = id,
        jobId = jobId,
        jobName = jobName,
        startTime = LocalDateTime.parse(startTime),
        endTime = endTime?.let { LocalDateTime.parse(it) },
        duration = duration
    )
}

fun TimeBlock.toEntity(): TimeBlockEntity {
    return TimeBlockEntity(
        id = id,
        jobId = jobId,
        jobName = jobName,
        startTime = startTime.toString(),
        endTime = endTime?.toString(),
        duration = duration
    )
} 