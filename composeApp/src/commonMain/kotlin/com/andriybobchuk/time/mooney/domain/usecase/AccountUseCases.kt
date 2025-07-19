package com.andriybobchuk.time.mooney.domain.usecase

import com.andriybobchuk.time.mooney.domain.Account
import com.andriybobchuk.time.mooney.domain.CoreRepository
import kotlinx.coroutines.flow.Flow

class AddAccountUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(account: Account) {
        repository.upsertAccount(account)
    }
}

class DeleteAccountUseCase(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(accountId: Int) {
        repository.deleteAccount(accountId)
    }
}

class GetAccountsUseCase(
    private val repository: CoreRepository
) {
    operator fun invoke(): Flow<List<Account?>> {
        return repository.getAllAccounts()
    }
    
    suspend operator fun invoke(id: Int): Account? {
        return repository.getAccountById(id)
    }
} 