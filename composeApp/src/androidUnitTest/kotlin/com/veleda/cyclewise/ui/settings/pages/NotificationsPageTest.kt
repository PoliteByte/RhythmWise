package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.sound.LocalSoundEffects
import com.veleda.cyclewise.sound.NoOpSoundEffectPlayer
import com.veleda.cyclewise.sound.SoundEffect
import com.veleda.cyclewise.sound.SoundEffectPlayer
import com.veleda.cyclewise.ui.settings.NotificationSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric-based Compose UI tests for [NotificationsPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class NotificationsPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        state: NotificationSettingsState = NotificationSettingsState(),
        onEvent: (SettingsEvent) -> Unit = {},
        soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
                LocalSoundEffects provides soundPlayer,
            ) {
                MaterialTheme {
                    NotificationsPage(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    // region Section visibility

    @Test
    fun notificationsSection_WHEN_rendered_THEN_titleDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun periodReminder_WHEN_rendered_THEN_sectionDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Period Prediction").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun medicationReminder_WHEN_rendered_THEN_sectionDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Daily Medication").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hydrationReminder_WHEN_rendered_THEN_sectionDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Hydration").performScrollTo().assertIsDisplayed()
    }

    // endregion

    // region Sound effects

    @Test
    fun soundSection_WHEN_rendered_THEN_toggleAndVolumeDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Sound").assertIsDisplayed()
        composeTestRule.onNodeWithText("UI sound effects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volume: 80%").assertIsDisplayed()
    }

    @Test
    fun soundToggle_WHEN_turnedOff_THEN_dispatchesSoundEffectsToggledFalse() {
        // GIVEN sound effects are enabled
        val events = mutableListOf<SettingsEvent>()
        setContent(
            state = NotificationSettingsState(soundEffectsEnabled = true),
            onEvent = { events.add(it) },
        )

        // WHEN tapping the toggle
        composeTestRule.onNodeWithTag("sound_effects_toggle").performClick()

        // THEN a disable event is dispatched
        assertEquals(listOf<SettingsEvent>(SettingsEvent.SoundEffectsToggled(false)), events)
    }

    @Test
    fun soundToggle_WHEN_turnedOn_THEN_previewsToggleSoundAtCurrentVolume() {
        // GIVEN sound effects are disabled at 60% volume
        val previews = mutableListOf<Pair<SoundEffect, Int>>()
        val recordingPlayer = object : SoundEffectPlayer {
            override fun play(effect: SoundEffect) = Unit
            override fun preview(effect: SoundEffect, volumePercent: Int) {
                previews.add(effect to volumePercent)
            }
        }
        setContent(
            state = NotificationSettingsState(soundEffectsEnabled = false, soundEffectsVolume = 60),
            soundPlayer = recordingPlayer,
        )

        // WHEN re-enabling via the toggle
        composeTestRule.onNodeWithTag("sound_effects_toggle").performClick()

        // THEN the toggle-on sound previews at the stored volume
        assertEquals(listOf(SoundEffect.TOGGLE_ON to 60), previews)
    }

    @Test
    fun volumeSlider_WHEN_soundDisabled_THEN_sliderIsDisabled() {
        setContent(state = NotificationSettingsState(soundEffectsEnabled = false))
        composeTestRule.onNodeWithTag("sound_volume_slider").assertIsNotEnabled()
    }

    // endregion

    // region Period reminder sub-sections

    @Test
    fun periodSubSection_WHEN_enabled_THEN_daysBeforeDisplayed() {
        setContent(
            state = NotificationSettingsState(
                periodReminderEnabled = true,
                periodPrivacyAccepted = true,
            ),
        )
        // "Days before: 2" should appear when enabled
        composeTestRule.onNodeWithText("Days before", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun periodSubSection_WHEN_disabled_THEN_daysBeforeNotDisplayed() {
        setContent(state = NotificationSettingsState(periodReminderEnabled = false))
        composeTestRule.onNodeWithText("Days before", substring = true).assertDoesNotExist()
    }

    // endregion

    // region Medication reminder sub-sections

    @Test
    fun medicationSubSection_WHEN_enabled_THEN_timeDisplayed() {
        setContent(state = NotificationSettingsState(medicationReminderEnabled = true))
        composeTestRule.onNodeWithText("Reminder time", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun medicationSubSection_WHEN_disabled_THEN_timeNotDisplayed() {
        setContent(state = NotificationSettingsState(medicationReminderEnabled = false))
        composeTestRule.onNodeWithText("Reminder time", substring = true).assertDoesNotExist()
    }

    // endregion

    // region Hydration reminder sub-sections

    @Test
    fun hydrationSubSection_WHEN_enabled_THEN_goalDisplayed() {
        setContent(state = NotificationSettingsState(hydrationReminderEnabled = true))
        composeTestRule.onNodeWithText("Daily goal", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hydrationSubSection_WHEN_disabled_THEN_goalNotDisplayed() {
        setContent(state = NotificationSettingsState(hydrationReminderEnabled = false))
        composeTestRule.onNodeWithText("Daily goal", substring = true).assertDoesNotExist()
    }

    // endregion
}
