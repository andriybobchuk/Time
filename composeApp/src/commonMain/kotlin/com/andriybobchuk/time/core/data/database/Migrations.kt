package com.andriybobchuk.time.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_3_6 = object : Migration(3, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // Add description column to time_blocks table
        connection.execSQL("ALTER TABLE time_blocks ADD COLUMN description TEXT")
    }
}