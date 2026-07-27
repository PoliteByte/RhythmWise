package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.CycleLengthResolver
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Predicts the start date of the next menstrual period.
 *
 * Calculation: `latestPeriod.startDate + resolved cycle length (rounded)`, where
 * the cycle length comes from [CycleLengthResolver] — derived average when ≥ 2
 * completed cycles exist, else the user's typical cycle length, else the 28-day
 * default (issue #143). A prediction therefore appears from the very first
 * logged period; only a completely empty history yields no insight.
 *
 * This generator runs first in the [InsightEngine] pipeline so that other generators
 * (e.g., [SymptomPhasePatternGenerator]) can use its prediction for recurrence forecasting.
 */
class NextPeriodPredictionGenerator : InsightGenerator {
    /**
     * Calculates the predicted start date of the next period by adding the
     * resolved cycle length to the latest period's start date.
     *
     * @param data Aggregated cycle data; requires at least one period.
     * @return A single-element list with a [NextPeriodPrediction], or empty
     *         when no periods exist.
     */
    @OptIn(ExperimentalTime::class)
    override fun generate(data: InsightData): List<Insight> {
        val latestCycle = data.allPeriods.firstOrNull() ?: return emptyList()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val resolved = CycleLengthResolver.resolve(data.allPeriods, data.typicalCycleLengthDays)
        val averageLengthInDays = resolved.days.roundToInt()
        val predictedDate = latestCycle.startDate.plus(averageLengthInDays, DateTimeUnit.DAY)

        val daysUntil = today.daysUntil(predictedDate)

        return listOf(NextPeriodPrediction(
            predictedDate = predictedDate,
            daysUntilPrediction = daysUntil
        ))
    }
}