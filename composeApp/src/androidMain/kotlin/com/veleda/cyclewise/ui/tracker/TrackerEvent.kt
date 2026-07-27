package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.domain.models.HeatmapMetric
import kotlinx.datetime.LocalDate

/**
 * Defines all user interactions that can occur on the Tracker screen.
 */
sealed interface TrackerEvent {
    /** The user selected or deselected a heatmap metric overlay. */
    data class SelectHeatmapMetric(val metric: HeatmapMetric?) : TrackerEvent
    /** The user tapped a day in the calendar. */
    data class DayTapped(val date: LocalDate) : TrackerEvent

    /** The user long-pressed a day to mark or unmark it as a period day. */
    data class PeriodMarkDay(val date: LocalDate) : TrackerEvent

    /** Undo an auto-filled period start: shrink the period back to its single start day (issue #144). */
    data class UndoAutoFill(val periodId: String, val startDate: LocalDate) : TrackerEvent

    /** The user long-pressed [anchorDate] and dragged to [releaseDate], requesting a period range operation. */
    data class PeriodRangeDragged(val anchorDate: LocalDate, val releaseDate: LocalDate) : TrackerEvent

    /** The user has dismissed the bottom sheet showing the log summary. */
    data object DismissLogSheet : TrackerEvent

    /** The user tapped the Edit button in the bottom sheet. */
    data class EditLogClicked(val date: LocalDate) : TrackerEvent

    /** The user tapped the Delete button — shows confirmation dialog. */
    data class DeletePeriodRequested(val periodId: String) : TrackerEvent

    /** The user confirmed deletion in the dialog. */
    data class DeletePeriodConfirmed(val periodId: String) : TrackerEvent

    /** The user dismissed the deletion confirmation dialog. */
    data object DeletePeriodDismissed : TrackerEvent

    /** Dispatched when the screen is first composed, triggering auto-close period logic. */
    data object ScreenEntered : TrackerEvent

    // ── Unmark Period Confirmation ────────────────────────────────────

    /** The user confirmed unmarking a single period day that had data. */
    data class UnmarkPeriodDayConfirmed(val date: LocalDate) : TrackerEvent

    /** The user confirmed unmarking multiple period days (drag-shrink) that had data. */
    data class UnmarkPeriodRangeConfirmed(val dates: List<LocalDate>) : TrackerEvent

    /** The user dismissed the unmark-period confirmation dialog. */
    data object UnmarkPeriodDismissed : TrackerEvent

    // ── Educational ──────────────────────────────────────────────────

    /** The user tapped an info button to view educational content for the given [contentTag]. */
    data class ShowEducationalSheet(val contentTag: String) : TrackerEvent

    /** The user dismissed the educational bottom sheet. */
    data object DismissEducationalSheet : TrackerEvent
}

/**
 * One-time side effects emitted by [TrackerViewModel].
 */
sealed interface TrackerEffect {
    /** Navigate to the daily log detail screen for the given [date]. */
    data class NavigateToDailyLog(val date: LocalDate) : TrackerEffect

    /** A period day was marked or unmarked, triggering a success animation. */
    data object PeriodMarked : TrackerEffect

    /**
     * A new period start was auto-filled to a multi-day range (issue #144).
     * The UI shows an undo snackbar; undo shrinks the period back to its
     * single tapped start day.
     */
    data class PeriodAutoFilled(
        val periodId: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
    ) : TrackerEffect
}
