package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Alpha applied to the leading-edge accent bar. Full-saturation bars read as
 * data ("what is that brighter pink?" — beta feedback, issue #153); muting them
 * makes the accent read as decorative structure.
 */
private const val ACCENT_BAR_ALPHA = 0.45f

/**
 * Shared card wrapper with a colored leading-edge accent for visual differentiation.
 *
 * All insight cards use this wrapper for consistent styling: medium-rounded card shape,
 * `surfaceVariant` background, and a 4.dp accent bar drawn on the leading edge (left in
 * LTR layouts, right in RTL layouts).
 *
 * The accent is **decorative grouping, not data**: it is rendered at
 * [ACCENT_BAR_ALPHA] so it can't be mistaken for a level or heat indicator
 * (issue #153).
 *
 * @param accentColor Color for the leading-edge accent (muted before drawing).
 * @param modifier    Modifier applied to the outer [Card].
 * @param content     Card body content.
 */
@Composable
internal fun AccentedInsightCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dims = LocalDimensions.current
    val accentWidth = dims.xs
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val barX = if (isRtl) size.width - accentWidth.toPx() else 0f
                drawRect(
                    color = accentColor.copy(alpha = ACCENT_BAR_ALPHA),
                    topLeft = Offset(barX, 0f),
                    size = Size(accentWidth.toPx(), size.height)
                )
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(
                start = dims.md + dims.sm + dims.xs,
                top = dims.md,
                end = dims.md,
                bottom = dims.md
            ),
            verticalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            content()
        }
    }
}
