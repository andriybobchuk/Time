package com.andriybobchuk.mooney.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val currency: String,
    val emoji: String
)

@Entity
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subcategoryId: String,
    val amount: Double,
    val accountId: Int,
    val date: String
)

