package com.plcoding.bookpedia.mooney.domain

import com.recallit.core.data.database.AccountEntity
import com.recallit.core.data.database.TransactionEntity


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
