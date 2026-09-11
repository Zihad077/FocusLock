package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.AppRepository
import com.example.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataBackupManager {

    private const val BACKUP_VERSION = 1
    private const val BACKUP_MAGIC = "FOCUSLOCK_BACKUP"

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val summary: BackupSummary? = null,
        val parsedBackup: FocusLockBackup? = null
    )

    data class BackupSummary(
        val createdAt: String,
        val version: Int,
        val limitsCount: Int,
        val schedulesCount: Int,
        val focusSessionsCount: Int,
        val usageRecordsCount: Int,
        val profilesCount: Int,
        val goalsCount: Int,
        val level: Int,
        val xp: Int,
        val streak: Int
    )

    data class FocusLockBackup(
        val version: Int,
        val magic: String,
        val timestamp: Long,
        val settings: UserSettings?,
        val limits: List<AppLimit>,
        val schedules: List<AppSchedule>,
        val dailyUsages: List<DailyUsage>,
        val usageEvents: List<UsageEvent>,
        val sessions: List<FocusSession>,
        val profiles: List<FocusProfile>,
        val profileApps: List<FocusProfileApp>,
        val goals: List<Goal>,
        val achievements: List<Achievement>,
        val groups: List<AppGroup>,
        val groupMembers: List<AppGroupMember>
    )

    suspend fun createBackupJson(repository: AppRepository): String = withContext(Dispatchers.IO) {
        val settings = repository.userSettings.first()
        val limits = repository.allLimits.first()
        val schedules = repository.allSchedules.first()
        val dailyUsages = repository.getAllUsage().first()
        val usageEvents = repository.allUsageEvents.first()
        val sessions = repository.allFocusSessions.first()
        val profiles = repository.allFocusProfiles.first()
        val profileApps = repository.allFocusProfileApps.first()
        val goals = repository.allGoals.first()
        val achievements = repository.allAchievements.first()
        val groups = repository.allAppGroups.first()
        val groupMembers = repository.allAppGroups.first().flatMap { group ->
            repository.getAppGroupMembers(group.id).first()
        }

        val root = JSONObject()
        root.put("magic", BACKUP_MAGIC)
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())

        // User Settings
        val settingsObj = JSONObject().apply {
            put("theme", settings.theme)
            put("language", settings.language)
            put("focusScore", settings.focusScore)
            put("currentStreak", settings.currentStreak)
            put("bestStreak", settings.bestStreak)
            put("consecutiveSuccessfulDays", settings.consecutiveSuccessfulDays)
            put("emergencyUnlocksRemaining", settings.emergencyUnlocksRemaining)
            put("maxEmergencyUnlocks", settings.maxEmergencyUnlocks)
            put("xp", settings.xp)
            put("level", settings.level)
            put("mindChallengeEnabled", settings.mindChallengeEnabled)
            put("focusChallengeEnabled", settings.focusChallengeEnabled)
            put("typingChallengeEnabled", settings.typingChallengeEnabled)
            put("pinUnlockEnabled", settings.pinUnlockEnabled)
            put("pinHash", settings.pinHash)
            put("difficulty", settings.difficulty)
            put("tempUnlockDurationMinutes", settings.tempUnlockDurationMinutes)
            put("notificationProtectionEnabled", settings.notificationProtectionEnabled)
            put("antiDeleteProtectionEnabled", settings.antiDeleteProtectionEnabled)
            put("stableLockModeEnabled", settings.stableLockModeEnabled)
            put("permissionProtectionEnabled", settings.permissionProtectionEnabled)
            put("escapeAttemptDetectionEnabled", settings.escapeAttemptDetectionEnabled)
            put("autoServiceRecoveryEnabled", settings.autoServiceRecoveryEnabled)
            put("focusProtectionEnabled", settings.focusProtectionEnabled)
            put("bedtimeEnabled", settings.bedtimeEnabled)
            put("bedtimeStartMinuteOfDay", settings.bedtimeStartMinuteOfDay)
            put("bedtimeEndMinuteOfDay", settings.bedtimeEndMinuteOfDay)
            put("distractionFreeFocusEnabled", settings.distractionFreeFocusEnabled)
            put("isPremium", settings.isPremium)
        }
        root.put("settings", settingsObj)

        // Limits
        val limitsArr = JSONArray()
        limits.forEach { limit ->
            limitsArr.put(JSONObject().apply {
                put("packageName", limit.packageName)
                put("appName", limit.appName)
                put("isEnabled", limit.isEnabled)
                put("dailyLimitMinutes", limit.dailyLimitMinutes)
                limit.sessionLimitMinutes?.let { put("sessionLimitMinutes", it) }
                limit.suggestedDailyLimitMinutes?.let { put("suggestedDailyLimitMinutes", it) }
                limit.mondayLimitMinutes?.let { put("mondayLimitMinutes", it) }
                limit.tuesdayLimitMinutes?.let { put("tuesdayLimitMinutes", it) }
                limit.wednesdayLimitMinutes?.let { put("wednesdayLimitMinutes", it) }
                limit.thursdayLimitMinutes?.let { put("thursdayLimitMinutes", it) }
                limit.fridayLimitMinutes?.let { put("fridayLimitMinutes", it) }
                limit.saturdayLimitMinutes?.let { put("saturdayLimitMinutes", it) }
                limit.sundayLimitMinutes?.let { put("sundayLimitMinutes", it) }
            })
        }
        root.put("limits", limitsArr)

        // Schedules
        val schedulesArr = JSONArray()
        schedules.forEach { s ->
            schedulesArr.put(JSONObject().apply {
                put("packageName", s.packageName)
                put("startTimeMinuteOfDay", s.startTimeMinuteOfDay)
                put("endTimeMinuteOfDay", s.endTimeMinuteOfDay)
                put("daysOfWeek", s.daysOfWeek)
            })
        }
        root.put("schedules", schedulesArr)

        // Daily Usage
        val usagesArr = JSONArray()
        dailyUsages.forEach { u ->
            usagesArr.put(JSONObject().apply {
                put("packageName", u.packageName)
                put("dateString", u.dateString)
                put("usedMinutes", u.usedMinutes)
            })
        }
        root.put("dailyUsages", usagesArr)

        // Usage Events
        val eventsArr = JSONArray()
        usageEvents.forEach { e ->
            eventsArr.put(JSONObject().apply {
                put("packageName", e.packageName)
                put("startTime", e.startTime)
                put("endTime", e.endTime)
                put("durationMinutes", e.durationMinutes)
                put("dateString", e.dateString)
            })
        }
        root.put("usageEvents", eventsArr)

        // Focus Sessions
        val sessionsArr = JSONArray()
        sessions.forEach { sess ->
            sessionsArr.put(JSONObject().apply {
                put("startTime", sess.startTime)
                put("durationMinutes", sess.durationMinutes)
                put("isCompleted", sess.isCompleted)
                put("mode", sess.mode)
                sess.profileId?.let { put("profileId", it) }
                sess.journalEntry?.let { put("journalEntry", it) }
            })
        }
        root.put("sessions", sessionsArr)

        // Focus Profiles
        val profilesArr = JSONArray()
        profiles.forEach { p ->
            profilesArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("icon", p.icon)
                put("focusDuration", p.focusDuration)
                put("isBedtime", p.isBedtime)
                put("isLocationEnabled", p.isLocationEnabled)
                p.latitude?.let { put("latitude", it) }
                p.longitude?.let { put("longitude", it) }
                p.radiusMeters?.let { put("radiusMeters", it) }
                put("isActive", p.isActive)
            })
        }
        root.put("profiles", profilesArr)

        // Profile Apps
        val profileAppsArr = JSONArray()
        profileApps.forEach { pa ->
            profileAppsArr.put(JSONObject().apply {
                put("profileId", pa.profileId)
                put("packageName", pa.packageName)
            })
        }
        root.put("profileApps", profileAppsArr)

        // Goals
        val goalsArr = JSONArray()
        goals.forEach { g ->
            goalsArr.put(JSONObject().apply {
                put("title", g.title)
                put("type", g.type)
                put("targetValue", g.targetValue)
                put("currentValue", g.currentValue)
                put("isCompleted", g.isCompleted)
                put("createdAt", g.createdAt)
            })
        }
        root.put("goals", goalsArr)

        // Achievements
        val achievementsArr = JSONArray()
        achievements.forEach { a ->
            achievementsArr.put(JSONObject().apply {
                put("id", a.id)
                put("title", a.title)
                put("description", a.description)
                put("isUnlocked", a.isUnlocked)
                a.unlockedAt?.let { put("unlockedAt", it) }
                put("xpReward", a.xpReward)
            })
        }
        root.put("achievements", achievementsArr)

        // App Groups
        val groupsArr = JSONArray()
        groups.forEach { grp ->
            groupsArr.put(JSONObject().apply {
                put("id", grp.id)
                put("name", grp.name)
                grp.color?.let { put("color", it) }
            })
        }
        root.put("groups", groupsArr)

        // App Group Members
        val membersArr = JSONArray()
        groupMembers.forEach { mem ->
            membersArr.put(JSONObject().apply {
                put("groupId", mem.groupId)
                put("packageName", mem.packageName)
            })
        }
        root.put("groupMembers", membersArr)

        root.toString(2)
    }

    suspend fun exportBackupFile(context: Context, repository: AppRepository): File = withContext(Dispatchers.IO) {
        val json = createBackupJson(repository)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date())
        val fileName = "focuslock_backup_$timestamp.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(json, Charsets.UTF_8)
        file
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FocusLock Complete Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export FocusLock Backup"))
    }

    suspend fun validateBackupFromUri(context: Context, uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext ValidationResult(isValid = false, errorMessage = "Could not open selected backup file.")

            val rawJson = stringBuilder.toString()
            if (rawJson.isBlank()) {
                return@withContext ValidationResult(isValid = false, errorMessage = "The selected file is empty.")
            }

            val root = try {
                JSONObject(rawJson)
            } catch (e: Exception) {
                return@withContext ValidationResult(isValid = false, errorMessage = "File is not a valid JSON document.")
            }

            val magic = root.optString("magic", "")
            if (magic != BACKUP_MAGIC) {
                return@withContext ValidationResult(isValid = false, errorMessage = "File is not a valid FocusLock backup package.")
            }

            val version = root.optInt("version", 0)
            if (version > BACKUP_VERSION || version <= 0) {
                return@withContext ValidationResult(isValid = false, errorMessage = "Unsupported backup version ($version).")
            }

            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            // Parse UserSettings
            var parsedSettings: UserSettings? = null
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                parsedSettings = UserSettings(
                    id = 1,
                    theme = s.optString("theme", "SYSTEM"),
                    language = s.optString("language", "en"),
                    focusScore = s.optInt("focusScore", 0),
                    currentStreak = s.optInt("currentStreak", 0),
                    bestStreak = s.optInt("bestStreak", 0),
                    consecutiveSuccessfulDays = s.optInt("consecutiveSuccessfulDays", 0),
                    emergencyUnlocksRemaining = s.optInt("emergencyUnlocksRemaining", 2),
                    maxEmergencyUnlocks = s.optInt("maxEmergencyUnlocks", 2),
                    isFocusModeActive = false,
                    xp = s.optInt("xp", 0),
                    level = s.optInt("level", 1).coerceAtLeast(1),
                    mindChallengeEnabled = s.optBoolean("mindChallengeEnabled", true),
                    focusChallengeEnabled = s.optBoolean("focusChallengeEnabled", true),
                    typingChallengeEnabled = s.optBoolean("typingChallengeEnabled", true),
                    pinUnlockEnabled = s.optBoolean("pinUnlockEnabled", false),
                    pinHash = s.optString("pinHash", ""),
                    difficulty = s.optString("difficulty", "NORMAL"),
                    tempUnlockDurationMinutes = s.optInt("tempUnlockDurationMinutes", 5),
                    notificationProtectionEnabled = s.optBoolean("notificationProtectionEnabled", false),
                    antiDeleteProtectionEnabled = s.optBoolean("antiDeleteProtectionEnabled", false),
                    stableLockModeEnabled = s.optBoolean("stableLockModeEnabled", true),
                    permissionProtectionEnabled = s.optBoolean("permissionProtectionEnabled", true),
                    escapeAttemptDetectionEnabled = s.optBoolean("escapeAttemptDetectionEnabled", true),
                    autoServiceRecoveryEnabled = s.optBoolean("autoServiceRecoveryEnabled", true),
                    focusProtectionEnabled = s.optBoolean("focusProtectionEnabled", true),
                    bedtimeEnabled = s.optBoolean("bedtimeEnabled", false),
                    bedtimeStartMinuteOfDay = s.optInt("bedtimeStartMinuteOfDay", 1380),
                    bedtimeEndMinuteOfDay = s.optInt("bedtimeEndMinuteOfDay", 420),
                    distractionFreeFocusEnabled = s.optBoolean("distractionFreeFocusEnabled", true),
                    isPremium = s.optBoolean("isPremium", false)
                )
            }

            // Parse Limits
            val limitsList = mutableListOf<AppLimit>()
            val limitsArr = root.optJSONArray("limits")
            if (limitsArr != null) {
                for (i in 0 until limitsArr.length()) {
                    val obj = limitsArr.getJSONObject(i)
                    val pkg = obj.optString("packageName", "")
                    if (pkg.isNotEmpty()) {
                        limitsList.add(
                            AppLimit(
                                packageName = pkg,
                                appName = obj.optString("appName", pkg),
                                isEnabled = obj.optBoolean("isEnabled", true),
                                dailyLimitMinutes = obj.optInt("dailyLimitMinutes", 30),
                                sessionLimitMinutes = if (obj.has("sessionLimitMinutes")) obj.optInt("sessionLimitMinutes") else null,
                                suggestedDailyLimitMinutes = if (obj.has("suggestedDailyLimitMinutes")) obj.optInt("suggestedDailyLimitMinutes") else null,
                                mondayLimitMinutes = if (obj.has("mondayLimitMinutes")) obj.optInt("mondayLimitMinutes") else null,
                                tuesdayLimitMinutes = if (obj.has("tuesdayLimitMinutes")) obj.optInt("tuesdayLimitMinutes") else null,
                                wednesdayLimitMinutes = if (obj.has("wednesdayLimitMinutes")) obj.optInt("wednesdayLimitMinutes") else null,
                                thursdayLimitMinutes = if (obj.has("thursdayLimitMinutes")) obj.optInt("thursdayLimitMinutes") else null,
                                fridayLimitMinutes = if (obj.has("fridayLimitMinutes")) obj.optInt("fridayLimitMinutes") else null,
                                saturdayLimitMinutes = if (obj.has("saturdayLimitMinutes")) obj.optInt("saturdayLimitMinutes") else null,
                                sundayLimitMinutes = if (obj.has("sundayLimitMinutes")) obj.optInt("sundayLimitMinutes") else null
                            )
                        )
                    }
                }
            }

            // Parse Schedules
            val schedulesList = mutableListOf<AppSchedule>()
            val schedulesArr = root.optJSONArray("schedules")
            if (schedulesArr != null) {
                for (i in 0 until schedulesArr.length()) {
                    val obj = schedulesArr.getJSONObject(i)
                    schedulesList.add(
                        AppSchedule(
                            id = 0,
                            packageName = obj.optString("packageName", ""),
                            startTimeMinuteOfDay = obj.optInt("startTimeMinuteOfDay", 0),
                            endTimeMinuteOfDay = obj.optInt("endTimeMinuteOfDay", 0),
                            daysOfWeek = obj.optString("daysOfWeek", "1,2,3,4,5,6,7")
                        )
                    )
                }
            }

            // Parse Usages
            val usagesList = mutableListOf<DailyUsage>()
            val usagesArr = root.optJSONArray("dailyUsages")
            if (usagesArr != null) {
                for (i in 0 until usagesArr.length()) {
                    val obj = usagesArr.getJSONObject(i)
                    usagesList.add(
                        DailyUsage(
                            id = 0,
                            packageName = obj.optString("packageName", ""),
                            dateString = obj.optString("dateString", ""),
                            usedMinutes = obj.optInt("usedMinutes", 0)
                        )
                    )
                }
            }

            // Parse Usage Events
            val usageEventsList = mutableListOf<UsageEvent>()
            val eventsArr = root.optJSONArray("usageEvents")
            if (eventsArr != null) {
                for (i in 0 until eventsArr.length()) {
                    val obj = eventsArr.getJSONObject(i)
                    usageEventsList.add(
                        UsageEvent(
                            id = 0,
                            packageName = obj.optString("packageName", ""),
                            startTime = obj.optLong("startTime", 0L),
                            endTime = obj.optLong("endTime", 0L),
                            durationMinutes = obj.optInt("durationMinutes", 0),
                            dateString = obj.optString("dateString", "")
                        )
                    )
                }
            }

            // Parse Sessions
            val sessionsList = mutableListOf<FocusSession>()
            val sessionsArr = root.optJSONArray("sessions")
            if (sessionsArr != null) {
                for (i in 0 until sessionsArr.length()) {
                    val obj = sessionsArr.getJSONObject(i)
                    sessionsList.add(
                        FocusSession(
                            id = 0,
                            startTime = obj.optLong("startTime", System.currentTimeMillis()),
                            durationMinutes = obj.optInt("durationMinutes", 25),
                            isCompleted = obj.optBoolean("isCompleted", true),
                            mode = obj.optString("mode", "Deep Work"),
                            profileId = if (obj.has("profileId")) obj.optInt("profileId") else null,
                            journalEntry = if (obj.has("journalEntry")) obj.optString("journalEntry") else null
                        )
                    )
                }
            }

            // Parse Profiles
            val profilesList = mutableListOf<FocusProfile>()
            val profilesArr = root.optJSONArray("profiles")
            if (profilesArr != null) {
                for (i in 0 until profilesArr.length()) {
                    val obj = profilesArr.getJSONObject(i)
                    profilesList.add(
                        FocusProfile(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", "Custom Profile"),
                            icon = obj.optString("icon", "Study"),
                            focusDuration = obj.optInt("focusDuration", 25),
                            isBedtime = obj.optBoolean("isBedtime", false),
                            isLocationEnabled = obj.optBoolean("isLocationEnabled", false),
                            latitude = if (obj.has("latitude")) obj.optDouble("latitude") else null,
                            longitude = if (obj.has("longitude")) obj.optDouble("longitude") else null,
                            radiusMeters = if (obj.has("radiusMeters")) obj.optDouble("radiusMeters").toFloat() else null,
                            isActive = false
                        )
                    )
                }
            }

            // Parse Profile Apps
            val profileAppsList = mutableListOf<FocusProfileApp>()
            val profileAppsArr = root.optJSONArray("profileApps")
            if (profileAppsArr != null) {
                for (i in 0 until profileAppsArr.length()) {
                    val obj = profileAppsArr.getJSONObject(i)
                    profileAppsList.add(
                        FocusProfileApp(
                            id = 0,
                            profileId = obj.optInt("profileId", 0),
                            packageName = obj.optString("packageName", "")
                        )
                    )
                }
            }

            // Parse Goals
            val goalsList = mutableListOf<Goal>()
            val goalsArr = root.optJSONArray("goals")
            if (goalsArr != null) {
                for (i in 0 until goalsArr.length()) {
                    val obj = goalsArr.getJSONObject(i)
                    goalsList.add(
                        Goal(
                            id = 0,
                            title = obj.optString("title", "Goal"),
                            type = obj.optString("type", "USAGE_LIMIT"),
                            targetValue = obj.optInt("targetValue", 60),
                            currentValue = obj.optInt("currentValue", 0),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Achievements
            val achievementsList = mutableListOf<Achievement>()
            val achievementsArr = root.optJSONArray("achievements")
            if (achievementsArr != null) {
                for (i in 0 until achievementsArr.length()) {
                    val obj = achievementsArr.getJSONObject(i)
                    achievementsList.add(
                        Achievement(
                            id = obj.optString("id", "ach_$i"),
                            title = obj.optString("title", "Achievement"),
                            description = obj.optString("description", ""),
                            isUnlocked = obj.optBoolean("isUnlocked", false),
                            unlockedAt = if (obj.has("unlockedAt")) obj.optLong("unlockedAt") else null,
                            xpReward = obj.optInt("xpReward", 50)
                        )
                    )
                }
            }

            // Parse Groups
            val groupsList = mutableListOf<AppGroup>()
            val groupsArr = root.optJSONArray("groups")
            if (groupsArr != null) {
                for (i in 0 until groupsArr.length()) {
                    val obj = groupsArr.getJSONObject(i)
                    groupsList.add(
                        AppGroup(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", "Group"),
                            color = if (obj.has("color")) obj.optInt("color") else null
                        )
                    )
                }
            }

            // Parse Group Members
            val membersList = mutableListOf<AppGroupMember>()
            val membersArr = root.optJSONArray("groupMembers")
            if (membersArr != null) {
                for (i in 0 until membersArr.length()) {
                    val obj = membersArr.getJSONObject(i)
                    membersList.add(
                        AppGroupMember(
                            id = 0,
                            groupId = obj.optInt("groupId", 0),
                            packageName = obj.optString("packageName", "")
                        )
                    )
                }
            }

            val backup = FocusLockBackup(
                version = version,
                magic = magic,
                timestamp = timestamp,
                settings = parsedSettings,
                limits = limitsList,
                schedules = schedulesList,
                dailyUsages = usagesList,
                usageEvents = usageEventsList,
                sessions = sessionsList,
                profiles = profilesList,
                profileApps = profileAppsList,
                goals = goalsList,
                achievements = achievementsList,
                groups = groupsList,
                groupMembers = membersList
            )

            val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val dateStr = formatter.format(Date(timestamp))

            val summary = BackupSummary(
                createdAt = dateStr,
                version = version,
                limitsCount = limitsList.size,
                schedulesCount = schedulesList.size,
                focusSessionsCount = sessionsList.size,
                usageRecordsCount = usagesList.size,
                profilesCount = profilesList.size,
                goalsCount = goalsList.size,
                level = parsedSettings?.level ?: 1,
                xp = parsedSettings?.xp ?: 0,
                streak = parsedSettings?.currentStreak ?: 0
            )

            ValidationResult(
                isValid = true,
                summary = summary,
                parsedBackup = backup
            )
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errorMessage = "Failed to parse backup: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    suspend fun restoreBackup(repository: AppRepository, backup: FocusLockBackup): Boolean = withContext(Dispatchers.IO) {
        try {
            repository.restoreAllData(
                settings = backup.settings,
                limits = backup.limits,
                schedules = backup.schedules,
                dailyUsages = backup.dailyUsages,
                usageEvents = backup.usageEvents,
                sessions = backup.sessions,
                profiles = backup.profiles,
                profileApps = backup.profileApps,
                goals = backup.goals,
                achievements = backup.achievements,
                groups = backup.groups,
                groupMembers = backup.groupMembers
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
