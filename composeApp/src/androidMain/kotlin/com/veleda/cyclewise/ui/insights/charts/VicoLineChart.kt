package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.veleda.cyclewise.domain.insights.charts.LineChartData
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Renders a [LineChartData] model using Vico.
 *
 * Maps each point's label onto the X axis, displays axis titles, fixes the
 * Y range for bounded metrics, colors each series from [colors], and shows
 * a marker label with the tapped point's exact value(s).
 *
 * @param data     The chart data to render.
 * @param colors   Color per series, indexed parallel to [LineChartData.series].
 *                 Must contain at least `data.series.size` entries.
 * @param modifier Modifier applied to the chart host.
 */
@Composable
internal fun VicoLineChart(
    data: LineChartData,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember(data.key) { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                data.series.forEach { chartSeries ->
                    series(
                        x = chartSeries.points.map { it.x.toDouble() },
                        y = chartSeries.points.map { it.y.toDouble() },
                    )
                }
            }
        }
    }

    val labels = remember(data.key, data.series) { extractXLabels(data) }
    val seriesNames = remember(data.key, data.series) { data.series.map { it.label } }
    val xFormatter = rememberCategoricalXFormatter(labels)
    val axisTitle = rememberAxisTitleComponent()
    val marker = rememberLineMarker(labels, seriesNames)
    val rangeProvider = rememberLineRangeProvider(data.key)

    val lines = data.series.mapIndexed { index, _ ->
        val color = colors.getOrElse(index) { colors.last() }
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill(color)),
        )
    }
    val lineProvider = remember(lines) { LineCartesianLayer.LineProvider.series(lines) }

    CartesianChartHost(
        rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = lineProvider,
                rangeProvider = rangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(
                title = data.yAxisLabel,
                titleComponent = axisTitle,
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = xFormatter,
                title = data.xAxisLabel,
                titleComponent = axisTitle,
            ),
            marker = marker,
        ),
        modelProducer,
        modifier = modifier,
    )
}

private fun extractXLabels(data: LineChartData): List<String> {
    val firstSeriesLabels = data.series.firstOrNull()?.points?.map { it.label.orEmpty() }
        ?: return emptyList()
    return if (firstSeriesLabels.any { it.isBlank() }) {
        firstSeriesLabels.mapIndexed { i, l -> if (l.isBlank()) "Cycle ${i + 1}" else l }
    } else {
        firstSeriesLabels
    }
}

@Composable
private fun rememberLineRangeProvider(key: String): CartesianLayerRangeProvider =
    remember(key) {
        when (key) {
            KEY_CYCLE_COMPARISON ->
                CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = MOOD_ENERGY_MAX)
            KEY_CYCLE_LENGTH_HISTORY ->
                FitDataYRangeProvider(yPadding = CYCLE_LENGTH_Y_PADDING)
            else -> CartesianLayerRangeProvider.auto()
        }
    }

/**
 * Y-axis range that fits the data with padding instead of anchoring to zero.
 *
 * Used by [KEY_CYCLE_LENGTH_HISTORY] so the line uses the full chart height
 * (cycle lengths cluster in the 21–35 day range — anchoring at 0 wastes
 * three-quarters of the chart).
 */
private class FitDataYRangeProvider(private val yPadding: Double) : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        floor(minY - yPadding)

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        ceil(maxY + yPadding)
}

private const val KEY_CYCLE_COMPARISON = "chart-cycle-comparison"
private const val KEY_CYCLE_LENGTH_HISTORY = "chart-cycle-length-history"
private const val MOOD_ENERGY_MAX = 5.0
private const val CYCLE_LENGTH_Y_PADDING = 1.0
