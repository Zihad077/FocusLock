package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val dailyLimitMinutes: Int, 
    val sessionLimitMinutes: Int? = null,
    
    // Adaptive Limits
    val suggestedDailyLimitMinutes: Int? = null,
    
    // Weekly Planning overrides
    val mondayLimitMinutes: Int? = null,
    val tuesdayLimitMinutes: Int? = null,
    val wednesdayLimitMinutes: Int? = null,
    val thursdayLimitMinutes: Int? = null,
    val fridayLimitMinutes: Int? = null,
    val saturdayLimitMinutes: Int? = null,
    val sundayLimitMinutes: Int? = null
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

@Entity(tableName = "usage_events")
data class UsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val dateString: String
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean,
    val mode: String = "Deep Work",
    val profileId: Int? = null,
    val journalEntry: String? = null
)

@Entity(tableName = "focus_profiles")
data class FocusProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String = "Study",
    val focusDuration: Int = 25,
    val isBedtime: Boolean = false,
    
    // Location Trigger
    val isLocationEnabled: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float? = null,
    
    // Automation
    val isActive: Boolean = false
)

@Entity(tableName = "focus_profile_apps")
data class FocusProfileApp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val packageName: String
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
    val tempUnlockOptions: String = "5,10,15",
    
    // Escape Prevention Settings
    val notificationProtectionEnabled: Boolean = false,
    val antiDeleteProtectionEnabled: Boolean = false,
    val stableLockModeEnabled: Boolean = true,
    val permissionProtectionEnabled: Boolean = true,
    val escapeAttemptDetectionEnabled: Boolean = true,
    val autoServiceRecoveryEnabled: Boolean = true,
    val focusProtectionEnabled: Boolean = true,
    
    // Bedtime Mode
    val bedtimeEnabled: Boolean = false,
    val bedtimeStartMinuteOfDay: Int = 1380, // 23:00
    val bedtimeEndMinuteOfDay: Int = 420,    // 07:00
    val isBedtimeActive: Boolean = false,
    
    // Distraction-Free Focus
    val distractionFreeFocusEnabled: Boolean = true,

    // Premium status (Zero ads & unlimited unlocks when true)
    val isPremium: Boolean = false
)

@Entity(tableName = "escape_attempts")
data class EscapeAttempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String?,
    val attemptTime: Long = System.currentTimeMillis(),
    val type: String // e.g. "SERVICE_STOP", "UNINSTALL_ATTEMPT", "PERMISSION_REVOKE"
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
