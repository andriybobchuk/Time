package com.andriybobchuk.time.mooney.domain.usecase

import com.andriybobchuk.time.mooney.domain.CoreRepository
import com.andriybobchuk.time.mooney.domain.Transaction
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val repository: CoreRepository
) {
    operator fun invoke(): Flow<List<Transaction?>> {
        return repository.getAllTransactions()
    }
    
    suspend operator fun invoke(id: Int): Transaction? {
        return repository.getTransactionById(id)
    }
} 