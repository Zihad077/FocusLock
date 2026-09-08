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
}
