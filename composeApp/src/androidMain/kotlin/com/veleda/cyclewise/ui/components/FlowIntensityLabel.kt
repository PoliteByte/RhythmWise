package com.veleda.cyclewise.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.FlowIntensity

/**
 * Single source of truth for the user-facing label of a [FlowIntensity].
 *
 * The period page chips and the day-summary sheet previously formatted the enum
 * name independently (one ALL-CAPS, one title case) and disagreed — beta
 * feedback flagged the shouting chips (issue #146). Both surfaces now render
 * through this mapper.
 */
@Composable
fun flowIntensityLabel(intensity: FlowIntensity): String = stringResource(
    when (intensity) {
        FlowIntensity.LIGHT -> R.string.flow_intensity_light
        FlowIntensity.MEDIUM -> R.string.flow_intensity_medium
        FlowIntensity.HEAVY -> R.string.flow_intensity_heavy
    }
)
