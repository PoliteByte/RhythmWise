package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import com.veleda.cyclewise.ui.tracker.parseHexColor

/** Material 200-shade presets for quick color selection. */
internal val PRESET_COLORS = listOf(
    "EF9A9A", // Red 200 (default menstruation)
    "F48FB1", // Pink 200
    "CE93D8", // Purple 200
    "B39DDB", // Deep Purple 200 (default luteal)
    "90CAF9", // Blue 200
    "80CBC4", // Teal 200 (default follicular)
    "A5D6A7", // Green 200
    "E6EE9C", // Lime 200
    "FFCC80", // Orange 200 (default ovulation)
    "BCAAA4", // Brown 200
)

/**
 * Composable that renders four color-editing rows (one per cycle phase),
 * each with a preset color grid, plus a "Reset to Defaults" button.
 *
 * Each row shows a colored preview swatch, the phase label, and the current hex
 * value; tapping it opens a [ColorPickerDialog] with hue/saturation/brightness
 * sliders (hex entry lives inside the dialog — issue #150). Below each row sits
 * a horizontally scrollable preset color grid for one-tap picks.
 *
 * Accepts state values and change callbacks instead of [AppSettings] directly,
 * wiring through the [SettingsViewModel].
 *
 * @param menstruationHex            Current 6-char hex color for the Menstruation phase.
 * @param follicularHex              Current 6-char hex color for the Follicular phase.
 * @param ovulationHex               Current 6-char hex color for the Ovulation phase.
 * @param lutealHex                  Current 6-char hex color for the Luteal phase.
 * @param onMenstruationColorChanged Callback invoked with the sanitised hex when the user edits the Menstruation color.
 * @param onFollicularColorChanged   Callback invoked with the sanitised hex when the user edits the Follicular color.
 * @param onOvulationColorChanged    Callback invoked with the sanitised hex when the user edits the Ovulation color.
 * @param onLutealColorChanged       Callback invoked with the sanitised hex when the user edits the Luteal color.
 * @param onResetDefaults            Callback invoked when the user taps "Reset to Defaults".
 * @param showTitle                  When `true` (default), renders a [titleMedium] header above the rows.
 *                                   Set to `false` when embedded inside a parent card that already provides a title.
 */
@Composable
fun PhaseColorSettings(
    menstruationHex: String,
    follicularHex: String,
    ovulationHex: String,
    lutealHex: String,
    onMenstruationColorChanged: (String) -> Unit,
    onFollicularColorChanged: (String) -> Unit,
    onOvulationColorChanged: (String) -> Unit,
    onLutealColorChanged: (String) -> Unit,
    onResetDefaults: () -> Unit,
    showTitle: Boolean = true,
) {
    val dims = LocalDimensions.current

    Column {
        if (showTitle) {
            Text(
                stringResource(R.string.phase_colors_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(dims.sm))
        }

        PhaseColorRow(
            label = stringResource(R.string.phase_color_period_label),
            hexValue = menstruationHex,
            defaultColor = CyclePhaseColors.Menstruation,
            onValueChange = onMenstruationColorChanged
        )
        PresetColorGrid(
            selectedHex = menstruationHex,
            onSelect = onMenstruationColorChanged
        )

        Spacer(Modifier.height(dims.sm))

        PhaseColorRow(
            label = stringResource(R.string.phase_color_follicular_label),
            hexValue = follicularHex,
            defaultColor = CyclePhaseColors.Follicular,
            onValueChange = onFollicularColorChanged
        )
        PresetColorGrid(
            selectedHex = follicularHex,
            onSelect = onFollicularColorChanged
        )

        Spacer(Modifier.height(dims.sm))

        PhaseColorRow(
            label = stringResource(R.string.phase_color_ovulation_label),
            hexValue = ovulationHex,
            defaultColor = CyclePhaseColors.Ovulation,
            onValueChange = onOvulationColorChanged
        )
        PresetColorGrid(
            selectedHex = ovulationHex,
            onSelect = onOvulationColorChanged
        )

        Spacer(Modifier.height(dims.sm))

        PhaseColorRow(
            label = stringResource(R.string.phase_color_luteal_label),
            hexValue = lutealHex,
            defaultColor = CyclePhaseColors.Luteal,
            onValueChange = onLutealColorChanged
        )
        PresetColorGrid(
            selectedHex = lutealHex,
            onSelect = onLutealColorChanged
        )

        Spacer(Modifier.height(dims.sm))
        FilledTonalButton(
            onClick = onResetDefaults,
            modifier = Modifier.padding(horizontal = dims.md)
        ) {
            Text(stringResource(R.string.phase_color_reset_defaults))
        }
    }
}

internal val HEX_FILTER = Regex("[^0-9A-Fa-f]")

/**
 * A single row for editing one phase's color.
 *
 * Shows a 24 dp colored preview swatch, the phase label, and the current hex
 * value. Tapping the row opens a [ColorPickerDialog] with hue/saturation/
 * brightness sliders — the raw hex field lives inside the dialog as the
 * advanced affordance (issue #150).
 *
 * @param label        Phase display name.
 * @param hexValue     Current persisted 6-char hex string.
 * @param defaultColor Fallback [Color] used when [hexValue] is invalid.
 * @param onValueChange Callback invoked with the chosen hex when the picker is applied.
 */
@Composable
internal fun PhaseColorRow(
    label: String,
    hexValue: String,
    defaultColor: Color,
    onValueChange: (String) -> Unit
) {
    val dims = LocalDimensions.current
    val parsedColor = parseHexColor(hexValue)
    var showPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.sm),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.color_row_edit_cd, label),
            ) { showPicker = true }
            .padding(horizontal = dims.md, vertical = dims.xs)
    ) {
        Box(
            modifier = Modifier
                .size(dims.lg)
                .clip(CircleShape)
                .background(parsedColor ?: defaultColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = hexValue,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dims.md),
        )
    }

    if (showPicker) {
        ColorPickerDialog(
            label = label,
            initialHex = hexValue,
            fallbackColor = defaultColor,
            onConfirm = { hex ->
                showPicker = false
                onValueChange(hex)
            },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * Horizontally scrollable row of 32 dp colored circle swatches from [PRESET_COLORS].
 *
 * Tapping a swatch invokes [onSelect] with its hex value. The currently selected
 * preset receives a 2 dp `primary`-colored border ring.
 *
 * @param selectedHex The currently active hex value (used to highlight the matching swatch).
 * @param onSelect    Callback invoked with the hex string of the tapped preset.
 */
@Composable
internal fun PresetColorGrid(
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    val dims = LocalDimensions.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .padding(horizontal = dims.md)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dims.sm)
    ) {
        PRESET_COLORS.forEach { hex ->
            val color = parseHexColor(hex) ?: return@forEach
            val isSelected = hex.equals(selectedHex, ignoreCase = true)
            val desc = stringResource(R.string.phase_color_preset_content_description, hex)

            Box(
                modifier = Modifier
                    .size(dims.xl)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(dims.xxs, primaryColor, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(hex) }
                    .semantics { contentDescription = desc }
            )
        }
    }
}
