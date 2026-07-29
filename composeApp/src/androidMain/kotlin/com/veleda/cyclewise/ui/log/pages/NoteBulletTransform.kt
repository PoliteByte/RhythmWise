package com.veleda.cyclewise.ui.log.pages

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Matches a bullet line: optional indent, a `-`/`•`/`*` marker with trailing space(s), then content. */
private val BULLET_LINE = Regex("""^(\s*)([-•*][ \t]+)(.*)$""")

/**
 * Continues or terminates a bullet list when the user presses return inside the
 * note editor.
 *
 * Given the field state [previous] and the [proposed] state produced by the
 * keystroke, detects a single newline inserted at a collapsed cursor:
 * - If the line before the newline is a bullet line **with content**
 *   (e.g. `"- milk"`), the same indent + marker is inserted after the newline so
 *   the list continues, with the cursor placed after the new marker.
 * - If that line is an **empty bullet** (e.g. `"- "`), the marker line and the
 *   typed newline are removed — pressing return on an empty bullet exits the
 *   list, matching standard notes-app behavior.
 * - Any other change (plain typing, pastes, deletions, selection replacements,
 *   newlines after non-bullet lines) is returned unchanged.
 *
 * Pure function — no state, no IME interaction — so the behavior is unit-testable
 * independently of Compose (issue #152).
 */
internal fun continueBulletList(
    previous: TextFieldValue,
    proposed: TextFieldValue,
): TextFieldValue {
    // Only a single inserted character at a collapsed cursor can be "the return key"
    if (proposed.text.length - previous.text.length != 1) return proposed
    if (!proposed.selection.collapsed) return proposed
    val cursor = proposed.selection.start
    if (cursor < 1 || proposed.text[cursor - 1] != '\n') return proposed
    // Pure insertion check: everything around the new char is unchanged
    if (previous.text.substring(0, cursor - 1) != proposed.text.substring(0, cursor - 1)) return proposed
    if (previous.text.substring(cursor - 1) != proposed.text.substring(cursor)) return proposed

    val lineStart = proposed.text.lastIndexOf('\n', cursor - 2).let { if (it == -1) 0 else it + 1 }
    val lineBefore = proposed.text.substring(lineStart, cursor - 1)
    val match = BULLET_LINE.matchEntire(lineBefore) ?: return proposed
    val (indent, marker, content) = match.destructured

    return if (content.isBlank()) {
        // Return on an empty bullet: drop the marker line and the typed newline
        TextFieldValue(
            text = proposed.text.removeRange(lineStart, cursor),
            selection = TextRange(lineStart),
        )
    } else {
        val prefix = indent + marker
        TextFieldValue(
            text = buildString {
                append(proposed.text, 0, cursor)
                append(prefix)
                append(proposed.text, cursor, proposed.text.length)
            },
            selection = TextRange(cursor + prefix.length),
        )
    }
}
