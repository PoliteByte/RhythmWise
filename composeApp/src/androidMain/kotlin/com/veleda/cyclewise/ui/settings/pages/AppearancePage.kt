package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.veleda.cyclewise.domain.CycleLengthResolver
import com.veleda.cyclewise.domain.models.CycleSettings
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.veleda.cyclewise.R
import com.veleda.cyclewise.sound.LocalSoundEffects
import com.veleda.cyclewise.sound.SoundEffect
import com.veleda.cyclewise.ui.settings.PhaseVisibilitySettings
import com.veleda.cyclewise.ui.settings.AppearanceSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.ThemeMode
import kotlin.math.roundToInt

/**
 * Page 1 — Appearance: Theme, display toggles, calendar phase visibility, and insights display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearancePage(
    state: AppearanceSettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val dims = LocalDimensions.current
    val sounds = LocalSoundEffects.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Spacer(Modifier.height(dims.sm))

        // ── Theme Card ───────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_theme)) {
            val modes = ThemeMode.entries
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md)
            ) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = modes.size
                        ),
                        onClick = {
                            if (state.themeMode != mode) sounds.play(SoundEffect.SELECT)
                            onEvent(SettingsEvent.ThemeModeChanged(mode))
                        },
                        selected = state.themeMode == mode,
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                }
                            )
                        }
                    )
                }
            }
        }

        // ── Display Card ─────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_display)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_mood_label)) },
                supportingContent = { Text(stringResource(R.string.show_mood_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showMood,
                        onCheckedChange = {
                            sounds.play(if (it) SoundEffect.TOGGLE_ON else SoundEffect.TOGGLE_OFF)
                            onEvent(SettingsEvent.ShowMoodToggled(it))
                        }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_energy_label)) },
                supportingContent = { Text(stringResource(R.string.show_energy_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showEnergy,
                        onCheckedChange = {
                            sounds.play(if (it) SoundEffect.TOGGLE_ON else SoundEffect.TOGGLE_OFF)
                            onEvent(SettingsEvent.ShowEnergyToggled(it))
                        }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_libido_label)) },
                supportingContent = { Text(stringResource(R.string.show_libido_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showLibido,
                        onCheckedChange = {
                            sounds.play(if (it) SoundEffect.TOGGLE_ON else SoundEffect.TOGGLE_OFF)
                            onEvent(SettingsEvent.ShowLibidoToggled(it))
                        }
                    )
                }
            )
        }

        // ── Calendar Display Card ────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_calendar_display)) {
            PhaseVisibilitySettings(
                showFollicular = state.showFollicular,
                showOvulation = state.showOvulation,
                showLuteal = state.showLuteal,
                onFollicularToggled = { onEvent(SettingsEvent.ShowFollicularToggled(it)) },
                onOvulationToggled = { onEvent(SettingsEvent.ShowOvulationToggled(it)) },
                onLutealToggled = { onEvent(SettingsEvent.ShowLutealToggled(it)) },
                showTitle = false,
            )
        }

        // ── Insights Display Card ────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_insights_display)) {
            Text(
                stringResource(R.string.settings_top_symptoms, state.topSymptomsCount),
                modifier = Modifier.padding(horizontal = dims.md)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { value ->
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (value == state.topSymptomsCount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (value == state.topSymptomsCount)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                    )
                }
            }
            Slider(
                value = state.topSymptomsCount.toFloat(),
                onValueChange = { newValue ->
                    val rounded = newValue.roundToInt()
                    if (rounded != state.topSymptomsCount) sounds.play(SoundEffect.TICK)
                    onEvent(SettingsEvent.TopSymptomsCountChanged(rounded))
                },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.padding(horizontal = dims.md)
            )
        }

        // ── Cycle Card (issue #143 — values live in the encrypted DB) ──
        SettingsSectionCard(title = stringResource(R.string.settings_section_cycle)) {
            val cycle = state.cycleSettings
            if (cycle == null) {
                Text(
                    stringResource(R.string.settings_cycle_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = dims.md),
                )
            } else {
                Text(
                    text = if (cycle.typicalCycleLengthDays != null) {
                        stringResource(
                            R.string.settings_typical_cycle_length,
                            cycle.typicalCycleLengthDays ?: 0,
                        )
                    } else {
                        stringResource(R.string.settings_typical_cycle_length_unset)
                    },
                    modifier = Modifier.padding(horizontal = dims.md),
                )
                Slider(
                    value = (cycle.typicalCycleLengthDays
                        ?: CycleLengthResolver.DEFAULT_CYCLE_LENGTH_DAYS).toFloat(),
                    onValueChange = {
                        val rounded = it.roundToInt()
                        val current = cycle.typicalCycleLengthDays
                            ?: CycleLengthResolver.DEFAULT_CYCLE_LENGTH_DAYS
                        if (rounded != current) sounds.play(SoundEffect.TICK)
                        onEvent(SettingsEvent.TypicalCycleLengthChanged(rounded))
                    },
                    valueRange = CycleSettings.MIN_CYCLE_LENGTH_DAYS.toFloat()..
                        CycleSettings.MAX_CYCLE_LENGTH_DAYS.toFloat(),
                    steps = CycleSettings.MAX_CYCLE_LENGTH_DAYS -
                        CycleSettings.MIN_CYCLE_LENGTH_DAYS - 1,
                    modifier = Modifier.padding(horizontal = dims.md),
                )
                Text(
                    stringResource(R.string.settings_typical_cycle_length_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = dims.md),
                )

                Spacer(Modifier.height(dims.md))

                Text(
                    stringResource(
                        R.string.settings_default_period_length,
                        cycle.defaultPeriodLengthDays,
                    ),
                    modifier = Modifier.padding(horizontal = dims.md),
                )
                Slider(
                    value = cycle.defaultPeriodLengthDays.toFloat(),
                    onValueChange = {
                        val rounded = it.roundToInt()
                        if (rounded != cycle.defaultPeriodLengthDays) sounds.play(SoundEffect.TICK)
                        onEvent(SettingsEvent.DefaultPeriodLengthChanged(rounded))
                    },
                    valueRange = CycleSettings.MIN_PERIOD_LENGTH_DAYS.toFloat()..
                        CycleSettings.MAX_PERIOD_LENGTH_DAYS.toFloat(),
                    steps = CycleSettings.MAX_PERIOD_LENGTH_DAYS -
                        CycleSettings.MIN_PERIOD_LENGTH_DAYS - 1,
                    modifier = Modifier.padding(horizontal = dims.md),
                )
                Text(
                    stringResource(R.string.settings_default_period_length_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = dims.md),
                )
            }
        }

        // Load the cycle snapshot whenever this page composes (session may have
        // opened/closed since the last visit)
        LaunchedEffect(Unit) {
            onEvent(SettingsEvent.CycleSettingsRequested)
        }

        Spacer(Modifier.height(dims.xl))
    }
}
