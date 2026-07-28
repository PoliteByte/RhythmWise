package com.veleda.cyclewise.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.runtime.CompositionLocalProvider
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [PassphraseLoadingIndicator] — the pre-resolution loading state of
 * the passphrase screen that replaced the blank early-return (issue #141).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class PassphraseLoadingIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given loading indicator when composed then progress container is displayed`() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    PassphraseLoadingIndicator()
                }
            }
        }

        // Then — the screen is never blank while settings resolve
        composeTestRule.onNodeWithTag("passphrase-loading").assertIsDisplayed()
    }
}
