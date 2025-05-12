package com.recallit.core.data.database

import com.plcoding.bookpedia.mooney.domain.Account
import com.plcoding.bookpedia.mooney.domain.Category
import com.plcoding.bookpedia.mooney.domain.Currency
import com.plcoding.bookpedia.mooney.domain.Transaction
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
