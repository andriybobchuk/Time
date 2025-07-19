package com.andriybobchuk.mooney.core.data.database

import androidx.room.RoomDatabase
import com.andriybobchuk.mooney.core.data.database.AppDatabase

expect class MooneyDatabaseFactory {
    fun create(): RoomDatabase.Builder<AppDatabase>
}