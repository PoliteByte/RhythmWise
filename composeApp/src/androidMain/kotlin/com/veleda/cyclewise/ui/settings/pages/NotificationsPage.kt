package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.sound.LocalSoundEffects
import com.veleda.cyclewise.sound.SoundEffect
import com.veleda.cyclewise.ui.settings.ReminderSettings
import com.veleda.cyclewise.ui.settings.NotificationSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlin.math.roundToInt

/**
 * Page 2 — Notifications: UI sound effects, plus period prediction, medication,
 * and hydration reminders.
 */
@Composable
internal fun NotificationsPage(
    state: NotificationSettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val dims = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Spacer(Modifier.height(dims.sm))

        // ── Sound Card ───────────────────────────────────────────────
        SoundSettingsCard(state = state, onEvent = onEvent)

        // ── Notifications Card ───────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_notifications)) {
            ReminderSettings(
                periodEnabled = state.periodReminderEnabled,
                periodDaysBefore = state.periodDaysBefore,
                periodPrivacyAccepted = state.periodPrivacyAccepted,
                medicationEnabled = state.medicationReminderEnabled,
                medicationHour = state.medicationHour,
                medicationMinute = state.medicationMinute,
                hydrationEnabled = state.hydrationReminderEnabled,
                hydrationGoalCups = state.hydrationGoalCups,
                hydrationFrequencyHours = state.hydrationFrequencyHours,
                hydrationStartHour = state.hydrationStartHour,
                hydrationEndHour = state.hydrationEndHour,
                showPermissionRationale = state.showPermissionRationale,
                showPrivacyDialog = state.showPrivacyDialog,
                onEvent = onEvent,
                showTitle = false,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }
}

/**
 * "Sound" section card: the UI sound effects switch plus a 0–100 volume slider.
 *
 * The slider dispatches [SettingsEvent.SoundVolumeChanged] continuously while
 * dragging and plays a preview at the released level so the user hears the
 * volume they just chose.
 */
@Composable
private fun SoundSettingsCard(
    state: NotificationSettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val dims = LocalDimensions.current
    val sounds = LocalSoundEffects.current

    SettingsSectionCard(title = stringResource(R.string.settings_section_sound)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.sound_effects_label)) },
            supportingContent = { Text(stringResource(R.string.sound_effects_description)) },
            trailingContent = {
                Switch(
                    checked = state.soundEffectsEnabled,
                    onCheckedChange = { enabled ->
                        // preview() bypasses the player's mirrored enabled flag, which
                        // only flips after the async DataStore write lands — a plain
                        // play() here would be silently dropped when re-enabling.
                        if (enabled) sounds.preview(SoundEffect.TOGGLE_ON, state.soundEffectsVolume)
                        onEvent(SettingsEvent.SoundEffectsToggled(enabled))
                    },
                    modifier = Modifier.testTag("sound_effects_toggle"),
                )
            }
        )
        Text(
            stringResource(R.string.settings_sound_volume, state.soundEffectsVolume),
            modifier = Modifier.padding(horizontal = dims.md),
        )
        val draggedVolume = remember { mutableIntStateOf(state.soundEffectsVolume) }
        Slider(
            value = state.soundEffectsVolume.toFloat(),
            onValueChange = {
                draggedVolume.intValue = it.roundToInt()
                onEvent(SettingsEvent.SoundVolumeChanged(draggedVolume.intValue))
            },
            onValueChangeFinished = {
                sounds.preview(SoundEffect.TAP, draggedVolume.intValue)
            },
            valueRange = 0f..100f,
            enabled = state.soundEffectsEnabled,
            modifier = Modifier
                .padding(horizontal = dims.md)
                .testTag("sound_volume_slider"),
        )
    }
}
