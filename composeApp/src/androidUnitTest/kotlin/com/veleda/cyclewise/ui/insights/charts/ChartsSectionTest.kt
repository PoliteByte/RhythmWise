package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.insights.charts.BarChartData
import com.veleda.cyclewise.domain.insights.charts.ChartBar
import com.veleda.cyclewise.domain.insights.charts.ChartPoint
import com.veleda.cyclewise.domain.insights.charts.ChartSeries
import com.veleda.cyclewise.domain.insights.charts.LineChartData
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [ChartsSection].
 *
 * Verifies Compose-level rendering: chart titles, the legend for multi-series
 * line charts, and the absence of a legend for single-series charts. Vico's
 * canvas-drawn axis tick labels are not directly testable as Compose nodes;
 * those are covered by manual QA on an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class ChartsSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chartsSection_WHEN_barChart_THEN_chartTitleDisplayed() {
        val bars = listOf(
            ChartBar("Menstrual", 3.1f),
            ChartBar("Follicular", 3.5f),
            ChartBar("Ovulatory", 4.0f),
            ChartBar("Luteal", 2.8f),
        )
        val chart = BarChartData(
            title = "Mood by Phase",
            key = "chart-mood-by-phase",
            bars = bars,
            xAxisLabel = "Phase",
            yAxisLabel = "Avg Mood",
        )

        setContent(charts = listOf(chart))

        composeTestRule.onNodeWithText("Mood by Phase").assertIsDisplayed()
    }

    @Test
    fun chartsSection_WHEN_multiSeriesLineChart_THEN_bothSeriesLabelsInLegend() {
        val moodSeries = ChartSeries(
            label = "Mood",
            points = listOf(
                ChartPoint(x = 1f, y = 3f, label = "Cycle 1"),
                ChartPoint(x = 2f, y = 3.5f, label = "Cycle 2"),
            ),
        )
        val energySeries = ChartSeries(
            label = "Energy",
            points = listOf(
                ChartPoint(x = 1f, y = 2.5f, label = "Cycle 1"),
                ChartPoint(x = 2f, y = 3f, label = "Cycle 2"),
            ),
        )
        val chart = LineChartData(
            title = "Cycle Comparison",
            key = "chart-cycle-comparison",
            series = listOf(moodSeries, energySeries),
            xAxisLabel = "Cycle",
            yAxisLabel = "Avg Score (1-5)",
        )

        setContent(charts = listOf(chart))

        composeTestRule.onNodeWithText("Cycle Comparison").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mood").assertIsDisplayed()
        composeTestRule.onNodeWithText("Energy").assertIsDisplayed()
    }

    @Test
    fun chartsSection_WHEN_singleSeriesLineChart_THEN_noSeriesLabelInLegend() {
        val series = ChartSeries(
            label = "Length (days)",
            points = listOf(
                ChartPoint(x = 1f, y = 28f, label = "Cycle 1"),
                ChartPoint(x = 2f, y = 29f, label = "Cycle 2"),
                ChartPoint(x = 3f, y = 27f, label = "Cycle 3"),
            ),
        )
        val chart = LineChartData(
            title = "Cycle Length History",
            key = "chart-cycle-length-history",
            series = listOf(series),
            xAxisLabel = "Cycle",
            yAxisLabel = "Days",
        )

        setContent(charts = listOf(chart))

        composeTestRule.onNodeWithText("Cycle Length History").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Length (days)").assertCountEquals(0)
    }

    private fun setContent(
        charts: List<com.veleda.cyclewise.domain.insights.charts.ChartData>,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    ChartsSection(charts = charts)
                }
            }
        }
    }
}
