package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.toEntity
import com.andriybobchuk.mooney.core.data.database.AccountDao
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.core.data.database.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DefaultCoreRepositoryImpl(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) : CoreRepository {

    ///////////////////////////// ACCOUNTS //////////////////////////////////////////////////
    //private val accounts = AccountDataSource.accounts.toMutableList()


    override suspend fun upsertAccount(account: Account) {
        accountDao.upsert(account.toEntity())
    }

    override suspend fun deleteAccount(id: Int) {
        accountDao.delete(id)
    }

    override fun getAllAccounts(): Flow<List<Account?>> {
        return accountDao.getAll().map { it.map { it.toDomain() } }
    }

    override suspend fun getAccountById(id: Int): Account? {
        return accountDao.getById(id)?.toDomain()
    }

    /////////////////////////////////// TRANSACTIONS /////////////////////////////////
//
//    override suspend fun upsertTransaction(transaction: Transaction) {
//        transactionDao.upsert(transaction.toEntity())
//    }

    override suspend fun upsertTransaction(transaction: Transaction) {
        val existingTransaction = transactionDao.getById(transaction.id)

        // 1. If updating: reverse the old transaction's effect
        if (existingTransaction != null) {
            val oldAccount = accountDao.getById(existingTransaction.accountId)
            val oldCategoryType = getAllCategories().find { it.id == existingTransaction.subcategoryId }?.getRoot()?.type

            if (oldAccount != null && oldCategoryType != null) {
                val reversedAmount = when (oldCategoryType) {
                    CategoryType.EXPENSE -> oldAccount.amount + existingTransaction.amount
                    CategoryType.INCOME -> oldAccount.amount - existingTransaction.amount
                }
                accountDao.upsert(oldAccount.copy(amount = reversedAmount))
            }
        }

        // 2. Apply the new transaction's effect
        val newAccount = accountDao.getById(transaction.account.id)
        if (newAccount != null) {
            val categoryType = transaction.subcategory.getRoot().type
            val adjustedAmount = when (categoryType) {
                CategoryType.EXPENSE -> newAccount.amount - transaction.amount
                CategoryType.INCOME -> newAccount.amount + transaction.amount
            }
            accountDao.upsert(newAccount.copy(amount = adjustedAmount))
        }

        // 3. Upsert transaction
        transactionDao.upsert(transaction.toEntity())
    }


    override suspend fun deleteTransaction(id: Int) {
        val transaction = transactionDao.getById(id)

        if (transaction != null) {
            val account = accountDao.getById(transaction.accountId)
            val categoryType = getAllCategories().find { it.id == transaction.subcategoryId }?.getRoot()?.type

            if (account != null && categoryType != null) {
                val adjustedAmount = when (categoryType) {
                    CategoryType.EXPENSE -> account.amount + transaction.amount
                    CategoryType.INCOME -> account.amount - transaction.amount
                }
                accountDao.upsert(account.copy(amount = adjustedAmount))
            }

            transactionDao.delete(id)
        }
    }


    override fun getAllTransactions(): Flow<List<Transaction?>> {
        val accountsFlow = getAllAccounts()
        val categories = getAllCategories()

        return transactionDao.getAll().combine(accountsFlow) { transactionEntities, accounts ->
            transactionEntities.map { transactionEntity ->
                val subcategory = categories.find {
                    it.id == transactionEntity.subcategoryId
                }

                val account = accounts.find {
                    it?.id == transactionEntity.accountId
                }

                if (subcategory != null && account != null) {
                    transactionEntity.toDomain(subcategory, account)
                } else {
                    null // Skip broken/missing data to prevent crash
                }
            }
        }
    }

    override suspend fun getTransactionById(id: Int): Transaction? {
        val entity = transactionDao.getById(id) ?: return null

        val subcategory = getAllCategories().find {
            it.id == entity.subcategoryId
        }

        val accounts = getAllAccounts().first()
        val account = accounts.find {
            it?.id == entity.accountId
        }

        return entity.toDomain(subcategory!!, account!!)
    }


//    private val transactions: MutableList<Transaction> = TransactionDataSource.transactions
//
//    override fun getAllTransactions(): List<Transaction> = transactions
//
//    override fun getTransactionById(id: Int): Transaction? = transactions.find { it.id == id }
//
//    override fun addTransaction(transaction: Transaction): Transaction {
//        val newId = (transactions.maxOfOrNull { it.id } ?: 0) + 1
//        val newTransaction = transaction.copy(id = newId)
//        transactions.add(newTransaction)
//        return newTransaction
//    }
//
//    override fun updateTransaction(transaction: Transaction): Boolean {
//        val index = transactions.indexOfFirst { it.id == transaction.id }
//        return if (index != -1) {
//            transactions[index] = transaction
//            true
//        } else {
//            false
//        }
//    }
//
//    override fun deleteTransaction(id: Int): Boolean {
//        return transactions.removeAll { it.id == id }
//    }


    ///////////////////////////// CATEGORIES - DO NOT MODIFY //////////////////////
    private val categoriesById = CategoryDataSource.categories.associateBy { it.id }

    override fun getAllCategories(): List<Category> = CategoryDataSource.categories

    override fun getCategoryById(id: String): Category? = categoriesById[id]

    override fun getTopLevelCategories(): List<Category> =
        CategoryDataSource.categories.filter { it.parent == null }

    override fun getSubcategories(parentId: String): List<Category> =
        CategoryDataSource.categories.filter { it.parent?.id == parentId }

}
