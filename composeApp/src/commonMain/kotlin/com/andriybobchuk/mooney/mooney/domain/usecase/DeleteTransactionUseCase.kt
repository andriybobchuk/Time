package com.andriybobchuk.mooney.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.CoreRepository

class DeleteTransactionUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(id: Int) {
        // Extract the business logic from DefaultCoreRepositoryImpl.deleteTransaction
        val transaction = repository.getTransactionById(id)

        if (transaction != null) {
            val account = repository.getAccountById(transaction.account.id)
            val categoryType = repository.getAllCategories().find { it.id == transaction.subcategory.id }?.getRoot()?.type

            if (account != null && categoryType != null) {
                val adjustedAmount = when (categoryType) {
                    com.andriybobchuk.mooney.mooney.domain.CategoryType.EXPENSE -> account.amount + transaction.amount
                    com.andriybobchuk.mooney.mooney.domain.CategoryType.INCOME -> account.amount - transaction.amount
                }
                repository.upsertAccount(account.copy(amount = adjustedAmount))
            }

            repository.deleteTransaction(id)
        }
    }
} 