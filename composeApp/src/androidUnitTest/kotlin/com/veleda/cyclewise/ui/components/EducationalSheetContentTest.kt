package com.veleda.cyclewise.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [EducationalSheetContent] — progressive disclosure of educational
 * articles (issue #153): titles always visible, bodies expand on tap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class EducationalSheetContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildArticle(index: Int) = EducationalArticle(
        id = "article-$index",
        title = "Article Title $index",
        body = "Body text of article $index",
        category = ArticleCategory.CYCLE_BASICS,
        contentTags = listOf("tag"),
        sourceName = "Source $index",
        sourceUrl = "https://example.org/$index",
        sortOrder = index,
    )

    private fun setContent(articles: List<EducationalArticle>) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    EducationalSheetContent(articles = articles)
                }
            }
        }
    }

    @Test
    fun `GIVEN multiple articles THEN titles visible and bodies collapsed`() {
        // GIVEN three articles
        setContent(listOf(buildArticle(1), buildArticle(2), buildArticle(3)))

        // THEN every title is scannable but no body is expanded
        composeTestRule.onNodeWithText("Article Title 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Article Title 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Article Title 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Body text of article 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Body text of article 2").assertDoesNotExist()
    }

    @Test
    fun `GIVEN collapsed article WHEN title tapped THEN body expands`() {
        // GIVEN
        setContent(listOf(buildArticle(1), buildArticle(2)))

        // WHEN
        composeTestRule.onNodeWithText("Article Title 1").performClick()

        // THEN — the tapped article expands (markdown body is an AndroidView with
        // unreliable Robolectric size semantics, so assert via the Compose-native
        // source attribution and the chevron state instead)
        composeTestRule.onNodeWithText("Source 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Collapse Article Title 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Source 2", substring = true).assertDoesNotExist()
    }

    @Test
    fun `GIVEN expanded article WHEN title tapped again THEN body collapses`() {
        // GIVEN an expanded article
        setContent(listOf(buildArticle(1), buildArticle(2)))
        composeTestRule.onNodeWithText("Article Title 1").performClick()

        // WHEN
        composeTestRule.onNodeWithText("Article Title 1").performClick()

        // THEN
        composeTestRule.onNodeWithText("Body text of article 1").assertDoesNotExist()
    }

    @Test
    fun `GIVEN single article THEN it renders already expanded`() {
        // GIVEN exactly one article — the user picked one topic, no extra tap needed
        setContent(listOf(buildArticle(1)))

        // THEN — already expanded: source attribution visible, chevron shows collapse
        composeTestRule.onNodeWithText("Source 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Collapse Article Title 1").assertIsDisplayed()
    }
}
