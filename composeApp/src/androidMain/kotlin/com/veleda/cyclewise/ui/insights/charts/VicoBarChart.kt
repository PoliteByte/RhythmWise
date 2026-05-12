package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.veleda.cyclewise.domain.insights.charts.BarChartData

/**
 * Renders a [BarChartData] model using Vico's column layer.
 *
 * Maps each bar's label onto the X axis, displays axis titles, fixes the
 * Y range for bounded metrics (mood, energy, flow intensity), and shows
 * a marker label with the bar's exact value on tap.
 */
@Composable
internal fun VicoBarChart(
    data: BarChartData,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember(data.key) { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(
                    x = (1..data.bars.size).toList(),
                    y = data.bars.map { it.value.toDouble() },
                )
            }
        }
    }

    val labels = remember(data.key, data.bars) { data.bars.map { it.label } }
    val xFormatter = rememberCategoricalXFormatter(labels)
    val axisTitle = rememberAxisTitleComponent()
    val marker = rememberBarMarker(labels)
    val rangeProvider = rememberBarRangeProvider(data.key)

    CartesianChartHost(
        rememberCartesianChart(
            rememberColumnCartesianLayer(rangeProvider = rangeProvider),
            startAxis = VerticalAxis.rememberStart(
                title = data.yAxisLabel,
                titleComponent = axisTitle,
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = xFormatter,
                title = data.xAxisLabel,
                titleComponent = axisTitle,
                labelRotationDegrees = BOTTOM_AXIS_LABEL_ROTATION,
            ),
            marker = marker,
        ),
        modelProducer,
        modifier = modifier,
    )
}

@Composable
private fun rememberBarRangeProvider(key: String): CartesianLayerRangeProvider =
    remember(key) {
        when (key) {
            KEY_MOOD_BY_PHASE, KEY_ENERGY_BY_PHASE ->
                CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = MOOD_ENERGY_MAX)
            KEY_FLOW_INTENSITY_BY_DAY ->
                CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = FLOW_INTENSITY_MAX)
            else -> CartesianLayerRangeProvider.auto()
        }
    }

private const val KEY_MOOD_BY_PHASE = "chart-mood-by-phase"
private const val KEY_ENERGY_BY_PHASE = "chart-energy-by-phase"
private const val KEY_FLOW_INTENSITY_BY_DAY = "chart-flow-intensity-by-day"
private const val MOOD_ENERGY_MAX = 5.0
private const val FLOW_INTENSITY_MAX = 3.0
private const val BOTTOM_AXIS_LABEL_ROTATION = 30f
