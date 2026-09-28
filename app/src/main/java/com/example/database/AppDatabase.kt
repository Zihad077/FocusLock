package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppLimit::class, 
        AppSchedule::class, 
        DailyUsage::class, 
        FocusSession::class, 
        UserSettings::class,
        TemporaryUnlock::class,
        Goal::class,
        Achievement::class,
        AppGroup::class,
        AppGroupMember::class,
        EscapeAttempt::class,
        UsageEvent::class,
        FocusProfile::class,
        FocusProfileApp::class
    ], 
    version = 12, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun focusDao(): FocusDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE user_settings ADD COLUMN premiumExpiryTimestamp INTEGER NOT NULL DEFAULT 0") } catch(e: Exception) {}
                try { db.execSQL("ALTER TABLE user_settings ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0") } catch(e: Exception) {}
                try { db.execSQL("ALTER TABLE user_settings ADD COLUMN distractionFreeFocusEnabled INTEGER NOT NULL DEFAULT 1") } catch(e: Exception) {}
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE user_settings ADD COLUMN premiumPlanId TEXT NOT NULL DEFAULT ''") } catch(e: Exception) {}
                try { db.execSQL("ALTER TABLE user_settings ADD COLUMN premiumTxId TEXT NOT NULL DEFAULT ''") } catch(e: Exception) {}
                try { db.execSQL("ALTER TABLE user_settings ADD COLUMN premiumActivationTime INTEGER NOT NULL DEFAULT 0") } catch(e: Exception) {}
                try { db.execSQL("ALTER TABLE user_settings ADD COLUMN focusSessionStartTime INTEGER NOT NULL DEFAULT 0") } catch(e: Exception) {}
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focuslock_database"
                )
                .addMigrations(MIGRATION_9_10, MIGRATION_11_12)
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.focusDao())
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.focusDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: FocusDao) {
            val defaultAchievements = com.example.util.StreakAndAchievementManager.defaultCatalog()
            dao.insertAchievements(defaultAchievements)

            // Add default profiles
            try {
                val profiles = dao.getAllFocusProfiles()
                // Just let user add them, or we could prepopulate
            } catch (e: Exception) {}
        }
    }
}
