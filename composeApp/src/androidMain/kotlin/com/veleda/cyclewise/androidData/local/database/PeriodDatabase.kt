package com.veleda.cyclewise.androidData.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veleda.cyclewise.androidData.local.dao.PeriodDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.dao.MedicationDao
import com.veleda.cyclewise.androidData.local.dao.MedicationLogDao
import com.veleda.cyclewise.androidData.local.dao.PeriodLogDao
import com.veleda.cyclewise.androidData.local.dao.SymptomDao
import com.veleda.cyclewise.androidData.local.dao.SymptomLogDao
import com.veleda.cyclewise.androidData.local.dao.CustomTagDao
import com.veleda.cyclewise.androidData.local.dao.CustomTagLogDao
import com.veleda.cyclewise.androidData.local.dao.UserCycleSettingsDao
import com.veleda.cyclewise.androidData.local.dao.WaterIntakeDao
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_1_2
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_2_3
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_3_4
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_4_5
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_5_6
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_6_7
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_7_8
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_8_9
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_9_10
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_10_11
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_11_12
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_12_13
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_13_14
import com.veleda.cyclewise.androidData.local.entities.CustomTagEntity
import com.veleda.cyclewise.androidData.local.entities.CustomTagLogEntity
import com.veleda.cyclewise.androidData.local.entities.PeriodEntity
import com.veleda.cyclewise.androidData.local.entities.Converters
import com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import com.veleda.cyclewise.androidData.local.entities.MedicationLogEntity
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import com.veleda.cyclewise.androidData.local.entities.SymptomLogEntity
import com.veleda.cyclewise.androidData.local.entities.PeriodLogEntity
import com.veleda.cyclewise.androidData.local.entities.UserCycleSettingsEntity
import com.veleda.cyclewise.androidData.local.entities.WaterIntakeEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * SQLCipher-encrypted Room database for all user health data.
 *
 * Opened via [create] with a passphrase-derived key. The caller (session scope) is
 * responsible for zeroizing the passphrase [ByteArray] after the database is opened.
 *
 * **Security:** All data at rest is AES-256-GCM encrypted via SQLCipher.
 * The database file (`cyclewise.db`) is unreadable without the correct passphrase.
 *
 * **Schema version:** 14. All migrations are registered in [create] and tested individually.
 */
@Database(
    entities = [
        PeriodEntity::class,
        DailyEntryEntity::class,
        SymptomEntity::class,
        MedicationEntity::class,
        MedicationLogEntity::class,
        SymptomLogEntity::class,
        PeriodLogEntity::class,
        WaterIntakeEntity::class,
        CustomTagEntity::class,
        CustomTagLogEntity::class,
        UserCycleSettingsEntity::class
    ],
    version = 14,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PeriodDatabase : RoomDatabase() {
    /** Returns the [PeriodDao] for CRUD operations on the `periods` table. */
    abstract fun periodDao(): PeriodDao

    /** Returns the [DailyEntryDao] for CRUD operations on the `daily_entries` table. */
    abstract fun dailyEntryDao(): DailyEntryDao

    /** Returns the [SymptomDao] for CRUD operations on the `symptom_library` table. */
    abstract fun symptomDao(): SymptomDao

    /** Returns the [MedicationDao] for CRUD operations on the `medication_library` table. */
    abstract fun medicationDao(): MedicationDao

    /** Returns the [MedicationLogDao] for CRUD operations on the `medication_logs` table. */
    abstract fun medicationLogDao(): MedicationLogDao

    /** Returns the [SymptomLogDao] for CRUD operations on the `symptom_logs` table. */
    abstract fun symptomLogDao(): SymptomLogDao

    /** Returns the [PeriodLogDao] for CRUD operations on the `period_logs` table. */
    abstract fun periodLogDao(): PeriodLogDao

    /** Returns the [WaterIntakeDao] for CRUD operations on the `water_intake` table. */
    abstract fun waterIntakeDao(): WaterIntakeDao

    /** Returns the [CustomTagDao] for CRUD operations on the `custom_tag_library` table. */
    abstract fun customTagDao(): CustomTagDao

    /** Returns the [CustomTagLogDao] for CRUD operations on the `custom_tag_logs` table. */
    abstract fun customTagLogDao(): CustomTagLogDao

    /** Returns the [UserCycleSettingsDao] for the single-row `user_cycle_settings` table. */
    abstract fun userCycleSettingsDao(): UserCycleSettingsDao

    /**
     * Re-encrypts the database with a new passphrase-derived key via SQLCipher's
     * public `changePassword(byte[])`.
     *
     * This method **closes** the Room database first to release the file lock, then opens
     * a raw SQLCipher connection with [oldKey] via the `byte[]` overload of `openDatabase`
     * (which passes the bytes to `sqlite3_key()` — SQLCipher applies PBKDF2 since the key
     * is 32 bytes and does not match the hex literal format). It then calls
     * `changePassword(byte[])`, which routes the same raw bytes to `sqlite3_rekey()`,
     * ensuring PBKDF2 is applied consistently for both open and rekey operations.
     *
     * **History:** the legacy `android-database-sqlcipher` artifact only exposed
     * `changePassword(String/char[])`, which mutf8-encode the key and derive a
     * *different* database key than the raw `byte[]` open path — this method used to
     * reach the private `native rekey(byte[])` via reflection to compensate. The
     * maintained `sqlcipher-android` artifact makes the raw-byte overload public, so
     * the reflection hack is gone.
     *
     * **Key ownership:** `changePassword(byte[])` retains the passed array **by
     * reference** inside the open connection's configuration. Both keys are therefore
     * zeroized only *after* the raw connection is closed — do not reorder the
     * `finally` blocks below.
     *
     * **Important:** After this method returns, the Room [PeriodDatabase] instance is
     * **closed and stale**. The caller must recreate the session (close the Koin session
     * scope and force re-authentication) so that a fresh [PeriodDatabase] is opened with
     * the new key via [SupportOpenHelperFactory].
     *
     * **Security:** Both [oldKey] and [newKey] arrays are zeroized in a `finally` block after
     * the operation completes, regardless of success or failure. The caller should also zeroize
     * any copies of the keys it holds.
     *
     * @param context application context for resolving the database file path and loading
     *                SQLCipher native libraries.
     * @param oldKey  32-byte AES key derived from the current passphrase (needed for opening
     *                the raw connection and for rollback).
     *                **Consumed and zeroized by this method** — do not reuse after calling.
     * @param newKey  32-byte AES key derived from the new passphrase.
     *                **Consumed and zeroized by this method** — do not reuse after calling.
     * @throws RekeyVerificationFailedException if the post-rekey verification fails (with or
     *         without successful rollback).
     */
    fun changeEncryptionKey(context: Context, oldKey: ByteArray, newKey: ByteArray) {
        val dbFile = context.getDatabasePath("cyclewise.db")
        try {
            // Close Room's connection to release the file lock
            close()

            System.loadLibrary("sqlcipher")
            val rawDb = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                oldKey,
                null,   // cursorFactory
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,
                null,   // errorHandler
                null,   // hook
            )
            try {
                rawDb.changePassword(newKey)
                // Verify the new key works
                rawDb.rawQuery("SELECT count(*) FROM sqlite_master", null)
                    .use { it.moveToFirst() }
            } catch (e: Exception) {
                try {
                    rawDb.changePassword(oldKey)
                } catch (rollback: Exception) {
                    throw RekeyVerificationFailedException(
                        "Rekey failed and rollback also failed",
                        e,
                    )
                }
                throw RekeyVerificationFailedException(
                    "Rekey failed; rolled back to old key",
                    e,
                )
            } finally {
                rawDb.close()
            }
        } finally {
            oldKey.fill(0)
            newKey.fill(0)
        }
    }

    companion object {
        /**
         * Current Room schema version — must stay in sync with [Database.version] above.
         *
         * Exposed as a constant so that [BackupManager] can embed it in the backup
         * metadata and check imported backups without reflection.
         */
        const val SCHEMA_VERSION = 14

        /**
         * Creates (or opens) the encrypted database backed by SQLCipher.
         *
         * ## Security: SupportOpenHelperFactory reference semantics
         *
         * [SupportOpenHelperFactory] stores the [passphrase] array **by reference**, not
         * by copy — the same semantics the legacy `SupportFactory` had. Meanwhile,
         * `Room.databaseBuilder().build()` returns **without** opening the
         * underlying SQLCipher file — the actual file open is deferred until the first
         * DAO query or an explicit call to [openHelper.writableDatabase].
         *
         * This creates a dangerous ordering constraint: if the caller zeros the
         * [passphrase] array between `build()` and the first real database open,
         * the factory will read an all-zeros key and SQLCipher will silently
         * encrypt with the wrong key (or open an empty database), making **every**
         * passphrase appear valid.
         *
         * To avoid this, callers must do **one** of the following:
         * 1. Pass `passphrase.copyOf()` so the factory has its own independent array
         *    (the approach used by [createDatabaseAndZeroizeKey]).
         * 2. Call `db.openHelper.writableDatabase` **before** zeroing the original
         *    array, which forces SQLCipher to consume the key immediately.
         *
         * Unlike the legacy `SupportFactory` (whose default `clearPassphrase = true`
         * zeroed its own array after the first open), [SupportOpenHelperFactory] keeps
         * its array untouched for the helper's lifetime — the open connection needs it
         * (e.g. for `changePassword`). The copy lives exactly as long as the session's
         * database and is reclaimed with it.
         *
         * @param context    application context for the Room builder.
         * @param passphrase 32-byte AES key derived from the user's passphrase.
         *                   **Caller must zeroize this array after this method returns.**
         *                   See the security section above for ordering constraints.
         * @param dbName     database file name (default: "cyclewise.db").
         * @return the [PeriodDatabase] instance. **Not yet opened** — the caller must
         *         call `db.openHelper.writableDatabase` to force SQLCipher to consume
         *         the key before any DAO access.
         */
        fun create(
            context: Context,
            passphrase: ByteArray,
            dbName: String = "cyclewise.db"
        ): PeriodDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context,
                PeriodDatabase::class.java,
                dbName
            )
                .openHelperFactory(factory)
                .addMigrations(
                    Migration_1_2,
                    Migration_2_3,
                    Migration_3_4,
                    Migration_4_5,
                    Migration_5_6,
                    Migration_6_7,
                    Migration_7_8,
                    Migration_8_9,
                    Migration_9_10,
                    Migration_10_11,
                    Migration_11_12,
                    Migration_12_13,
                    Migration_13_14
                )
                .build()
        }
    }
}

/**
 * Thrown when the post-rekey verification query fails after re-encryption.
 *
 * The message indicates whether a rollback to the old key succeeded. If rollback also
 * failed, the database may be in an indeterminate encryption state.
 */
class RekeyVerificationFailedException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)