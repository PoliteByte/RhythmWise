package com.veleda.cyclewise.domain

import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.daysUntil
import kotlin.math.roundToInt

/**
 * Resolves how many days to auto-fill when the user marks a new period start
 * (issue #144): historical average of completed periods → the user's
 * default-period-length setting.
 *
 * The history average deliberately **excludes 1-day periods**: before #144, a
 * day-1-only logger accumulated exactly those artifacts, and including them
 * would drag the average to 1 and disable the feature for the very user it
 * serves. At least [MIN_HISTORY_PERIODS] qualifying periods are required, and
 * the result is clamped to a plausible 2..10 day range.
 */
object PeriodLengthResolver {

    /** Minimum completed periods of length ≥ 2 before history drives the fill length. */
    const val MIN_HISTORY_PERIODS = 3

    /** Clamp bounds for a history-derived fill length. */
    const val MIN_FILL_DAYS = 2
    const val MAX_FILL_DAYS = 10

    /**
     * Returns the auto-fill length in days for a new period.
     *
     * @param periods                 all recorded periods (any order).
     * @param defaultPeriodLengthDays the user's setting (falls back to 5 upstream).
     */
    fun resolve(periods: List<Period>, defaultPeriodLengthDays: Int): Int {
        val qualifyingLengths = periods
            .filter { it.endDate != null }
            .map { it.startDate.daysUntil(it.endDate!!) + 1 }
            .filter { it >= MIN_FILL_DAYS }

        if (qualifyingLengths.size >= MIN_HISTORY_PERIODS) {
            return qualifyingLengths
                .map { it.toDouble() }
                .average()
                .roundToInt()
                .coerceIn(MIN_FILL_DAYS, MAX_FILL_DAYS)
        }
        return defaultPeriodLengthDays
    }
}

/**
 * Outcome of [com.veleda.cyclewise.domain.repository.PeriodRepository.logPeriodStart].
 *
 * @property autoFilled true when a multi-day range was created (the tapped day
 *   was an island and the fill length exceeded one day).
 * @property periodId   id of the created period when [autoFilled], for undo.
 * @property filledStart first day of the created/affected range.
 * @property filledEnd   last day of the created/affected range.
 */
data class PeriodStartResult(
    val autoFilled: Boolean,
    val periodId: String?,
    val filledStart: kotlinx.datetime.LocalDate,
    val filledEnd: kotlinx.datetime.LocalDate,
)
