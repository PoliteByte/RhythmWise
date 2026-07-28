package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.testutil.buildPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class NextPeriodPredictionGeneratorTest {

    private lateinit var generator: NextPeriodPredictionGenerator

    @BeforeTest
    fun setUp() {
        generator = NextPeriodPredictionGenerator()
    }

    @Test
    fun generate_WHEN_singlePeriodAndNoTypicalLength_THEN_predictsWithDefaultCycle() {
        // ARRANGE — one period, no derivable average, no user answer (issue #143:
        // the resolver falls back to the 28-day default instead of staying silent)
        val start = LocalDate(2025, 1, 1)
        val data = InsightData(
            allPeriods = listOf(buildPeriod(startDate = start, endDate = LocalDate(2025, 1, 5))),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        assertEquals(start.plus(28, DateTimeUnit.DAY), insight.predictedDate)
    }

    @Test
    fun generate_WHEN_singlePeriodAndTypicalLengthProvided_THEN_predictsWithTypicalLength() {
        // ARRANGE — the user's onboarding answer drives the first prediction (issue #143)
        val start = LocalDate(2025, 1, 1)
        val data = InsightData(
            allPeriods = listOf(buildPeriod(startDate = start, endDate = LocalDate(2025, 1, 5))),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = null,
            topSymptomsCount = 3,
            typicalCycleLengthDays = 30,
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        assertEquals(start.plus(30, DateTimeUnit.DAY), insight.predictedDate)
    }

    @Test
    fun generate_WHEN_historyExistsAndTypicalLengthProvided_THEN_derivedAverageWins() {
        // ARRANGE — two completed periods 27 days apart; the user's answer must NOT override history
        val older = LocalDate(2025, 1, 1)
        val latest = older.plus(27, DateTimeUnit.DAY)
        val data = InsightData(
            allPeriods = listOf(
                buildPeriod(startDate = latest, endDate = latest.plus(4, DateTimeUnit.DAY)),
                buildPeriod(startDate = older, endDate = older.plus(5, DateTimeUnit.DAY)),
            ),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 27.0,
            topSymptomsCount = 3,
            typicalCycleLengthDays = 35,
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        assertEquals(latest.plus(27, DateTimeUnit.DAY), insight.predictedDate)
    }

    @Test
    fun generate_WHEN_noPeriodsExist_THEN_returnsEmptyList() {
        // ARRANGE
        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.0,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertTrue(result.isEmpty())
    }

    @Test
    fun generate_WHEN_dataIsAvailable_THEN_returnsNextPeriodPrediction() {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val latestPeriodStart = today.minus(20, DateTimeUnit.DAY)
        val averageCycleLength = 28.0
        val latestPeriod = buildPeriod(
            startDate = latestPeriodStart,
            endDate = latestPeriodStart.plus(5, DateTimeUnit.DAY)
        )
        val data = InsightData(
            allPeriods = listOf(latestPeriod),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = averageCycleLength,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        val expectedDate = latestPeriodStart.plus(28, DateTimeUnit.DAY)
        assertEquals(expectedDate, insight.predictedDate)
        assertEquals(today.daysUntil(expectedDate), insight.daysUntilPrediction)
    }

    @Test
    fun generate_WHEN_derivedAverageHasFraction_THEN_roundsToNearestDay() {
        // ARRANGE — completed periods with start-to-start gaps of 28 and 29
        // days: derived average 28.5 rounds to 29
        val first = LocalDate(2025, 1, 1)
        val second = first.plus(28, DateTimeUnit.DAY)
        val third = second.plus(29, DateTimeUnit.DAY)
        val data = InsightData(
            allPeriods = listOf(
                buildPeriod(startDate = third, endDate = third.plus(4, DateTimeUnit.DAY)),
                buildPeriod(startDate = second, endDate = second.plus(4, DateTimeUnit.DAY)),
                buildPeriod(startDate = first, endDate = first.plus(4, DateTimeUnit.DAY)),
            ),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.5,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        // 28.5 rounds to 29
        val expectedDate = third.plus(29, DateTimeUnit.DAY)
        assertEquals(expectedDate, insight.predictedDate)
    }

    @Test
    fun generate_WHEN_multiplePeriodsExist_THEN_usesFirstPeriodInList() {
        // ARRANGE — allPeriods is sorted by startDate descending
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val latestStart = today.minus(10, DateTimeUnit.DAY)
        val olderStart = today.minus(40, DateTimeUnit.DAY)
        val latestPeriod = buildPeriod(startDate = latestStart, endDate = latestStart.plus(4, DateTimeUnit.DAY))
        val olderPeriod = buildPeriod(startDate = olderStart, endDate = olderStart.plus(5, DateTimeUnit.DAY))
        val data = InsightData(
            allPeriods = listOf(latestPeriod, olderPeriod),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 30.0,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT — uses latestPeriod (first in list)
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<NextPeriodPrediction>(insight)
        val expectedDate = latestStart.plus(30, DateTimeUnit.DAY)
        assertEquals(expectedDate, insight.predictedDate)
        assertEquals("NEXT_PERIOD_PREDICTION", insight.id)
        assertEquals(110, insight.priority)
    }
}
