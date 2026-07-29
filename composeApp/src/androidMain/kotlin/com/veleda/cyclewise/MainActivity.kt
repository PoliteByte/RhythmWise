package com.veleda.cyclewise

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.veleda.cyclewise.ui.CycleWiseAppUI

/**
 * Single-activity entry point for the RhythmWise application.
 *
 * Configures three security/UX concerns before setting the Compose content:
 * - **FLAG_SECURE** — prevents screenshots and the recent-apps thumbnail from
 *   exposing sensitive health data. Applied only to non-debuggable (release)
 *   builds so that emulator-based UI verification and Play Store screenshot
 *   sessions can capture the screen from a debug build; every distributed
 *   build keeps the protection.
 * - **Global crash handler** — logs uncaught exceptions via [Log.e], then
 *   delegates to the framework's default handler so the process still dies and
 *   relaunches cleanly. See [crashLoggingExceptionHandler].
 * - **Splash screen** — integrates the AndroidX SplashScreen API for a seamless
 *   cold-start transition.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (shouldApplySecureFlag(applicationInfo)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        Thread.setDefaultUncaughtExceptionHandler(
            crashLoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler())
        )

        setContent {
            CycleWiseAppUI()
        }
    }
}

/**
 * Decides whether [WindowManager.LayoutParams.FLAG_SECURE] should be applied to
 * the activity window.
 *
 * Returns `true` for non-debuggable (release) builds, which must block
 * screenshots, screen recording, and recent-apps thumbnails to protect health
 * data. Returns `false` for debuggable builds so emulator-driven UI
 * verification and Play Store screenshot sessions can capture the screen —
 * debuggable builds are never distributed (Google Play rejects them).
 *
 * @param applicationInfo the running app's [ApplicationInfo], whose
 *   [ApplicationInfo.FLAG_DEBUGGABLE] bit identifies debug builds.
 */
internal fun shouldApplySecureFlag(applicationInfo: ApplicationInfo): Boolean =
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0

/**
 * Creates the global uncaught-exception handler installed by [MainActivity].
 *
 * Logs the full stack trace via [Log.e] (logcat is the app's only crash channel —
 * there is deliberately no remote crash reporting), then **delegates to
 * [previousHandler]** — on a real device that is the Android framework handler,
 * which shows the crash dialog and kills the process so the next launch starts
 * clean. The previous implementation swallowed the exception after logging,
 * leaving the process alive with a dead UI that users experienced as the app
 * "never loading" (issue #141). Never swallow crashes here.
 *
 * @param previousHandler the handler that was installed before ours; null is
 *   tolerated for safety but does not occur on Android, where the framework
 *   installs its own default handler before any app code runs.
 */
internal fun crashLoggingExceptionHandler(
    previousHandler: Thread.UncaughtExceptionHandler?,
): Thread.UncaughtExceptionHandler =
    Thread.UncaughtExceptionHandler { thread, throwable ->
        Log.e("GlobalCrashHandler", "Uncaught exception in ${thread.name}", throwable)
        previousHandler?.uncaughtException(thread, throwable)
    }

@Preview
@Composable
fun AppAndroidPreview() {
    CycleWiseAppUI()
}