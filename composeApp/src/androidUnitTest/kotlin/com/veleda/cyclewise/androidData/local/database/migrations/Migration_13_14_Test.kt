package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Migration_13_14] — creation of the `user_cycle_settings`
 * single-row table (issue #143). Follows the repo's migration-test pattern:
 * mock the database and assert on the captured SQL.
 */
class Migration_13_14_Test {

    @Test
    fun migrate_WHEN_run_THEN_createsUserCycleSettingsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sql = slot<String>()

        // ACT
        Migration_13_14.migrate(db)

        // ASSERT
        verify(exactly = 1) { db.execSQL(capture(sql)) }
        val statement = sql.captured
        assertTrue("creates the table", statement.contains("CREATE TABLE IF NOT EXISTS `user_cycle_settings`"))
        assertTrue("id primary key", statement.contains("PRIMARY KEY(`id`)"))
        assertTrue("nullable typical length", statement.contains("`typical_cycle_length_days` INTEGER"))
        assertTrue(
            "period length defaults to 5",
            statement.contains("`default_period_length_days` INTEGER NOT NULL DEFAULT 5"),
        )
    }
}
