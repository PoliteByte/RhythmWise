package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * One entry in a [ChartLegend].
 *
 * @property label Series name (e.g., "Mood", "Energy").
 * @property color Dot color matching the rendered series line.
 */
internal data class LegendEntry(val label: String, val color: Color)

/**
 * Horizontal legend row of colored dots paired with series labels.
 *
 * Used under multi-series line charts so users can identify which color
 * corresponds to which data series. Scrolls horizontally if entries overflow.
 */
@Composable
internal fun ChartLegend(
    entries: List<LegendEntry>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return

    val dims = LocalDimensions.current
    val legendCd = stringResource(R.string.insights_chart_legend_cd)

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = legendCd },
        horizontalArrangement = Arrangement.spacedBy(dims.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEach { entry ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(dims.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(LEGEND_DOT_SIZE)
                        .clip(CircleShape)
                        .background(entry.color),
                )
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val LEGEND_DOT_SIZE = 8.dp
