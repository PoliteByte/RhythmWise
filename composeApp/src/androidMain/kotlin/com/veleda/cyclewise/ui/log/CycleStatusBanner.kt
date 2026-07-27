package com.veleda.cyclewise.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.usecases.CycleStatus

/**
 * Glanceable cycle-status banner shown at the top of the home screen
 * (issue #142) — the tester's most-wanted fact: "what day of period you're on
 * or when your next period will be". Tapping navigates to the Tracker.
 */
@Composable
internal fun CycleStatusBanner(
    status: CycleStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = com.veleda.cyclewise.ui.theme.LocalDimensions.current

    val headline = when (status) {
        is CycleStatus.OnPeriod ->
            stringResource(R.string.cycle_status_on_period, status.dayOfPeriod)
        is CycleStatus.Predicted ->
            if (status.daysAway > 0) {
                pluralStringResource(
                    R.plurals.cycle_status_predicted,
                    status.daysAway,
                    status.daysAway,
                )
            } else {
                stringResource(R.string.cycle_status_due)
            }
        is CycleStatus.InsufficientData ->
            stringResource(R.string.cycle_status_no_data)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .testTag("cycle-status-banner"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.sm),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.md),
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (status is CycleStatus.InsufficientData) {
                    Text(
                        text = stringResource(R.string.cycle_status_no_data_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
