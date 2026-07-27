package com.veleda.cyclewise

import com.veleda.cyclewise.settings.AUTOLOCK_IMMEDIATE_MINUTES
import com.veleda.cyclewise.settings.AUTOLOCK_NEVER_MINUTES
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [shouldLockNow] — the pure autolock decision applied when the app
 * returns to the foreground. The first four tests pin the pre-#149 semantics
 * (immediate lock, missing timestamp, threshold boundary); the rest cover the
 * "Never" sentinel added by #149.
 */
class CycleWiseAppTest {

    private val tenMinutesMs = 10 * 60_000L

    @Test
    fun `given immediate setting when foregrounded then always locks`() {
        // Given minutes == 0 — even with no background timestamp
        // When / Then
        assertTrue(shouldLockNow(AUTOLOCK_IMMEDIATE_MINUTES, lastBgAtElapsed = -1L, nowElapsed = 1_000L))
        assertTrue(shouldLockNow(AUTOLOCK_IMMEDIATE_MINUTES, lastBgAtElapsed = 500L, nowElapsed = 501L))
    }

    @Test
    fun `given no background timestamp when foregrounded then does not lock`() {
        // Given a timed setting but no recorded ON_STOP timestamp
        // When / Then
        assertFalse(shouldLockNow(minutes = 10, lastBgAtElapsed = -1L, nowElapsed = 1_000_000L))
        assertFalse(shouldLockNow(minutes = 10, lastBgAtElapsed = 0L, nowElapsed = 1_000_000L))
    }

    @Test
    fun `given elapsed time at or past threshold when foregrounded then locks`() {
        // Given exactly the threshold and just past it
        val last = 100_000L
        // When / Then
        assertTrue(shouldLockNow(minutes = 10, lastBgAtElapsed = last, nowElapsed = last + tenMinutesMs))
        assertTrue(shouldLockNow(minutes = 10, lastBgAtElapsed = last, nowElapsed = last + tenMinutesMs + 1))
    }

    @Test
    fun `given elapsed time below threshold when foregrounded then does not lock`() {
        // Given one millisecond short of the threshold
        val last = 100_000L
        // When / Then
        assertFalse(shouldLockNow(minutes = 10, lastBgAtElapsed = last, nowElapsed = last + tenMinutesMs - 1))
    }

    @Test
    fun `given never setting when foregrounded after a long absence then does not lock`() {
        // Given the Never sentinel and days of elapsed background time
        val last = 1_000L
        val threeDaysMs = 3 * 24 * 60 * 60_000L
        // When / Then
        assertFalse(shouldLockNow(AUTOLOCK_NEVER_MINUTES, lastBgAtElapsed = last, nowElapsed = last + threeDaysMs))
    }

    @Test
    fun `given never setting when no background timestamp then does not lock`() {
        // Given
        // When / Then
        assertFalse(shouldLockNow(AUTOLOCK_NEVER_MINUTES, lastBgAtElapsed = -1L, nowElapsed = 1_000L))
    }
}
