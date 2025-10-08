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

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Create jobs table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS jobs (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                color INTEGER NOT NULL,
                isMain INTEGER NOT NULL DEFAULT 0,
                hourlyRatePLN REAL,
                isHidden INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        
        // Insert existing hardcoded jobs
        connection.execSQL("""
            INSERT INTO jobs (id, name, color, isMain, hourlyRatePLN, isHidden)
            VALUES ('rivian', 'Rivian', 4294950727, 1, NULL, 0)
        """.trimIndent())
        
        connection.execSQL("""
            INSERT INTO jobs (id, name, color, isMain, hourlyRatePLN, isHidden)
            VALUES ('plato', 'Plato', 4279168511, 0, NULL, 0)
        """.trimIndent())
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        // Update effectiveness enum value from "Unproductive" to "Meetings"
        connection.execSQL("""
            UPDATE time_blocks 
            SET effectiveness = 'Meetings' 
            WHERE effectiveness = 'Unproductive'
        """.trimIndent())
    }
}