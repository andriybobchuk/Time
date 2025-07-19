package com.andriybobchuk.mooney.core.data.database

import androidx.room.RoomDatabaseConstructor
import com.andriybobchuk.mooney.core.data.database.AppDatabase

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
