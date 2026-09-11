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
    
    // Usage Events (Timeline)
    @Query("SELECT * FROM usage_events WHERE dateString = :dateString ORDER BY startTime ASC")
    fun getUsageEventsForDate(dateString: String): Flow<List<UsageEvent>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageEvent(event: UsageEvent)
    
    // Focus Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSession)
    
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllFocusSessions(): Flow<List<FocusSession>>
    
    // Focus Profiles
    @Query("SELECT * FROM focus_profiles")
    fun getAllFocusProfiles(): Flow<List<FocusProfile>>
    
    @Query("SELECT * FROM focus_profiles WHERE isActive = 1")
    suspend fun getActiveFocusProfiles(): List<FocusProfile>

    @Query("SELECT * FROM focus_profiles WHERE id = :id")
    suspend fun getFocusProfile(id: Int): FocusProfile?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusProfile(profile: FocusProfile): Long
    
    @Query("DELETE FROM focus_profiles WHERE id = :id")
    suspend fun deleteFocusProfile(id: Int)
    
    @Query("SELECT * FROM focus_profile_apps WHERE profileId = :profileId")
    fun getAppsForProfile(profileId: Int): Flow<List<FocusProfileApp>>

    @Query("SELECT * FROM focus_profile_apps WHERE profileId = :profileId")
    suspend fun getAppsListForProfile(profileId: Int): List<FocusProfileApp>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusProfileApp(app: FocusProfileApp)
    
    @Query("DELETE FROM focus_profile_apps WHERE profileId = :profileId")
    suspend fun deleteAppsForProfile(profileId: Int)
    
    // Settings
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: UserSettings)

    // Temporary Unlocks
    @Query("SELECT * FROM temporary_unlocks WHERE packageName = :packageName")
    suspend fun getTemporaryUnlocks(packageName: String): List<TemporaryUnlock>
    
    @Query("SELECT * FROM temporary_unlocks")
    fun getAllTemporaryUnlocks(): Flow<List<TemporaryUnlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemporaryUnlock(unlock: TemporaryUnlock)

    @Query("DELETE FROM temporary_unlocks WHERE id = :id")
    suspend fun deleteTemporaryUnlock(id: Int)

    // Goals
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Int)

    // Achievements
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    // App Groups
    @Query("SELECT * FROM app_groups")
    fun getAllAppGroups(): Flow<List<AppGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppGroup(group: AppGroup)

    @Query("DELETE FROM app_groups WHERE id = :id")
    suspend fun deleteAppGroup(id: Int)

    @Query("SELECT * FROM app_group_members WHERE groupId = :groupId")
    fun getAppGroupMembers(groupId: Int): Flow<List<AppGroupMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppGroupMember(member: AppGroupMember)

    @Query("DELETE FROM app_group_members WHERE id = :id")
    suspend fun deleteAppGroupMember(id: Int)

    // Escape Attempts
    @Query("SELECT * FROM escape_attempts ORDER BY attemptTime DESC")
    fun getAllEscapeAttempts(): Flow<List<EscapeAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEscapeAttempt(attempt: EscapeAttempt)

    // Data Backup & Restore queries
    @Query("SELECT * FROM app_schedules")
    fun getAllSchedules(): Flow<List<AppSchedule>>

    @Query("SELECT * FROM usage_events")
    fun getAllUsageEvents(): Flow<List<UsageEvent>>

    @Query("SELECT * FROM focus_profile_apps")
    fun getAllFocusProfileApps(): Flow<List<FocusProfileApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimits(limits: List<AppLimit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<AppSchedule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyUsages(usages: List<DailyUsage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageEvents(events: List<UsageEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusProfiles(profiles: List<FocusProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusProfileApps(profileApps: List<FocusProfileApp>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppGroups(groups: List<AppGroup>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppGroupMembers(members: List<AppGroupMember>)

    @Query("DELETE FROM app_limits")
    suspend fun clearLimits()

    @Query("DELETE FROM app_schedules")
    suspend fun clearSchedules()

    @Query("DELETE FROM daily_usage")
    suspend fun clearDailyUsage()

    @Query("DELETE FROM usage_events")
    suspend fun clearUsageEvents()

    @Query("DELETE FROM focus_sessions")
    suspend fun clearFocusSessions()

    @Query("DELETE FROM focus_profiles")
    suspend fun clearFocusProfiles()

    @Query("DELETE FROM focus_profile_apps")
    suspend fun clearFocusProfileApps()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements()

    @Query("DELETE FROM app_groups")
    suspend fun clearAppGroups()

    @Query("DELETE FROM app_group_members")
    suspend fun clearAppGroupMembers()
}
