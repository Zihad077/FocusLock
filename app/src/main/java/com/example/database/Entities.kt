package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val dailyLimitMinutes: Int, // Total daily limit in minutes
    val sessionLimitMinutes: Int? = null // Optional continuous session limit in minutes
)

@Entity(tableName = "app_schedules")
data class AppSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val startTimeMinuteOfDay: Int, // e.g., 22 * 60 = 1320 (10:00 PM)
    val endTimeMinuteOfDay: Int,   // e.g., 7 * 60 = 420 (7:00 AM)
    val daysOfWeek: String // comma separated, e.g., "1,2,3,4,5"
)

@Entity(tableName = "daily_usage")
data class DailyUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val dateString: String, // e.g., "2023-10-25"
    val usedMinutes: Int
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // Single row
    val theme: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val language: String = "en",
    val focusScore: Int = 0,
    val currentStreak: Int = 0,
    val emergencyUnlocksRemaining: Int = 2,
    val maxEmergencyUnlocks: Int = 2,
    val isFocusModeActive: Boolean = false
)
