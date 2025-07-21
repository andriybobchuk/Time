package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.Job

object TimeDataSource {
    val jobs = listOf(
        Job(
            id = "rivian",
            name = "Rivian",
            color = 0xFFFFC747
        ),
        Job(
            id = "plato",
            name = "Plato",
            color = 0xFF41D0FF
        ),
        Job(
            id = "business",
            name = "Business",
            color = 0xFF4CAF50
        )
    )
} 