package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.CycleLengthResolver
import com.veleda.cyclewise.domain.PeriodLengthResolver
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.math.roundToInt

/**
 * The single most important fact to surface when the app opens (issue #142,
 * beta feedback: "what day of period you're on or when your next period will
 * be — make this much more accessible").
 */
sealed interface CycleStatus {
    /** Today is day [dayOfPeriod] (1-based) of the user's period. */
    data class OnPeriod(val dayOfPeriod: Int) : CycleStatus

    /**
     * The next period is predicted for [predictedDate], [daysAway] days from
     * today. [daysAway] <= 0 means due/overdue — rendered as "any day now"
     * rather than re-projected, keeping the type honest.
     */
    data class Predicted(val predictedDate: LocalDate, val daysAway: Int) : CycleStatus

    /** No periods logged yet — nothing to compute from. */
    data object InsufficientData : CycleStatus
}

/**
 * Computes the glanceable [CycleStatus] for the home screen.
 *
 * Uses the same [CycleLengthResolver] precedence as the Insights prediction
 * card and the reminder worker (issue #143), so the three surfaces can never
 * disagree. Ongoing periods are assumed to last the [PeriodLengthResolver]
 * length, consistent with the calendar's phase rendering (issue #145).
 */
class GetCycleStatusUseCase(
    private val periodRepository: PeriodRepository,
) {
    suspend operator fun invoke(today: LocalDate): CycleStatus {
        val periods = periodRepository.getAllPeriods().first()
        val latest = periods.maxByOrNull { it.startDate }
            ?: return CycleStatus.InsufficientData

        val settings = periodRepository.observeCycleSettings().first()

        val periodEnd = latest.endDate ?: latest.startDate.plus(
            PeriodLengthResolver.resolve(periods, settings.defaultPeriodLengthDays) - 1,
            DateTimeUnit.DAY,
        )
        if (today in latest.startDate..periodEnd) {
            return CycleStatus.OnPeriod(latest.startDate.daysUntil(today) + 1)
        }

        val resolved = CycleLengthResolver.resolve(periods, settings.typicalCycleLengthDays)
        val predicted = latest.startDate.plus(resolved.days.roundToInt(), DateTimeUnit.DAY)
        return CycleStatus.Predicted(
            predictedDate = predicted,
            daysAway = today.daysUntil(predicted),
        )
    }
}
