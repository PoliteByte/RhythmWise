package com.veleda.cyclewise.ui.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.tracker.parseHexColor

/**
 * Converts HSV components to a 6-character uppercase RGB hex string (no `#`).
 *
 * @param hue        0..360
 * @param saturation 0..1
 * @param value      0..1
 */
internal fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
    val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    return String.format("%06X", argb and 0xFFFFFF)
}

/**
 * Converts a 6-character RGB hex string to HSV components (hue 0..360,
 * saturation 0..1, value 0..1), or null when the hex is invalid.
 */
internal fun hexToHsv(hex: String): Triple<Float, Float, Float>? {
    val color = parseHexColor(hex) ?: return null
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

/**
 * Visual color picker dialog: hue / saturation / brightness sliders with a live
 * preview, plus the hex field as the advanced affordance (issue #150 — "no user
 * will know the hex code of the color they want to pick").
 *
 * Editing the hex field moves the sliders when the value parses; moving a
 * slider rewrites the hex field. [onConfirm] receives the final 6-character
 * uppercase hex only when Apply is tapped.
 *
 * @param label        Display name of the color being edited (dialog title).
 * @param initialHex   Current persisted hex; falls back to [fallbackColor] when invalid.
 * @param fallbackColor Color used to seed the sliders when [initialHex] is invalid.
 * @param onConfirm    Called with the chosen hex on Apply.
 * @param onDismiss    Called on Cancel or outside-tap.
 */
// Dialog layout: preview + hex + three sliders — cohesive, splitting adds indirection
@Suppress("LongMethod")
@Composable
internal fun ColorPickerDialog(
    label: String,
    initialHex: String,
    fallbackColor: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dims = LocalDimensions.current

    val initialHsv = remember {
        hexToHsv(initialHex) ?: run {
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(fallbackColor.toArgb(), hsv)
            Triple(hsv[0], hsv[1], hsv[2])
        }
    }
    var hue by remember { mutableFloatStateOf(initialHsv.first) }
    var saturation by remember { mutableFloatStateOf(initialHsv.second) }
    var brightness by remember { mutableFloatStateOf(initialHsv.third) }
    var hexText by remember { mutableStateOf(hsvToHex(initialHsv.first, initialHsv.second, initialHsv.third)) }

    fun onSliderChanged() {
        hexText = hsvToHex(hue, saturation, brightness)
    }

    val previewColor = parseHexColor(hexText) ?: fallbackColor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_picker_title, label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.md),
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(dims.xl + dims.md)
                            .clip(CircleShape)
                            .background(previewColor)
                            .testTag("color-picker-preview"),
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { raw ->
                            val filtered = raw.replace(HEX_FILTER, "").take(6).uppercase()
                            hexText = filtered
                            hexToHsv(filtered)?.let { (h, s, v) ->
                                hue = h
                                saturation = s
                                brightness = v
                            }
                        },
                        label = { Text(stringResource(R.string.phase_color_hex_hint)) },
                        isError = parseHexColor(hexText) == null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("color-picker-hex"),
                    )
                }

                Text(stringResource(R.string.color_picker_hue), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hue,
                    onValueChange = { hue = it; onSliderChanged() },
                    valueRange = 0f..360f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dims.lg)
                        .testTag("color-picker-hue"),
                )

                Text(stringResource(R.string.color_picker_saturation), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it; onSliderChanged() },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dims.lg)
                        .testTag("color-picker-saturation"),
                )

                Text(stringResource(R.string.color_picker_brightness), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it; onSliderChanged() },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dims.lg)
                        .testTag("color-picker-brightness"),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parseHexColor(hexText) != null,
                onClick = { onConfirm(hexText) },
            ) {
                Text(stringResource(R.string.color_picker_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.color_picker_cancel))
            }
        },
    )
}
