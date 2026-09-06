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
}
