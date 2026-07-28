package com.veleda.cyclewise.domain.models

/**
 * User-provided cycle configuration (issue #143).
 *
 * @property typicalCycleLengthDays  Self-reported typical cycle length in days,
 *   or null when the user skipped the question. Used as the prediction fallback
 *   until enough logged history exists — see `CycleLengthResolver`.
 * @property defaultPeriodLengthDays Number of days auto-filled when the user
 *   marks a new period start (issue #144).
 */
data class CycleSettings(
    val typicalCycleLengthDays: Int? = null,
    val defaultPeriodLengthDays: Int = DEFAULT_PERIOD_LENGTH_DAYS,
) {
    companion object {
        /** Default auto-filled period length when neither history nor a setting exists. */
        const val DEFAULT_PERIOD_LENGTH_DAYS = 5

        /** Inclusive UI bounds for the typical-cycle-length question. */
        const val MIN_CYCLE_LENGTH_DAYS = 21
        const val MAX_CYCLE_LENGTH_DAYS = 40

        /** Inclusive UI bounds for the default period length setting. */
        const val MIN_PERIOD_LENGTH_DAYS = 2
        const val MAX_PERIOD_LENGTH_DAYS = 10
    }
}
