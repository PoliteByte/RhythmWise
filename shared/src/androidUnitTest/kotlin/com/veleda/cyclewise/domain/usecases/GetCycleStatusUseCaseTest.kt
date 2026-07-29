package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.CycleSettings
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.buildPeriod
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

/**
 * Tests for [GetCycleStatusUseCase] — the home-screen banner's three states
 * and their boundary days (issue #142).
 */
@OptIn(ExperimentalTime::class)
class GetCycleStatusUseCaseTest {

    private val today = LocalDate(2025, 6, 15)
    private val repository: PeriodRepository = mockk()
    private val useCase = GetCycleStatusUseCase(repository)

    private fun stub(periods: List<com.veleda.cyclewise.domain.models.Period>, settings: CycleSettings = CycleSettings()) {
        every { repository.getAllPeriods() } returns flowOf(periods)
        every { repository.observeCycleSettings() } returns flowOf(settings)
    }

    @Test
    fun invoke_WHEN_noPeriods_THEN_returnsInsufficientData() = runTest {
        // ARRANGE
        stub(emptyList())

        // ACT / ASSERT
        assertIs<CycleStatus.InsufficientData>(useCase(today))
    }

    @Test
    fun invoke_WHEN_todayInsideCompletedPeriod_THEN_returnsOnPeriodWithDay() = runTest {
        // ARRANGE — period started 2 days ago, ends tomorrow
        stub(listOf(buildPeriod(
            startDate = today.minus(2, DateTimeUnit.DAY),
            endDate = today.plus(1, DateTimeUnit.DAY),
        )))

        // ACT
        val status = useCase(today)

        // ASSERT — 1-based day of period
        assertEquals(CycleStatus.OnPeriod(3), status)
    }

    @Test
    fun invoke_WHEN_ongoingPeriodWithinAssumedLength_THEN_returnsOnPeriod() = runTest {
        // ARRANGE — ongoing period started yesterday; assumed 5-day length covers today
        stub(listOf(buildPeriod(startDate = today.minus(1, DateTimeUnit.DAY), endDate = null)))

        // ACT / ASSERT
        assertEquals(CycleStatus.OnPeriod(2), useCase(today))
    }

    @Test
    fun invoke_WHEN_betweenPeriods_THEN_predictsWithTypicalLength() = runTest {
        // ARRANGE — one completed period 10 days ago, user typical = 30
        val start = today.minus(10, DateTimeUnit.DAY)
        stub(
            listOf(buildPeriod(startDate = start, endDate = start.plus(4, DateTimeUnit.DAY))),
            CycleSettings(typicalCycleLengthDays = 30),
        )

        // ACT
        val status = useCase(today)

        // ASSERT — predicted = start + 30, i.e. 20 days away
        assertIs<CycleStatus.Predicted>(status)
        assertEquals(start.plus(30, DateTimeUnit.DAY), status.predictedDate)
        assertEquals(20, status.daysAway)
    }

    @Test
    fun invoke_WHEN_predictionOverdue_THEN_daysAwayIsNonPositive() = runTest {
        // ARRANGE — period started 30 days ago, default 28-day cycle → 2 days overdue
        val start = today.minus(30, DateTimeUnit.DAY)
        stub(listOf(buildPeriod(startDate = start, endDate = start.plus(4, DateTimeUnit.DAY))))

        // ACT
        val status = useCase(today)

        // ASSERT
        assertIs<CycleStatus.Predicted>(status)
        assertEquals(-2, status.daysAway)
    }
}
