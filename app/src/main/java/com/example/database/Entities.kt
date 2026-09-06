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
    val emergencyUnlocksRemaining: Int = 2,
    val maxEmergencyUnlocks: Int = 2,
    val isFocusModeActive: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    val lastResetDateString: String = "",
    val activeFocusEndTime: Long = 0L // 0 if no active focus session
)

@Entity(tableName = "temporary_unlocks")
data class TemporaryUnlock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val type: String, // "CHALLENGE" or "EMERGENCY"
    val startTime: Long,
    val durationMinutes: Int
)

