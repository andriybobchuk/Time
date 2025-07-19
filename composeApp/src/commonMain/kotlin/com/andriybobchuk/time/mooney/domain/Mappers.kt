package com.andriybobchuk.time.mooney.domain

import com.andriybobchuk.time.core.data.database.AccountEntity
import com.andriybobchuk.time.core.data.database.TransactionEntity


fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    title = title,
    amount = amount,
    currency = currency.name,
    emoji = emoji
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    subcategoryId = subcategory.id,
    amount = amount,
    accountId = account.id,
    date = date.toString()
)
