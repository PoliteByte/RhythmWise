package com.veleda.cyclewise.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [ColorPickerDialog] and its pure HSV/hex conversions (issue #150).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class ColorPickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region hsv/hex conversions (pure)

    @Test
    fun `given known colors when converting hsv to hex then rgb matches`() {
        // GIVEN / WHEN / THEN — primary anchors of the HSV wheel
        assertEquals("FF0000", hsvToHex(hue = 0f, saturation = 1f, value = 1f))
        assertEquals("00FF00", hsvToHex(hue = 120f, saturation = 1f, value = 1f))
        assertEquals("0000FF", hsvToHex(hue = 240f, saturation = 1f, value = 1f))
        assertEquals("FFFFFF", hsvToHex(hue = 0f, saturation = 0f, value = 1f))
        assertEquals("000000", hsvToHex(hue = 0f, saturation = 0f, value = 0f))
    }

    @Test
    fun `given valid hex when round-tripping through hsv then value is preserved`() {
        // GIVEN the default phase palette hexes
        listOf("EF9A9A", "80CBC4", "FFCC80", "B39DDB").forEach { hex ->
            // WHEN
            val (h, s, v) = requireNotNull(hexToHsv(hex)) { "$hex should parse" }

            // THEN
            assertEquals(hex, hsvToHex(h, s, v))
        }
    }

    @Test
    fun `given invalid hex when converting to hsv then null is returned`() {
        assertNull(hexToHsv("XYZ"))
        assertNull(hexToHsv(""))
        assertNull(hexToHsv("12345"))
    }

    // endregion

    // region dialog behavior

    private fun setDialog(
        initialHex: String = "EF9A9A",
        onConfirm: (String) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    ColorPickerDialog(
                        label = "Period",
                        initialHex = initialHex,
                        fallbackColor = Color.Red,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }

    @Test
    fun `given dialog shown then title sliders and hex field are displayed`() {
        // GIVEN / WHEN
        setDialog()

        // THEN
        composeTestRule.onNodeWithText("Pick a color for Period").assertIsDisplayed()
        composeTestRule.onNodeWithTag("color-picker-hue").assertIsDisplayed()
        composeTestRule.onNodeWithTag("color-picker-saturation").assertIsDisplayed()
        composeTestRule.onNodeWithTag("color-picker-brightness").assertIsDisplayed()
        composeTestRule.onNodeWithTag("color-picker-hex").assertIsDisplayed()
    }

    @Test
    fun `given valid hex typed when apply tapped then confirm receives the hex`() {
        // GIVEN
        var confirmed: String? = null
        setDialog(onConfirm = { confirmed = it })

        // WHEN — type a new hex and apply
        composeTestRule.onNodeWithTag("color-picker-hex").performTextClearance()
        composeTestRule.onNodeWithTag("color-picker-hex").performTextInput("112233")
        composeTestRule.onNodeWithText("Apply").performClick()

        // THEN
        assertEquals("112233", confirmed)
    }

    @Test
    fun `given invalid hex when typed then apply is disabled`() {
        // GIVEN
        var confirmed: String? = null
        setDialog(onConfirm = { confirmed = it })

        // WHEN — clear to an invalid (empty) hex and try to apply
        composeTestRule.onNodeWithTag("color-picker-hex").performTextClearance()
        composeTestRule.onNodeWithText("Apply").performClick()

        // THEN — nothing confirmed
        assertNull(confirmed)
    }

    @Test
    fun `given cancel tapped then dismiss fires and confirm does not`() {
        // GIVEN
        var confirmed: String? = null
        var dismissed = false
        setDialog(onConfirm = { confirmed = it }, onDismiss = { dismissed = true })

        // WHEN
        composeTestRule.onNodeWithText("Cancel").performClick()

        // THEN
        assertTrue(dismissed)
        assertNull(confirmed)
    }

    // endregion
}
