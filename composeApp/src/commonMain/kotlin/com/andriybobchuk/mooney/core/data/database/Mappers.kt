package com.andriybobchuk.mooney.core.data.database

import com.andriybobchuk.mooney.core.data.database.AccountEntity
import com.andriybobchuk.mooney.core.data.database.TransactionEntity
import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Transaction
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
