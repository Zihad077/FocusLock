package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    
    // Limits
    @Query("SELECT * FROM app_limits")
    fun getAllLimits(): Flow<List<AppLimit>>
    
    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getLimit(packageName: String): AppLimit?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimit(limit: AppLimit)
    
    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteLimit(packageName: String)
    
    // Schedules
    @Query("SELECT * FROM app_schedules WHERE packageName = :packageName")
    fun getSchedulesForApp(packageName: String): Flow<List<AppSchedule>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: AppSchedule)
    
    @Query("DELETE FROM app_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: Int)
    
    // Usage
    @Query("SELECT * FROM daily_usage WHERE dateString = :dateString")
    fun getUsageForDate(dateString: String): Flow<List<DailyUsage>>
    
    @Query("SELECT * FROM daily_usage")
    fun getAllUsage(): Flow<List<DailyUsage>>
    
    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName AND dateString = :dateString")
    suspend fun getUsage(packageName: String, dateString: String): DailyUsage?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: DailyUsage)
    
    // Focus Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSession)
    
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllFocusSessions(): Flow<List<FocusSession>>
    
    // Settings
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: UserSettings)

    // Temporary Unlocks
    @Query("SELECT * FROM temporary_unlocks WHERE packageName = :packageName")
    suspend fun getTemporaryUnlocks(packageName: String): List<TemporaryUnlock>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemporaryUnlock(unlock: TemporaryUnlock)

    @Query("DELETE FROM temporary_unlocks WHERE id = :id")
    suspend fun deleteTemporaryUnlock(id: Int)
}
