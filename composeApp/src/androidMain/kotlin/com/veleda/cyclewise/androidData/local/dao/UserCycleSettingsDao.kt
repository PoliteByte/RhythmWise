package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.veleda.cyclewise.androidData.local.entities.UserCycleSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the single-row `user_cycle_settings` table (issue #143).
 */
@Dao
interface UserCycleSettingsDao {

    /** Emits the settings row, or null while the user has never configured anything. */
    @Query("SELECT * FROM user_cycle_settings WHERE id = ${UserCycleSettingsEntity.SINGLETON_ROW_ID}")
    fun observe(): Flow<UserCycleSettingsEntity?>

    /** Returns the current row once, or null when absent. */
    @Query("SELECT * FROM user_cycle_settings WHERE id = ${UserCycleSettingsEntity.SINGLETON_ROW_ID}")
    suspend fun get(): UserCycleSettingsEntity?

    /** Inserts or replaces the singleton settings row. */
    @Upsert
    suspend fun upsert(entity: UserCycleSettingsEntity)
}
