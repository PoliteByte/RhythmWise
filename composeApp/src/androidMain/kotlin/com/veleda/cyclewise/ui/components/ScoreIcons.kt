package com.veleda.cyclewise.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps a 1-5 mood score to a sentiment face, clamping out-of-range values.
 *
 * Single source of truth shared by the wellness input selector and the
 * day-summary sheet so the two surfaces always speak the same icon language
 * (issue #146 follow-up: "five faces for mood, ranging from a sad face to
 * very happy").
 */
fun moodFaceIcon(score: Int): ImageVector = when {
    score <= 1 -> Icons.Default.SentimentVeryDissatisfied
    score == 2 -> Icons.Default.SentimentDissatisfied
    score == 3 -> Icons.Default.SentimentNeutral
    score == 4 -> Icons.Default.SentimentSatisfied
    else -> Icons.Default.SentimentVerySatisfied
}
