package com.example.data

import com.example.database.AppLimit
import com.example.database.AppSchedule
import com.example.database.DailyUsage
import com.example.database.FocusDao
import com.example.database.FocusSession
import com.example.database.UserSettings
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
    
    suspend fun insertFocusSession(session: FocusSession) {
        focusDao.insertFocusSession(session)
    }
    
    val allFocusSessions: Flow<List<FocusSession>> = focusDao.getAllFocusSessions()
    
    val userSettings: Flow<UserSettings> = focusDao.getUserSettings().map { it ?: UserSettings() }
    
    suspend fun updateSettings(settings: UserSettings) {
        focusDao.updateSettings(settings)
    }

    suspend fun getTemporaryUnlocks(packageName: String) = focusDao.getTemporaryUnlocks(packageName)
    
    val allTemporaryUnlocks: Flow<List<com.example.database.TemporaryUnlock>> = focusDao.getAllTemporaryUnlocks()

    suspend fun insertTemporaryUnlock(unlock: com.example.database.TemporaryUnlock) {
        focusDao.insertTemporaryUnlock(unlock)
    }

    suspend fun deleteTemporaryUnlock(id: Int) {
        focusDao.deleteTemporaryUnlock(id)
    }

    // Goals
    val allGoals: Flow<List<com.example.database.Goal>> = focusDao.getAllGoals()
    
    suspend fun insertGoal(goal: com.example.database.Goal) {
        focusDao.insertGoal(goal)
    }

    suspend fun deleteGoal(id: Int) {
        focusDao.deleteGoal(id)
    }

    // Achievements
    val allAchievements: Flow<List<com.example.database.Achievement>> = focusDao.getAllAchievements()

    suspend fun insertAchievement(achievement: com.example.database.Achievement) {
        focusDao.insertAchievement(achievement)
    }
    
    suspend fun insertAchievements(achievements: List<com.example.database.Achievement>) {
        focusDao.insertAchievements(achievements)
    }

    // App Groups
    val allAppGroups: Flow<List<com.example.database.AppGroup>> = focusDao.getAllAppGroups()

    suspend fun insertAppGroup(group: com.example.database.AppGroup) {
        focusDao.insertAppGroup(group)
    }

    suspend fun deleteAppGroup(id: Int) {
        focusDao.deleteAppGroup(id)
    }
    
    fun getAppGroupMembers(groupId: Int): Flow<List<com.example.database.AppGroupMember>> = focusDao.getAppGroupMembers(groupId)
    
    suspend fun insertAppGroupMember(member: com.example.database.AppGroupMember) {
        focusDao.insertAppGroupMember(member)
    }

    suspend fun deleteAppGroupMember(id: Int) {
        focusDao.deleteAppGroupMember(id)
    }
}
