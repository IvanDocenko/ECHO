package ru.echoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RecordingEntity::class, ReplyEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordings ADD COLUMN mood INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE recordings ADD COLUMN transcript TEXT")
                db.execSQL("ALTER TABLE recordings ADD COLUMN sealedUntil INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS replies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        rootRecordingId INTEGER NOT NULL,
                        parentReplyId INTEGER,
                        filePath TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        mood INTEGER NOT NULL DEFAULT -1,
                        transcript TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordings ADD COLUMN sealedNoDelete INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recordings ADD COLUMN kind TEXT NOT NULL DEFAULT 'record'")
                db.execSQL("ALTER TABLE recordings ADD COLUMN waveform TEXT")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "echo.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
    }
}
