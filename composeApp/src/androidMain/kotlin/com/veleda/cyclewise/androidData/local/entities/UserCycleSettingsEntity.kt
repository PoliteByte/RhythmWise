package com.veleda.cyclewise.androidData.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.veleda.cyclewise.domain.models.CycleSettings

/**
 * Single-row table holding user-provided cycle configuration (issue #143).
 *
 * Kept in the encrypted database (not DataStore) by explicit decision: typical
 * cycle length is health-adjacent data and the privacy model keeps all health
 * data behind SQLCipher. The row uses a fixed [SINGLETON_ROW_ID]; a missing row
 * means "never configured" and callers fall back to [CycleSettings] defaults.
 *
 * @property id                      Always [SINGLETON_ROW_ID].
 * @property typicalCycleLengthDays  Self-reported typical cycle length, or null
 *                                   when the user skipped the question.
 * @property defaultPeriodLengthDays Days auto-filled when a new period starts
 *                                   (issue #144); defaults to 5.
 */
@Entity(tableName = "user_cycle_settings")
data class UserCycleSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ROW_ID,
    @ColumnInfo(name = "typical_cycle_length_days")
    val typicalCycleLengthDays: Int?,
    @ColumnInfo(name = "default_period_length_days", defaultValue = "5")
    val defaultPeriodLengthDays: Int = CycleSettings.DEFAULT_PERIOD_LENGTH_DAYS,
) {
    /** Maps this entity to the shared-module domain model. */
    fun toDomain(): CycleSettings = CycleSettings(
        typicalCycleLengthDays = typicalCycleLengthDays,
        defaultPeriodLengthDays = defaultPeriodLengthDays,
    )

    companion object {
        /** Fixed primary key — the table never holds more than one row. */
        const val SINGLETON_ROW_ID = 1

        /** Maps the domain model to the singleton entity row. */
        fun fromDomain(settings: CycleSettings): UserCycleSettingsEntity =
            UserCycleSettingsEntity(
                id = SINGLETON_ROW_ID,
                typicalCycleLengthDays = settings.typicalCycleLengthDays,
                defaultPeriodLengthDays = settings.defaultPeriodLengthDays,
            )
    }
}
