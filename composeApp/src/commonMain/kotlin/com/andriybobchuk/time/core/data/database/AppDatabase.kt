package com.andriybobchuk.time.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.andriybobchuk.time.time.data.TimeBlockDao
import com.andriybobchuk.time.time.data.TimeBlockEntity

@Database(entities = [TransactionEntity::class, AccountEntity::class, TimeBlockEntity::class], version = 3)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val accountDao: AccountDao
    abstract val timeBlockDao: TimeBlockDao

    companion object {
        const val DB_NAME = "time_debug_v2.db"
    }
}
