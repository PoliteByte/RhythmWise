package com.veleda.cyclewise.ui.tracker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.ui.components.flowIntensityLabel
import com.veleda.cyclewise.ui.components.moodFaceIcon
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import kotlinx.datetime.LocalDate

/**
 * Bottom-sheet content summarising a single day's log.
 *
 * Displays logged data in [InfoCard] rows with meaningful icons
 * (phase, flow, color, consistency, mood, energy, libido, water, notes),
 * symptom/medication chips, and a "View Full Log" button at the bottom.
 *
 * @param log               The full daily log to display.
 * @param periodId          Associated period ID, or null if the day is not a period day.
 * @param cyclePhase        Computed cycle phase for this date, or null if not determinable.
 * @param symptomLibrary    Library of all symptoms for name resolution.
 * @param medicationLibrary Library of all medications for name resolution.
 * @param customTagLibrary  Library of all custom tags for name resolution.
 * @param waterCups         Number of water cups logged, or null.
 * @param showMood          Whether to display the mood score row (controlled by user setting).
 * @param showEnergy        Whether to display the energy level row (controlled by user setting).
 * @param showLibido        Whether to display the libido score row (controlled by user setting).
 * @param onEditClick       Callback when the user taps the edit button.
 * @param onDeleteClick     Callback when the user taps the delete button.
 * @param onViewFullLogClick Callback when the user taps the "View Full Log" button.
 */
// Linear null-guarded InfoCard rows for a single cohesive summary sheet
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun LogSummarySheetContent(
    log: FullDailyLog,
    periodId: String?,
    cyclePhase: CyclePhase? = null,
    symptomLibrary: List<Symptom>,
    medicationLibrary: List<Medication>,
    customTagLibrary: List<CustomTag>,
    waterCups: Int?,
    showMood: Boolean,
    showEnergy: Boolean,
    showLibido: Boolean,
    onEditClick: (LocalDate) -> Unit,
    onDeleteClick: (String) -> Unit,
    onViewFullLogClick: (LocalDate) -> Unit
) {
    val dims = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tracker_log_for, log.entry.entryDate.toLocalizedDateString()),
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                IconButton(
                    onClick = { onEditClick(log.entry.entryDate) },
                    modifier = Modifier.testTag("edit-log-button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.tracker_edit_log))
                }
                if (periodId != null) {
                    IconButton(
                        onClick = { onDeleteClick(periodId) },
                        modifier = Modifier.testTag("delete-period-button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.tracker_delete_period))
                    }
                }
            }
        }

        HorizontalDivider()

        cyclePhase?.let { phase ->
            InfoCard(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.tracker_phase_label),
                value = when (phase) {
                    CyclePhase.MENSTRUATION -> stringResource(R.string.phase_color_period_label)
                    CyclePhase.FOLLICULAR -> stringResource(R.string.phase_color_follicular_label)
                    CyclePhase.OVULATION -> stringResource(R.string.phase_color_ovulation_label)
                    CyclePhase.LUTEAL -> stringResource(R.string.phase_color_luteal_label)
                }
            )
        }

        log.periodLog?.flowIntensity?.let {
            InfoCard(
                icon = Icons.Default.Opacity,
                title = stringResource(R.string.tracker_flow_label),
                value = flowIntensityLabel(it)
            )
        }

        log.periodLog?.periodColor?.let {
            InfoCard(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.tracker_color_label),
                value = when (it) {
                    PeriodColor.PINK -> stringResource(R.string.period_color_pink)
                    PeriodColor.BRIGHT_RED -> stringResource(R.string.period_color_bright_red)
                    PeriodColor.DARK_RED -> stringResource(R.string.period_color_dark_red)
                    PeriodColor.BROWN -> stringResource(R.string.period_color_brown)
                    PeriodColor.BLACK_OR_VERY_DARK -> stringResource(R.string.period_color_black)
                    PeriodColor.UNUSUAL_COLOR -> stringResource(R.string.period_color_unusual)
                }
            )
        }

        log.periodLog?.periodConsistency?.let {
            InfoCard(
                icon = Icons.Default.Grain,
                title = stringResource(R.string.tracker_consistency_label),
                value = when (it) {
                    PeriodConsistency.THIN -> stringResource(R.string.period_consistency_thin)
                    PeriodConsistency.MODERATE -> stringResource(R.string.period_consistency_moderate)
                    PeriodConsistency.THICK -> stringResource(R.string.period_consistency_thick)
                    PeriodConsistency.STRINGY -> stringResource(R.string.period_consistency_stringy)
                    PeriodConsistency.CLOTS_SMALL -> stringResource(R.string.period_consistency_clots_small)
                    PeriodConsistency.CLOTS_LARGE -> stringResource(R.string.period_consistency_clots_large)
                }
            )
        }

        if (showMood) {
            log.entry.moodScore?.let { score ->
                InfoCard(
                    icon = Icons.Default.Mood,
                    title = stringResource(R.string.tracker_mood_label),
                ) {
                    MoodFaceValue(score = score)
                }
            }
        }

        if (showEnergy) {
            log.entry.energyLevel?.let { score ->
                InfoCard(
                    icon = Icons.Default.Bolt,
                    title = stringResource(R.string.tracker_energy_label),
                ) {
                    IconScale(
                        score = score,
                        filledIcon = Icons.Default.Bolt,
                        emptyIcon = Icons.Outlined.Bolt,
                    )
                }
            }
        }

        if (showLibido) {
            log.entry.libidoScore?.let { score ->
                InfoCard(
                    icon = Icons.Default.FavoriteBorder,
                    title = stringResource(R.string.tracker_libido_label),
                ) {
                    IconScale(
                        score = score,
                        filledIcon = Icons.Default.Favorite,
                        emptyIcon = Icons.Default.FavoriteBorder,
                    )
                }
            }
        }

        waterCups?.let {
            if (it > 0) {
                InfoCard(
                    icon = Icons.Default.WaterDrop,
                    title = stringResource(R.string.tracker_water_label),
                    value = stringResource(R.string.tracker_water_cups, it)
                )
            }
        }

        if (!log.entry.note.isNullOrBlank()) {
            InfoCard(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = stringResource(R.string.tracker_notes_label),
                value = log.entry.note!!
            )
        }

        ChipRowSection(
            titleResId = R.string.tracker_symptoms_label,
            items = log.symptomLogs,
            key = { it.symptomId },
            resolveName = { symptomLog ->
                symptomLibrary.find { it.id == symptomLog.symptomId }?.name
            },
        )

        ChipRowSection(
            titleResId = R.string.tracker_medications_label,
            items = log.medicationLogs,
            key = { it.medicationId },
            resolveName = { medicationLog ->
                medicationLibrary.find { it.id == medicationLog.medicationId }?.name
            },
        )

        ChipRowSection(
            titleResId = R.string.tracker_custom_tags_label,
            items = log.customTagLogs,
            key = { it.tagId },
            resolveName = { tagLog ->
                customTagLibrary.find { it.id == tagLog.tagId }?.name
            },
        )

        FilledTonalButton(
            onClick = { onViewFullLogClick(log.entry.entryDate) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.tracker_view_full_log))
        }

        Spacer(Modifier.height(dims.md))
    }
}

/**
 * A card with a titled [LazyRow] of [SuggestionChip]s, used for symptom, medication,
 * and custom tag summaries in the log sheet.
 *
 * @param T           The type of log entry items (e.g. SymptomLog, MedicationLog).
 * @param titleResId  String resource ID for the section title.
 * @param items       The log entries to display as chips.
 * @param key         Key selector for [LazyRow] item identity.
 * @param resolveName Resolves a display name from a log entry, or `null` if unresolvable.
 */
@Composable
private fun <T> ChipRowSection(
    @StringRes titleResId: Int,
    items: List<T>,
    key: (T) -> Any,
    resolveName: (T) -> String?,
) {
    if (items.isEmpty()) return

    val dims = LocalDimensions.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(dims.sm)) {
            Text(
                stringResource(titleResId),
                style = MaterialTheme.typography.titleMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(dims.sm),
                modifier = Modifier.padding(top = dims.xs)
            ) {
                items(items, key = key) { item ->
                    val name = resolveName(item)
                    if (name != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(name) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A card-styled info row with a leading icon, title, and trailing value.
 *
 * Used inside [LogSummarySheetContent] for each scalar data field
 * (phase, flow, color, consistency, mood, energy, libido, water, notes).
 *
 * @param icon  The leading [ImageVector] icon.
 * @param title The label text displayed after the icon.
 * @param value The data value displayed at the trailing edge.
 */
@Composable
private fun InfoCard(icon: ImageVector, title: String, value: String) {
    InfoCard(icon = icon, title = title) {
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * [InfoCard] variant whose value slot is arbitrary composable [content] —
 * used for the visual score scales that replaced the bare "n / 5" text
 * (issue #146).
 */
@Composable
private fun InfoCard(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    val dims = LocalDimensions.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Text(text = "$title:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            content()
        }
    }
}

/**
 * Mood value rendered as an expressive sentiment face plus the numeric score —
 * beta testers preferred a glanceable face over bare "n / 5" (issue #146).
 */
@Composable
private fun MoodFaceValue(score: Int) {
    val dims = LocalDimensions.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.xs),
    ) {
        Icon(
            imageVector = moodFaceIcon(score),
            contentDescription = stringResource(R.string.score_of_five, score),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(dims.lg),
        )
        Text(text = "$score / 5", style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Five-step icon scale matching the wellness input selectors' single-highlight
 * model (Daniel's review: no fill-up anywhere): only the position equal to
 * [score] renders [filledIcon] in the primary color at full opacity; the rest
 * render [emptyIcon] faded. The row carries a "N of 5" content description so
 * screen readers announce the value once.
 */
@Composable
private fun IconScale(
    score: Int,
    filledIcon: ImageVector,
    emptyIcon: ImageVector,
) {
    val dims = LocalDimensions.current
    val description = stringResource(R.string.score_of_five, score)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
    ) {
        repeat(5) { index ->
            val isSelected = index + 1 == score
            Icon(
                imageVector = if (isSelected) filledIcon else emptyIcon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(dims.lg)
                    .alpha(if (isSelected) 1f else UNSELECTED_SCALE_ALPHA),
            )
        }
    }
}

/** Alpha for non-selected positions in [IconScale] — mirrors the input selectors. */
private const val UNSELECTED_SCALE_ALPHA = 0.35f
