package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val dailyLimitMinutes: Int, 
    val sessionLimitMinutes: Int? = null 
)

@Entity(tableName = "app_schedules")
data class AppSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val startTimeMinuteOfDay: Int, 
    val endTimeMinuteOfDay: Int,   
    val daysOfWeek: String 
)

@Entity(tableName = "daily_usage")
data class DailyUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val dateString: String, 
    val usedMinutes: Int
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean,
    val mode: String = "Deep Work"
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val theme: String = "SYSTEM",
    val language: String = "en",
    val focusScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val consecutiveSuccessfulDays: Int = 0,
    val emergencyUnlocksRemaining: Int = 2,
    val maxEmergencyUnlocks: Int = 2,
    val isFocusModeActive: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    val lastResetDateString: String = "",
    val activeFocusEndTime: Long = 0L,
    
    // Verification Settings
    val mindChallengeEnabled: Boolean = true,
    val focusChallengeEnabled: Boolean = true,
    val typingChallengeEnabled: Boolean = true,
    val pinUnlockEnabled: Boolean = false,
    val pinHash: String = "",
    val difficulty: String = "NORMAL", // EASY, NORMAL, HARD
    val tempUnlockDurationMinutes: Int = 5,
    val tempUnlockOptions: String = "5,10,15"
)

@Entity(tableName = "temporary_unlocks")
data class TemporaryUnlock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val type: String, // "MIND", "FOCUS", "TYPING", "PIN", "EMERGENCY"
    val startTime: Long,
    val durationMinutes: Int
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "USAGE_LIMIT", "FOCUS_TIME", "PRODUCTIVITY"
    val targetValue: Int,
    val currentValue: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val xpReward: Int
)

@Entity(tableName = "app_groups")
data class AppGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: Int? = null
)

@Entity(tableName = "app_group_members")
data class AppGroupMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val packageName: String
)

