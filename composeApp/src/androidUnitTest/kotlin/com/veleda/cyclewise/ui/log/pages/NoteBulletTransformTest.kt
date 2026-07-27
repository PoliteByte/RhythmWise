package com.veleda.cyclewise.ui.log.pages

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Tests for [continueBulletList] — the pure bullet-continuation transform used
 * by the note editor (issue #152). Given-When-Then per test.
 */
class NoteBulletTransformTest {

    /** Simulates pressing return at [cursor] in [text], producing the proposed value. */
    private fun returnPressed(text: String, cursor: Int): Pair<TextFieldValue, TextFieldValue> {
        val previous = TextFieldValue(text, TextRange(cursor))
        val proposed = TextFieldValue(
            text.substring(0, cursor) + "\n" + text.substring(cursor),
            TextRange(cursor + 1),
        )
        return previous to proposed
    }

    @Test
    fun `given bullet line with content when return pressed then next line continues the bullet`() {
        // Given a note ending in a dash bullet with content
        val (previous, proposed) = returnPressed("- milk", cursor = 6)

        // When
        val result = continueBulletList(previous, proposed)

        // Then the new line starts with the same marker and the cursor sits after it
        assertEquals("- milk\n- ", result.text)
        assertEquals(TextRange(9), result.selection)
    }

    @Test
    fun `given empty bullet when return pressed then bullet is removed and list exits`() {
        // Given the user just typed a bullet marker but no content
        val (previous, proposed) = returnPressed("- milk\n- ", cursor = 9)

        // When
        val result = continueBulletList(previous, proposed)

        // Then the empty bullet line and the typed newline are gone
        assertEquals("- milk\n", result.text)
        assertEquals(TextRange(7), result.selection)
    }

    @Test
    fun `given return pressed mid bullet line then remainder moves to a new bullet`() {
        // Given the cursor in the middle of a bullet line: "- ab|cd"
        val (previous, proposed) = returnPressed("- abcd", cursor = 4)

        // When
        val result = continueBulletList(previous, proposed)

        // Then the split-off remainder gets its own bullet
        assertEquals("- ab\n- cd", result.text)
        assertEquals(TextRange(7), result.selection)
    }

    @Test
    fun `given non-bullet line when return pressed then text is unchanged`() {
        // Given
        val (previous, proposed) = returnPressed("plain text", cursor = 10)

        // When
        val result = continueBulletList(previous, proposed)

        // Then the proposed value passes through untouched
        assertSame(proposed, result)
    }

    @Test
    fun `given dot and star markers when return pressed then marker style is preserved`() {
        // Given a • bullet
        val (prevDot, propDot) = returnPressed("• water", cursor = 7)
        // When / Then
        assertEquals("• water\n• ", continueBulletList(prevDot, propDot).text)

        // Given a * bullet
        val (prevStar, propStar) = returnPressed("* tea", cursor = 5)
        // When / Then
        assertEquals("* tea\n* ", continueBulletList(prevStar, propStar).text)
    }

    @Test
    fun `given indented bullet when return pressed then indentation is preserved`() {
        // Given a nested bullet
        val (previous, proposed) = returnPressed("  - nested", cursor = 10)

        // When
        val result = continueBulletList(previous, proposed)

        // Then the continuation keeps the two-space indent
        assertEquals("  - nested\n  - ", result.text)
        assertEquals(TextRange(15), result.selection)
    }

    @Test
    fun `given multi-character paste when text changes then transform does not fire`() {
        // Given a paste of a newline-containing chunk
        val previous = TextFieldValue("- milk", TextRange(6))
        val proposed = TextFieldValue("- milk\nabc", TextRange(10))

        // When
        val result = continueBulletList(previous, proposed)

        // Then the pasted content is untouched
        assertSame(proposed, result)
    }

    @Test
    fun `given selection replaced by newline when transformed then text is unchanged`() {
        // Given "- milk and" with "and" selected, replaced by return (net length change != 1)
        val previous = TextFieldValue("- milk and", TextRange(7, 10))
        val proposed = TextFieldValue("- milk \n", TextRange(8))

        // When
        val result = continueBulletList(previous, proposed)

        // Then no bullet logic applies
        assertSame(proposed, result)
    }

    @Test
    fun `given bullet on a later line when return pressed then that bullet continues`() {
        // Given a note whose second line is the bullet
        val text = "header\n- second"
        val (previous, proposed) = returnPressed(text, cursor = text.length)

        // When
        val result = continueBulletList(previous, proposed)

        // Then
        assertEquals("header\n- second\n- ", result.text)
        assertEquals(TextRange(18), result.selection)
    }

    @Test
    fun `given plain deletion when text shrinks then transform does not fire`() {
        // Given a backspace over a newline
        val previous = TextFieldValue("- a\n- b", TextRange(7))
        val proposed = TextFieldValue("- a\n- ", TextRange(6))

        // When
        val result = continueBulletList(previous, proposed)

        // Then untouched
        assertSame(proposed, result)
    }
}
