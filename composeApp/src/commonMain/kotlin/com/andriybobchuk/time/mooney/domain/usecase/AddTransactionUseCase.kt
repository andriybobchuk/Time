package com.andriybobchuk.time.mooney.domain.usecase

import com.andriybobchuk.time.mooney.domain.CoreRepository
import com.andriybobchuk.time.mooney.domain.Transaction

class AddTransactionUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        // Extract the business logic from DefaultCoreRepositoryImpl.upsertTransaction
        val existingTransaction = repository.getTransactionById(transaction.id)

        // 1. If updating: reverse the old transaction's effect
        if (existingTransaction != null) {
            val oldAccount = repository.getAccountById(existingTransaction.account.id)
            val oldCategoryType = repository.getAllCategories().find { it.id == existingTransaction.subcategory.id }?.getRoot()?.type

            if (oldAccount != null && oldCategoryType != null) {
                val reversedAmount = when (oldCategoryType) {
                    com.andriybobchuk.time.mooney.domain.CategoryType.EXPENSE -> oldAccount.amount + existingTransaction.amount
                    com.andriybobchuk.time.mooney.domain.CategoryType.INCOME -> oldAccount.amount - existingTransaction.amount
                }
                repository.upsertAccount(oldAccount.copy(amount = reversedAmount))
            }
        }

        // 2. Apply the new transaction's effect
        val newAccount = repository.getAccountById(transaction.account.id)
        if (newAccount != null) {
            val categoryType = transaction.subcategory.getRoot().type
            val adjustedAmount = when (categoryType) {
                com.andriybobchuk.time.mooney.domain.CategoryType.EXPENSE -> newAccount.amount - transaction.amount
                com.andriybobchuk.time.mooney.domain.CategoryType.INCOME -> newAccount.amount + transaction.amount
            }
            repository.upsertAccount(newAccount.copy(amount = adjustedAmount))
        }

        // 3. Upsert transaction
        repository.upsertTransaction(transaction)
    }
} 