package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 9, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun focusDao(): FocusDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focuslock_database"
                )
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
            val defaultAchievements = listOf(
                Achievement(id = "first_step", title = "First Step", description = "Complete your first focus session", isUnlocked = false, xpReward = 50),
                Achievement(id = "deep_diver", title = "Deep Diver", description = "Focus for 2 hours in a single session", isUnlocked = false, xpReward = 150),
                Achievement(id = "iron_will", title = "Iron Will", description = "Resist opening a blocked app 10 times", isUnlocked = false, xpReward = 200),
                Achievement(id = "consistency", title = "Consistency", description = "Achieve a 7-day streak", isUnlocked = false, xpReward = 300)
            )
            dao.insertAchievements(defaultAchievements)
            
            // Add default profiles
            try {
                val profiles = dao.getAllFocusProfiles()
                // Just let user add them, or we could prepopulate
            } catch (e: Exception) {}
        }
    }
}
