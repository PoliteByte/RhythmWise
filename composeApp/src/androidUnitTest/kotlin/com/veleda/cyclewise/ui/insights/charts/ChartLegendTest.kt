package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [ChartLegend].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class ChartLegendTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chartLegend_WHEN_twoEntries_THEN_bothLabelsDisplayed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    ChartLegend(
                        entries = listOf(
                            LegendEntry("Mood", Color.Red),
                            LegendEntry("Energy", Color.Blue),
                        ),
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Mood").assertIsDisplayed()
        composeTestRule.onNodeWithText("Energy").assertIsDisplayed()
    }

    @Test
    fun chartLegend_WHEN_emptyEntries_THEN_rendersNothing() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    ChartLegend(entries = emptyList())
                }
            }
        }

        composeTestRule.onNodeWithText("Mood").assertDoesNotExist()
    }
}
