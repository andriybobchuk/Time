package com.plcoding.bookpedia.mooney.domain

import kotlinx.coroutines.flow.Flow

interface CoreRepository {

    // Account CRUD
    suspend fun upsertAccount(account: Account)
    suspend fun deleteAccount(id: Int)
    fun getAllAccounts(): Flow<List<Account?>>
    suspend fun getAccountById(id: Int): Account?

    // Transaction CRUD
    suspend fun upsertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: Int)
    fun getAllTransactions(): Flow<List<Transaction?>>
    suspend fun getTransactionById(id: Int): Transaction?

//    fun getAllTransactions(): List<Transaction>
//    fun getTransactionById(id: Int): Transaction?
//    fun addTransaction(transaction: Transaction): Transaction
//    fun updateTransaction(transaction: Transaction): Boolean
//    fun deleteTransaction(id: Int): Boolean

    // Category
    fun getAllCategories(): List<Category>
    fun getCategoryById(id: String): Category?
    fun getTopLevelCategories(): List<Category>
    fun getSubcategories(parentId: String): List<Category>
}
