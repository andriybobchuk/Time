package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.Job

fun JobEntity.toDomain(): Job {
    return Job(
        id = id,
        name = name,
        color = color,
        isMain = isMain,
        hourlyRatePLN = hourlyRatePLN,
        isHidden = isHidden
    )
}

fun Job.toEntity(): JobEntity {
    return JobEntity(
        id = id,
        name = name,
        color = color,
        isMain = isMain,
        hourlyRatePLN = hourlyRatePLN,
        isHidden = isHidden
    )
}