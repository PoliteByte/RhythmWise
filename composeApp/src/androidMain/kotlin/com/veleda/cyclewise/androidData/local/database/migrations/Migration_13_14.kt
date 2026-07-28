package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v13 -> v14: Adds the `user_cycle_settings` single-row table (issue #143).
 *
 * Stores the user's self-reported typical cycle length (nullable — never asked
 * or skipped) and the default period length used by one-tap period auto-fill
 * (issue #144). Lives in the encrypted database by explicit decision: these are
 * health-adjacent values and the app's privacy model keeps all health data
 * behind SQLCipher.
 *
 * No data migration — the table starts empty and callers treat the missing row
 * as "defaults" (typical = null, period length = 5).
 */
object Migration_13_14 : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_cycle_settings` (
                `id` INTEGER NOT NULL,
                `typical_cycle_length_days` INTEGER,
                `default_period_length_days` INTEGER NOT NULL DEFAULT 5,
                PRIMARY KEY(`id`)
            )
            """
        )
    }
}
