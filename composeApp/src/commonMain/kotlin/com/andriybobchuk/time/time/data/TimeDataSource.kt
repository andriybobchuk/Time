package com.andriybobchuk.time.time.data

import com.andriybobchuk.time.time.domain.Job

object TimeDataSource {
    val jobs = listOf(
        Job(
            id = "rivian",
            name = "Rivian",
            color = 0xFFFBCF6C
        ),
        Job(
            id = "plato",
            name = "Plato",
            color = 0xFF6CFB95
        )
    )
} 