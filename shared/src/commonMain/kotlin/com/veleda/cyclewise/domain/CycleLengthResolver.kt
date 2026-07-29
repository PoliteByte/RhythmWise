package com.veleda.cyclewise.domain

import com.veleda.cyclewise.domain.models.Period
import kotlinx.datetime.daysUntil

/**
 * How a resolved cycle length was obtained — lets UI phrase predictions
 * honestly ("based on your logged cycles" vs "based on your typical length"
 * vs "estimate").
 */
enum class CycleLengthSource {
    /** Average of the user's logged completed cycles (most reliable). */
    DERIVED_AVERAGE,

    /** The user's self-reported typical cycle length (issue #143). */
    USER_PROVIDED,

    /** The medical-standard 28-day default — no data, no answer. */
    DEFAULT,
}

/** A resolved cycle length plus where it came from. */
data class CycleLengthEstimate(
    val days: Double,
    val source: CycleLengthSource,
)

/**
 * Single source of truth for "how long is this user's cycle?" (issue #143).
 *
 * Precedence: derived average from ≥ 2 completed periods → the user's
 * self-reported typical length → [DEFAULT_CYCLE_LENGTH_DAYS]. [resolve] never
 * fails, which removes the pre-#143 dead zone where predictions and phase
 * coloring required two full logged cycles before appearing.
 *
 * Callers that must not fabricate data (e.g. the "your average cycle" insight)
 * use [derivedAverage], which still returns null without enough history.
 */
object CycleLengthResolver {

    /** Medical-standard fallback cycle length. */
    const val DEFAULT_CYCLE_LENGTH_DAYS = 28

    /** Minimum completed periods before a derived average is trusted. */
    const val MIN_PERIODS_FOR_AVERAGE = 2

    /**
     * Resolves the working cycle length for [periods] with [userTypicalCycleLengthDays]
     * as the user-provided fallback. Never returns null.
     */
    fun resolve(periods: List<Period>, userTypicalCycleLengthDays: Int?): CycleLengthEstimate {
        derivedAverage(periods)?.let {
            return CycleLengthEstimate(days = it, source = CycleLengthSource.DERIVED_AVERAGE)
        }
        userTypicalCycleLengthDays?.let {
            return CycleLengthEstimate(days = it.toDouble(), source = CycleLengthSource.USER_PROVIDED)
        }
        return CycleLengthEstimate(
            days = DEFAULT_CYCLE_LENGTH_DAYS.toDouble(),
            source = CycleLengthSource.DEFAULT,
        )
    }

    /**
     * Average start-to-start cycle length over completed periods (those with an
     * end date), or null when fewer than [MIN_PERIODS_FOR_AVERAGE] exist.
     * Ongoing periods are excluded — their length is not yet known.
     */
    fun derivedAverage(periods: List<Period>): Double? {
        val completed = periods
            .filter { it.endDate != null }
            .sortedBy { it.startDate }
        if (completed.size < MIN_PERIODS_FOR_AVERAGE) return null

        val gaps = completed
            .zipWithNext { a, b -> a.startDate.daysUntil(b.startDate) }
            .filter { it > 0 }
        if (gaps.isEmpty()) return null

        return gaps.map { it.toDouble() }.average()
    }
}
