package com.andriybobchuk.time.core.data.database

import com.andriybobchuk.time.mooney.domain.Account
import com.andriybobchuk.time.mooney.domain.Category
import com.andriybobchuk.time.mooney.domain.Currency
import com.andriybobchuk.time.mooney.domain.Transaction
import kotlinx.datetime.LocalDate

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    title = title,
    amount = amount,
    currency = Currency.valueOf(currency),
    emoji = emoji
)

fun TransactionEntity.toDomain(
    subcategory: Category,
    account: Account
): Transaction = Transaction(
    id = id,
    subcategory = subcategory,
    amount = amount,
    account = account,
    date = LocalDate.parse(date)
)
