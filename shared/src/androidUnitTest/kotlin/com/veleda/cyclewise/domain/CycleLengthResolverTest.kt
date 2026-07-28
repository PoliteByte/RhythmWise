package com.veleda.cyclewise.domain

import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

/**
 * Tests for [CycleLengthResolver] — the single precedence point for cycle
 * length: derived average → user typical → 28-day default (issue #143).
 */
@OptIn(ExperimentalTime::class)
class CycleLengthResolverTest {

    private val base = LocalDate(2025, 1, 1)

    private fun completedPeriods(vararg gaps: Int): List<com.veleda.cyclewise.domain.models.Period> {
        var start = base
        val result = mutableListOf(buildPeriod(startDate = start, endDate = start.plus(4, DateTimeUnit.DAY)))
        for (gap in gaps) {
            start = start.plus(gap, DateTimeUnit.DAY)
            result += buildPeriod(startDate = start, endDate = start.plus(4, DateTimeUnit.DAY))
        }
        return result
    }

    @Test
    fun resolve_WHEN_enoughCompletedPeriods_THEN_returnsDerivedAverage() {
        // ARRANGE — gaps of 28 and 30 days → average 29.0
        val periods = completedPeriods(28, 30)

        // ACT
        val estimate = CycleLengthResolver.resolve(periods, userTypicalCycleLengthDays = 35)

        // ASSERT — history beats the user's answer
        assertEquals(29.0, estimate.days)
        assertEquals(CycleLengthSource.DERIVED_AVERAGE, estimate.source)
    }

    @Test
    fun resolve_WHEN_tooFewPeriodsAndTypicalProvided_THEN_returnsUserValue() {
        // ARRANGE — a single completed period cannot yield an average
        val periods = completedPeriods()

        // ACT
        val estimate = CycleLengthResolver.resolve(periods, userTypicalCycleLengthDays = 31)

        // ASSERT
        assertEquals(31.0, estimate.days)
        assertEquals(CycleLengthSource.USER_PROVIDED, estimate.source)
    }

    @Test
    fun resolve_WHEN_noDataAtAll_THEN_returnsDefault() {
        // ACT
        val estimate = CycleLengthResolver.resolve(emptyList(), userTypicalCycleLengthDays = null)

        // ASSERT
        assertEquals(28.0, estimate.days)
        assertEquals(CycleLengthSource.DEFAULT, estimate.source)
    }

    @Test
    fun derivedAverage_WHEN_fewerThanTwoCompleted_THEN_returnsNull() {
        // ARRANGE — one completed + one ongoing period
        val ongoing = buildPeriod(startDate = base.plus(28, DateTimeUnit.DAY), endDate = null)
        val periods = completedPeriods() + ongoing

        // ACT / ASSERT — ongoing periods never count toward the average
        assertNull(CycleLengthResolver.derivedAverage(periods))
    }

    @Test
    fun derivedAverage_WHEN_unsortedInput_THEN_sortsBeforeAveraging() {
        // ARRANGE — gaps 26 and 28 supplied in reverse order
        val periods = completedPeriods(26, 28).reversed()

        // ACT / ASSERT
        assertEquals(27.0, CycleLengthResolver.derivedAverage(periods))
    }
}
