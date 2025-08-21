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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        // Create status_updates table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS status_updates (
                id TEXT NOT NULL PRIMARY KEY,
                jobId TEXT NOT NULL,
                date TEXT NOT NULL,
                statusText TEXT NOT NULL,
                lastUpdated TEXT NOT NULL
            )
        """.trimIndent())
    }
}