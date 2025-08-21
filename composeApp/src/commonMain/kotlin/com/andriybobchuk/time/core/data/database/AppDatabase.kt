package com.andriybobchuk.time.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.andriybobchuk.time.time.data.TimeBlockDao
import com.andriybobchuk.time.time.data.TimeBlockEntity
import com.andriybobchuk.time.time.data.StatusUpdateDao

@Database(entities = [TransactionEntity::class, AccountEntity::class, TimeBlockEntity::class, com.andriybobchuk.time.time.data.StatusUpdateEntity::class], version = 7)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val accountDao: AccountDao
    abstract val timeBlockDao: TimeBlockDao
    abstract val statusUpdateDao: StatusUpdateDao

    companion object {
        const val DB_NAME = "time_debug_v2.db"
    }
}
