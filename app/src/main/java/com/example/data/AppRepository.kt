package com.example.data

import com.example.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(private val focusDao: FocusDao) {
    
    val allLimits: Flow<List<AppLimit>> = focusDao.getAllLimits()
    
    suspend fun getLimit(packageName: String): AppLimit? = focusDao.getLimit(packageName)
    
    suspend fun insertLimit(limit: AppLimit) {
        focusDao.insertLimit(limit)
    }
    
    suspend fun deleteLimit(packageName: String) {
        focusDao.deleteLimit(packageName)
    }
    
    fun getSchedulesForApp(packageName: String): Flow<List<AppSchedule>> = focusDao.getSchedulesForApp(packageName)
    
    suspend fun insertSchedule(schedule: AppSchedule) {
        focusDao.insertSchedule(schedule)
    }
    
    suspend fun deleteSchedule(id: Int) {
        focusDao.deleteSchedule(id)
    }
    
    fun getUsageForDate(dateString: String): Flow<List<DailyUsage>> = focusDao.getUsageForDate(dateString)
    
    fun getAllUsage(): Flow<List<DailyUsage>> = focusDao.getAllUsage()
    
    suspend fun getUsage(packageName: String, dateString: String): DailyUsage? = focusDao.getUsage(packageName, dateString)
    
    suspend fun insertUsage(usage: DailyUsage) {
        focusDao.insertUsage(usage)
    }

    // Usage Events
    fun getUsageEventsForDate(dateString: String): Flow<List<UsageEvent>> = focusDao.getUsageEventsForDate(dateString)

    suspend fun insertUsageEvent(event: UsageEvent) {
        focusDao.insertUsageEvent(event)
    }
    
    suspend fun insertFocusSession(session: FocusSession) {
        focusDao.insertFocusSession(session)
    }
    
    val allFocusSessions: Flow<List<FocusSession>> = focusDao.getAllFocusSessions()
    
    // Focus Profiles
    val allFocusProfiles: Flow<List<FocusProfile>> = focusDao.getAllFocusProfiles()

    suspend fun getActiveFocusProfiles(): List<FocusProfile> = focusDao.getActiveFocusProfiles()

    suspend fun getFocusProfile(id: Int): FocusProfile? = focusDao.getFocusProfile(id)

    suspend fun insertFocusProfile(profile: FocusProfile): Long = focusDao.insertFocusProfile(profile)

    suspend fun deleteFocusProfile(id: Int) {
        focusDao.deleteFocusProfile(id)
    }

    fun getAppsForProfile(profileId: Int): Flow<List<FocusProfileApp>> = focusDao.getAppsForProfile(profileId)

    suspend fun getAppsListForProfile(profileId: Int): List<FocusProfileApp> = focusDao.getAppsListForProfile(profileId)

    suspend fun insertFocusProfileApp(app: FocusProfileApp) {
        focusDao.insertFocusProfileApp(app)
    }

    suspend fun deleteAppsForProfile(profileId: Int) {
        focusDao.deleteAppsForProfile(profileId)
    }

    val userSettings: Flow<UserSettings> = focusDao.getUserSettings().map { it ?: UserSettings() }
    
    suspend fun updateSettings(settings: UserSettings) {
        focusDao.updateSettings(settings)
    }

    suspend fun getTemporaryUnlocks(packageName: String) = focusDao.getTemporaryUnlocks(packageName)
    
    val allTemporaryUnlocks: Flow<List<TemporaryUnlock>> = focusDao.getAllTemporaryUnlocks()

    suspend fun insertTemporaryUnlock(unlock: TemporaryUnlock) {
        focusDao.insertTemporaryUnlock(unlock)
    }

    suspend fun deleteTemporaryUnlock(id: Int) {
        focusDao.deleteTemporaryUnlock(id)
    }

    // Goals
    val allGoals: Flow<List<Goal>> = focusDao.getAllGoals()
    
    suspend fun insertGoal(goal: Goal) {
        focusDao.insertGoal(goal)
    }

    suspend fun deleteGoal(id: Int) {
        focusDao.deleteGoal(id)
    }

    // Achievements
    val allAchievements: Flow<List<Achievement>> = focusDao.getAllAchievements()

    suspend fun insertAchievement(achievement: Achievement) {
        focusDao.insertAchievement(achievement)
    }
    
    suspend fun insertAchievements(achievements: List<Achievement>) {
        focusDao.insertAchievements(achievements)
    }

    // App Groups
    val allAppGroups: Flow<List<AppGroup>> = focusDao.getAllAppGroups()

    suspend fun insertAppGroup(group: AppGroup) {
        focusDao.insertAppGroup(group)
    }

    suspend fun deleteAppGroup(id: Int) {
        focusDao.deleteAppGroup(id)
    }
    
    fun getAppGroupMembers(groupId: Int): Flow<List<AppGroupMember>> = focusDao.getAppGroupMembers(groupId)
    
    suspend fun insertAppGroupMember(member: AppGroupMember) {
        focusDao.insertAppGroupMember(member)
    }

    suspend fun deleteAppGroupMember(id: Int) {
        focusDao.deleteAppGroupMember(id)
    }

    // Escape Attempts
    val allEscapeAttempts: Flow<List<EscapeAttempt>> = focusDao.getAllEscapeAttempts()

    suspend fun insertEscapeAttempt(attempt: EscapeAttempt) {
        focusDao.insertEscapeAttempt(attempt)
    }

    // Data Backup & Restore
    val allSchedules: Flow<List<AppSchedule>> = focusDao.getAllSchedules()
    val allUsageEvents: Flow<List<UsageEvent>> = focusDao.getAllUsageEvents()
    val allFocusProfileApps: Flow<List<FocusProfileApp>> = focusDao.getAllFocusProfileApps()

    suspend fun restoreAllData(
        settings: UserSettings?,
        limits: List<AppLimit>,
        schedules: List<AppSchedule>,
        dailyUsages: List<DailyUsage>,
        usageEvents: List<UsageEvent>,
        sessions: List<FocusSession>,
        profiles: List<FocusProfile>,
        profileApps: List<FocusProfileApp>,
        goals: List<Goal>,
        achievements: List<Achievement>,
        groups: List<AppGroup>,
        groupMembers: List<AppGroupMember>
    ) {
        // Clear old tables
        focusDao.clearLimits()
        focusDao.clearSchedules()
        focusDao.clearDailyUsage()
        focusDao.clearUsageEvents()
        focusDao.clearFocusSessions()
        focusDao.clearFocusProfiles()
        focusDao.clearFocusProfileApps()
        focusDao.clearGoals()
        focusDao.clearAchievements()
        focusDao.clearAppGroups()
        focusDao.clearAppGroupMembers()

        // Batch insert new records
        if (settings != null) {
            focusDao.updateSettings(settings)
        }
        if (limits.isNotEmpty()) focusDao.insertLimits(limits)
        if (schedules.isNotEmpty()) focusDao.insertSchedules(schedules)
        if (dailyUsages.isNotEmpty()) focusDao.insertDailyUsages(dailyUsages)
        if (usageEvents.isNotEmpty()) focusDao.insertUsageEvents(usageEvents)
        if (sessions.isNotEmpty()) focusDao.insertFocusSessions(sessions)
        if (profiles.isNotEmpty()) focusDao.insertFocusProfiles(profiles)
        if (profileApps.isNotEmpty()) focusDao.insertFocusProfileApps(profileApps)
        if (goals.isNotEmpty()) focusDao.insertGoals(goals)
        if (achievements.isNotEmpty()) focusDao.insertAchievements(achievements)
        if (groups.isNotEmpty()) focusDao.insertAppGroups(groups)
        if (groupMembers.isNotEmpty()) focusDao.insertAppGroupMembers(groupMembers)
    }
}
