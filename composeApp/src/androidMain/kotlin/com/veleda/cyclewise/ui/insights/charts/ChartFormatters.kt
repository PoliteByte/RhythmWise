package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.component.TextComponent

/**
 * Returns a Vico [CartesianValueFormatter] that maps a (1-based) x value
 * to a text label from [labels].
 *
 * Chart data in `ChartDataGenerator` uses `x = index + 1`, so we subtract 1
 * before indexing. Out-of-range x values fall back to a single space — Vico's
 * `getMaxLabelWidth` probes positions beyond the data range during layout and
 * crashes if the formatter returns an empty string.
 */
@Composable
internal fun rememberCategoricalXFormatter(labels: List<String>): CartesianValueFormatter =
    remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt() - 1
            if (index in labels.indices) labels[index] else " "
        }
    }

/**
 * A [TextComponent] styled for axis titles ("Phase", "Avg Mood", etc.).
 *
 * Uses `onSurfaceVariant` so the title remains legible on the chart card's
 * `surfaceVariant` background in both light and dark themes.
 */
@Composable
internal fun rememberAxisTitleComponent(): TextComponent =
    rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textSize = MaterialTheme.typography.labelSmall.fontSize,
        padding = insets(horizontal = 4.dp, vertical = 2.dp),
    )

/**
 * A marker for bar (column) charts that renders "{label}: {value}" on tap,
 * where label comes from [labels] indexed by the tapped column's x value.
 */
@Composable
internal fun rememberBarMarker(labels: List<String>): CartesianMarker {
    val labelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textSize = MaterialTheme.typography.labelSmall.fontSize,
        padding = insets(horizontal = 6.dp, vertical = 4.dp),
    )
    return rememberDefaultCartesianMarker(
        label = labelComponent,
        valueFormatter = remember(labels) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val target = targets.firstOrNull() as? ColumnCartesianLayerMarkerTarget
                    ?: return@ValueFormatter ""
                val column = target.columns.firstOrNull() ?: return@ValueFormatter ""
                val name = labels.getOrNull(target.x.toInt() - 1).orEmpty()
                val value = formatChartValue(column.entry.y)
                if (name.isEmpty()) value else "$name: $value"
            }
        },
        labelPosition = DefaultCartesianMarker.LabelPosition.AbovePoint,
        indicatorSize = MARKER_INDICATOR_DP.dp,
    )
}

/**
 * A marker for line charts that renders "{seriesName} {xLabel}: {value}"
 * for every target point at the tapped x position.
 */
@Composable
internal fun rememberLineMarker(
    labels: List<String>,
    seriesNames: List<String>,
): CartesianMarker {
    val labelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textSize = MaterialTheme.typography.labelSmall.fontSize,
        padding = insets(horizontal = 6.dp, vertical = 4.dp),
    )
    return rememberDefaultCartesianMarker(
        label = labelComponent,
        valueFormatter = remember(labels, seriesNames) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val target = targets.firstOrNull() as? LineCartesianLayerMarkerTarget
                    ?: return@ValueFormatter ""
                val xLabel = labels.getOrNull(target.x.toInt() - 1).orEmpty()
                target.points.mapIndexed { index, point ->
                    val seriesName = seriesNames.getOrNull(index).orEmpty()
                    val value = formatChartValue(point.entry.y)
                    when {
                        seriesName.isEmpty() && xLabel.isEmpty() -> value
                        seriesName.isEmpty() -> "$xLabel: $value"
                        xLabel.isEmpty() -> "$seriesName: $value"
                        else -> "$seriesName ($xLabel): $value"
                    }
                }.joinToString(separator = "  ")
            }
        },
        labelPosition = DefaultCartesianMarker.LabelPosition.AbovePoint,
        indicatorSize = MARKER_INDICATOR_DP.dp,
    )
}

/**
 * Material 3 colors used to distinguish series in line charts and legends.
 *
 * The same list is consumed by [VicoLineChart] (line fill colors) and the
 * legend dots in [ChartsSection], so the two stay in sync.
 */
@Composable
internal fun lineSeriesColors(count: Int): List<Color> {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
    )
    return List(count) { i -> palette[i % palette.size] }
}

private fun formatChartValue(value: Double): String {
    val rounded = kotlin.math.round(value * VALUE_FORMAT_SCALE) / VALUE_FORMAT_SCALE
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

private const val MARKER_INDICATOR_DP = 8f
private const val VALUE_FORMAT_SCALE = 10.0
