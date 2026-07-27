package com.veleda.cyclewise.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Modal bottom sheet displaying one or more [EducationalArticle]s with source
 * attribution and a medical disclaimer footer.
 *
 * Articles are **collapsed to their titles by default** and expand on tap —
 * dumping every full article at once overwhelmed beta testers ("opened it and
 * instantly closed from overwhelm", issue #153). A single article is shown
 * expanded, since the user already chose exactly one topic.
 *
 * Uses `skipPartiallyExpanded = true` to match the existing [TrackerScreen]
 * bottom sheet pattern — the sheet opens fully rather than stopping at a
 * partially-expanded state.
 *
 * @param articles  The articles to display. Must be non-empty.
 * @param onDismiss Callback invoked when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationalBottomSheet(
    articles: List<EducationalArticle>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        EducationalSheetContent(articles = articles)
    }
}

/**
 * Sheet body — extracted from [EducationalBottomSheet] so the collapse/expand
 * behavior is testable without a modal window (repo convention: test the
 * content composable directly).
 */
@Composable
internal fun EducationalSheetContent(articles: List<EducationalArticle>) {
    val dims = LocalDimensions.current

    LazyColumn(
        contentPadding = PaddingValues(horizontal = dims.md, vertical = dims.sm),
        verticalArrangement = Arrangement.spacedBy(dims.md),
    ) {
        items(articles, key = { it.id }) { article ->
            ExpandableArticle(
                article = article,
                initiallyExpanded = articles.size == 1,
            )
            Spacer(Modifier.height(dims.xs))
            HorizontalDivider()
        }

        item {
            MedicalDisclaimer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dims.sm, bottom = dims.md),
            )
        }
    }
}

/**
 * One article row: always-visible title with an expand/collapse chevron; body
 * and source attribution appear only when expanded.
 */
@Composable
private fun ExpandableArticle(
    article: EducationalArticle,
    initiallyExpanded: Boolean,
) {
    val dims = LocalDimensions.current
    var expanded by rememberSaveable(article.id) { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.educational_collapse_cd else R.string.educational_expand_cd,
                    article.title,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
                MarkdownText(
                    text = article.body,
                    style = MaterialTheme.typography.bodyMedium,
                )
                SourceAttribution(sourceName = article.sourceName)
            }
        }
    }
}
