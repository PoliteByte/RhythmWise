package com.veleda.cyclewise

import android.content.pm.ApplicationInfo
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * Tests for [shouldApplySecureFlag] — the release-only gate for
 * `FLAG_SECURE` screenshot protection on the main window — and
 * [crashLoggingExceptionHandler] — the log-then-delegate global crash handler.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Test
    fun `given non-debuggable build when deciding secure flag then it is applied`() {
        // Given an ApplicationInfo without the debuggable bit (release build)
        val releaseInfo = ApplicationInfo().apply {
            flags = 0
        }

        // When deciding whether to secure the window
        val applied = shouldApplySecureFlag(releaseInfo)

        // Then the flag is applied
        assertTrue(applied)
    }

    @Test
    fun `given debuggable build when deciding secure flag then it is skipped`() {
        // Given an ApplicationInfo with the debuggable bit set (debug build)
        val debugInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_DEBUGGABLE
        }

        // When deciding whether to secure the window
        val applied = shouldApplySecureFlag(debugInfo)

        // Then the flag is skipped so screen capture works for QA
        assertFalse(applied)
    }

    @Test
    fun `given debuggable bit among other flags when deciding secure flag then it is skipped`() {
        // Given a realistic ApplicationInfo carrying several flags at once
        val debugInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_DEBUGGABLE or
                ApplicationInfo.FLAG_ALLOW_BACKUP or
                ApplicationInfo.FLAG_HAS_CODE
        }

        // When deciding whether to secure the window
        val applied = shouldApplySecureFlag(debugInfo)

        // Then the debuggable bit alone controls the decision
        assertFalse(applied)
    }

    @Test
    fun `given crash handler when exception uncaught then it delegates to previous handler`() {
        // Given a recording previous handler and an uncaught exception
        var delegatedThread: Thread? = null
        var delegatedThrowable: Throwable? = null
        val previous = Thread.UncaughtExceptionHandler { t, e ->
            delegatedThread = t
            delegatedThrowable = e
        }
        val handler = crashLoggingExceptionHandler(previous)
        val crash = IllegalStateException("boom")

        // When the handler runs
        handler.uncaughtException(Thread.currentThread(), crash)

        // Then the previous (framework) handler receives the same thread and throwable,
        // so the process still dies instead of surviving with a dead UI
        assertSame(Thread.currentThread(), delegatedThread)
        assertSame(crash, delegatedThrowable)
    }

    @Test
    fun `given crash handler when exception uncaught then full stack trace is logged`() {
        // Given
        ShadowLog.clear()
        val handler = crashLoggingExceptionHandler(previousHandler = null)
        val crash = IllegalStateException("boom")

        // When
        handler.uncaughtException(Thread.currentThread(), crash)

        // Then the crash is logged at error level with the throwable attached
        val logged = ShadowLog.getLogsForTag("GlobalCrashHandler")
        assertEquals(1, logged.size)
        assertEquals(Log.ERROR, logged.first().type)
        assertSame(crash, logged.first().throwable)
    }

    @Test
    fun `given no previous handler when exception uncaught then handler does not throw`() {
        // Given a handler built with no delegate (unreachable on Android, tolerated for safety)
        val handler = crashLoggingExceptionHandler(previousHandler = null)

        // When / Then — logging still happens and no secondary exception escapes
        handler.uncaughtException(Thread.currentThread(), RuntimeException("boom"))
    }
}
