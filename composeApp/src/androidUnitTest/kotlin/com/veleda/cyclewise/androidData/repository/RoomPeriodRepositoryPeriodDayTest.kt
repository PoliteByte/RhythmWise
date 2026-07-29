package com.veleda.cyclewise.androidData.repository

import com.veleda.cyclewise.KoinTestRule
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.testDatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for the [PeriodRepository.logPeriodDay] /
 * [PeriodRepository.unLogPeriodDay] state machines (previously untested — these
 * pin the pre-#144 behavior that drag-editing depends on), plus behavior tests
 * for the new auto-filling [PeriodRepository.logPeriodStart] (issue #144).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class RoomPeriodRepositoryPeriodDayTest : KoinTest {

    private val repositoryModule = module {
        includes(testDatabaseModule)
        single<PeriodRepository> {
            RoomPeriodRepository(
                db = get(),
                periodDao = get(),
                dailyEntryDao = get(),
                symptomDao = get(),
                symptomLogDao = get(),
                medicationDao = get(),
                medicationLogDao = get(),
                periodLogDao = get(),
                waterIntakeDao = get(),
                customTagDao = get(),
                customTagLogDao = get(),
                userCycleSettingsDao = get(),
            )
        }
    }

    @get:Rule
    val koinRule = KoinTestRule(listOf(repositoryModule))

    private val repository: PeriodRepository by inject()
    private val db: PeriodDatabase by inject()

    private val base = LocalDate(2025, 3, 10)

    @After
    fun tearDown() {
        db.close()
    }

    // ── logPeriodDay characterization (pre-#144 behavior, must not change) ──

    @Test
    fun logPeriodDay_WHEN_islandDay_THEN_createsSingleDayPeriod() = runTest {
        // ACT
        repository.logPeriodDay(base)

        // ASSERT — exactly one 1-day completed period (the drag path relies on this)
        val periods = repository.getAllPeriods().first()
        assertEquals(1, periods.size)
        assertEquals(base, periods.first().startDate)
        assertEquals(base, periods.first().endDate)
    }

    @Test
    fun logPeriodDay_WHEN_dayAfterExistingPeriod_THEN_extendsEnd() = runTest {
        // ARRANGE
        repository.createCompletedPeriod(base, base.plus(2, DateTimeUnit.DAY))

        // ACT — mark the day immediately after the period
        repository.logPeriodDay(base.plus(3, DateTimeUnit.DAY))

        // ASSERT — same period, extended by one day
        val periods = repository.getAllPeriods().first()
        assertEquals(1, periods.size)
        assertEquals(base.plus(3, DateTimeUnit.DAY), periods.first().endDate)
    }

    @Test
    fun logPeriodDay_WHEN_bridgingTwoPeriods_THEN_mergesThem() = runTest {
        // ARRANGE — two periods with exactly one free day between
        repository.createCompletedPeriod(base, base.plus(1, DateTimeUnit.DAY))
        repository.createCompletedPeriod(base.plus(3, DateTimeUnit.DAY), base.plus(5, DateTimeUnit.DAY))

        // ACT — mark the bridge day
        repository.logPeriodDay(base.plus(2, DateTimeUnit.DAY))

        // ASSERT — one merged period spanning both
        val periods = repository.getAllPeriods().first()
        assertEquals(1, periods.size)
        assertEquals(base, periods.first().startDate)
        assertEquals(base.plus(5, DateTimeUnit.DAY), periods.first().endDate)
    }

    @Test
    fun unLogPeriodDay_WHEN_middleDay_THEN_splitsPeriod() = runTest {
        // ARRANGE
        repository.createCompletedPeriod(base, base.plus(4, DateTimeUnit.DAY))

        // ACT — unmark the middle day
        repository.unLogPeriodDay(base.plus(2, DateTimeUnit.DAY))

        // ASSERT — two periods around the removed day
        val periods = repository.getAllPeriods().first().sortedBy { it.startDate }
        assertEquals(2, periods.size)
        assertEquals(base.plus(1, DateTimeUnit.DAY), periods[0].endDate)
        assertEquals(base.plus(3, DateTimeUnit.DAY), periods[1].startDate)
        assertEquals(base.plus(4, DateTimeUnit.DAY), periods[1].endDate)
    }

    // ── logPeriodStart (issue #144) ─────────────────────────────────────

    @Test
    fun logPeriodStart_WHEN_islandAndNoHistory_THEN_fillsDefaultFiveDays() = runTest {
        // ACT
        val result = repository.logPeriodStart(base)

        // ASSERT
        assertTrue(result.autoFilled)
        assertNotNull(result.periodId)
        assertEquals(base, result.filledStart)
        assertEquals(base.plus(4, DateTimeUnit.DAY), result.filledEnd)
        val period = repository.getAllPeriods().first().single()
        assertEquals(base, period.startDate)
        assertEquals(base.plus(4, DateTimeUnit.DAY), period.endDate)
    }

    @Test
    fun logPeriodStart_WHEN_settingChanged_THEN_fillsSettingLength() = runTest {
        // ARRANGE
        repository.setDefaultPeriodLengthDays(7)

        // ACT
        val result = repository.logPeriodStart(base)

        // ASSERT
        assertEquals(base.plus(6, DateTimeUnit.DAY), result.filledEnd)
    }

    @Test
    fun logPeriodStart_WHEN_enoughHistory_THEN_historyBeatsSetting() = runTest {
        // ARRANGE — three completed 4-day periods; setting says 7
        repository.setDefaultPeriodLengthDays(7)
        var start = base.minus(120, DateTimeUnit.DAY)
        repeat(3) {
            repository.createCompletedPeriod(start, start.plus(3, DateTimeUnit.DAY))
            start = start.plus(28, DateTimeUnit.DAY)
        }

        // ACT
        val result = repository.logPeriodStart(base)

        // ASSERT — 4-day historical average wins
        assertEquals(base.plus(3, DateTimeUnit.DAY), result.filledEnd)
    }

    @Test
    fun logPeriodStart_WHEN_nextPeriodClose_THEN_clampsBeforeIt() = runTest {
        // ARRANGE — an existing period starting 3 days after the new start
        repository.createCompletedPeriod(base.plus(3, DateTimeUnit.DAY), base.plus(6, DateTimeUnit.DAY))

        // ACT
        val result = repository.logPeriodStart(base)

        // ASSERT — fill stops the day before the existing period; both survive
        assertTrue(result.autoFilled)
        assertEquals(base.plus(2, DateTimeUnit.DAY), result.filledEnd)
        assertEquals(2, repository.getAllPeriods().first().size)
    }

    @Test
    fun logPeriodStart_WHEN_dayAdjacentToPeriod_THEN_delegatesWithoutAutoFill() = runTest {
        // ARRANGE — the tapped day directly follows an existing period
        repository.createCompletedPeriod(base, base.plus(2, DateTimeUnit.DAY))

        // ACT
        val result = repository.logPeriodStart(base.plus(3, DateTimeUnit.DAY))

        // ASSERT — plain extend, no auto-fill (drag semantics preserved)
        assertFalse(result.autoFilled)
        val period = repository.getAllPeriods().first().single()
        assertEquals(base.plus(3, DateTimeUnit.DAY), period.endDate)
    }
}
