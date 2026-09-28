package com.example.util

import com.example.data.AppRepository
import com.example.database.Achievement
import com.example.database.AppLimit
import com.example.database.FocusSession
import com.example.database.Goal
import com.example.database.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StreakDayActivity(
    val dayLabel: String,
    val dateString: String,
    val isCompleted: Boolean,
    val isToday: Boolean
)

data class StreakMilestoneInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val nextMilestoneDays: Int,
    val progressToMilestone: Float,
    val statusHeadline: String,
    val statusSubtitle: String,
    val last7Days: List<StreakDayActivity>,
    val totalCompletedSessions: Int,
    val totalFocusMinutes: Int
)

data class AchievementProgress(
    val achievement: Achievement,
    val currentVal: Int,
    val targetVal: Int,
    val progressFraction: Float,
    val progressLabel: String,
    val categoryTag: String
)

object StreakAndAchievementManager {

    private val _newlyUnlockedAchievement = MutableStateFlow<Achievement?>(null)
    val newlyUnlockedAchievement: StateFlow<Achievement?> = _newlyUnlockedAchievement.asStateFlow()

    private val _unlockEvents = MutableSharedFlow<Achievement>(extraBufferCapacity = 8)
    val unlockEvents: SharedFlow<Achievement> = _unlockEvents.asSharedFlow()

    val milestoneTargets = listOf(3, 7, 14, 30, 60, 100)

    fun dismissUnlockedBanner() {
        _newlyUnlockedAchievement.value = null
    }

    fun defaultCatalog(): List<Achievement> = listOf(
        Achievement(
            id = "first_step",
            title = "Locked In 🔒",
            description = "Complete your very first deep focus session",
            isUnlocked = false,
            xpReward = 50
        ),
        Achievement(
            id = "streak_3",
            title = "On a Roll 🔥",
            description = "Keep a 3-day consecutive focus streak alive",
            isUnlocked = false,
            xpReward = 100
        ),
        Achievement(
            id = "consistency",
            title = "Week Warrior ⚡",
            description = "Lock in for 7 consecutive days of focus",
            isUnlocked = false,
            xpReward = 300
        ),
        Achievement(
            id = "deep_diver",
            title = "Deep Diver 🌊",
            description = "Focus for 2+ hours in a single session",
            isUnlocked = false,
            xpReward = 150
        ),
        Achievement(
            id = "focus_5_sessions",
            title = "Flow State Regular ✨",
            description = "Complete 5 deep focus sessions",
            isUnlocked = false,
            xpReward = 150
        ),
        Achievement(
            id = "focus_300_min",
            title = "Time Reclaimer ⏳",
            description = "Accumulate 300+ total minutes of deep focus",
            isUnlocked = false,
            xpReward = 250
        ),
        Achievement(
            id = "limit_setter",
            title = "Boundary Builder 🛡️",
            description = "Set active daily limits on 3+ distracting apps",
            isUnlocked = false,
            xpReward = 100
        ),
        Achievement(
            id = "goal_crusher",
            title = "Goal Crusher 🎯",
            description = "Complete a daily focus commitment goal",
            isUnlocked = false,
            xpReward = 150
        ),
        Achievement(
            id = "iron_will",
            title = "Touch Grass Champ 🌱",
            description = "Block 10+ impulse app opens or complete 3+ sessions with active shields",
            isUnlocked = false,
            xpReward = 200
        ),
        Achievement(
            id = "streak_14",
            title = "Unstoppable Era 🚀",
            description = "Maintain a 14-day focus streak without breaking flow",
            isUnlocked = false,
            xpReward = 500
        ),
        Achievement(
            id = "streak_30",
            title = "Monk Mode Legend 👑",
            description = "Achieve a 30-day master focus streak",
            isUnlocked = false,
            xpReward = 1000
        )
    )

    /**
     * Ensures all catalog achievements exist in Room while preserving existing unlock state and timestamps.
     */
    suspend fun ensureDefaultAchievements(repository: AppRepository) {
        val existing = repository.allAchievements.first().associateBy { it.id }
        val catalog = defaultCatalog()
        val toInsert = mutableListOf<Achievement>()

        for (item in catalog) {
            val prev = existing[item.id]
            if (prev == null) {
                toInsert.add(item)
            } else if (prev.title != item.title || prev.description != item.description) {
                repository.insertAchievement(
                    prev.copy(
                        title = item.title,
                        description = item.description,
                        xpReward = item.xpReward
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) {
            repository.insertAchievements(toInsert)
        }
    }

    /**
     * Evaluates user's real focus sessions, goals, app limits, and streak data.
     * Persists updated streaks, goals, XP, and newly unlocked achievements to Room.
     */
    suspend fun evaluateAndSync(
        repository: AppRepository,
        completedSessionMinutesJustNow: Int? = null
    ) {
        ensureDefaultAchievements(repository)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = System.currentTimeMillis()
        val todayStr = dateFormat.format(Date(now))

        val settings = repository.userSettings.first()
        val allSessions = repository.allFocusSessions.first()
        val completedSessions = allSessions.filter { it.isCompleted }
        val allGoals = repository.allGoals.first()
        val allLimits = repository.allLimits.first()
        val escapeAttempts = repository.allEscapeAttempts.first()

        // 1. Update active focus goals based on today's completed focus minutes
        val todayCompletedMinutes = completedSessions
            .filter { dateFormat.format(Date(it.startTime)) == todayStr }
            .sumOf { it.durationMinutes }

        var anyGoalCompletedToday = false
        for (goal in allGoals) {
            if (goal.type == "FOCUS_MINUTES" || goal.type == "FOCUS_TIME" || goal.type == "PRODUCTIVITY") {
                val updatedVal = maxOf(goal.currentValue, todayCompletedMinutes)
                val isNowCompleted = updatedVal >= goal.targetValue && goal.targetValue > 0
                if (isNowCompleted) anyGoalCompletedToday = true
                if (updatedVal != goal.currentValue || isNowCompleted != goal.isCompleted) {
                    repository.insertGoal(
                        goal.copy(
                            currentValue = updatedVal,
                            isCompleted = isNowCompleted
                        )
                    )
                }
            } else if (goal.isCompleted || (goal.targetValue > 0 && goal.currentValue >= goal.targetValue)) {
                anyGoalCompletedToday = true
            }
        }

        // 2. Compute consecutive days of completed focus sessions / goals
        val activeDates = completedSessions
            .map { dateFormat.format(Date(it.startTime)) }
            .toMutableSet()
        if (anyGoalCompletedToday || (completedSessionMinutesJustNow != null && completedSessionMinutesJustNow > 0)) {
            activeDates.add(todayStr)
        }

        val sessionStreak = computeConsecutiveStreakFromDates(activeDates, todayStr)

        var newCurrentStreak = maxOf(settings.currentStreak, sessionStreak)
        var newLastReset = settings.lastResetDateString

        if (completedSessionMinutesJustNow != null && completedSessionMinutesJustNow > 0) {
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayStr = dateFormat.format(yesterdayCal.time)
            if (settings.lastResetDateString != todayStr) {
                newCurrentStreak = if (settings.lastResetDateString == yesterdayStr) {
                    maxOf(settings.currentStreak + 1, sessionStreak)
                } else {
                    maxOf(1, sessionStreak)
                }
                newLastReset = todayStr
            } else if (newCurrentStreak == 0) {
                newCurrentStreak = 1
            }
        }

        val newBestStreak = maxOf(settings.bestStreak, newCurrentStreak)
        val totalFocusMinutes = completedSessions.sumOf { it.durationMinutes }
        val maxSessionMinutes = completedSessions.maxOfOrNull { it.durationMinutes } ?: 0
        val enabledLimitsCount = allLimits.count { it.isEnabled }
        val refreshedGoals = repository.allGoals.first()
        val completedGoalsCount = refreshedGoals.count { it.isCompleted || (it.targetValue > 0 && it.currentValue >= it.targetValue) }

        // 3. Check achievements against real user metrics
        val currentAchievements = repository.allAchievements.first()
        var earnedXpDelta = 0
        var latestUnlocked: Achievement? = null

        for (ach in currentAchievements) {
            if (ach.isUnlocked) continue

            val shouldUnlock = when (ach.id) {
                "first_step" -> completedSessions.isNotEmpty()
                "streak_3" -> newCurrentStreak >= 3 || newBestStreak >= 3
                "consistency" -> newCurrentStreak >= 7 || newBestStreak >= 7
                "streak_14" -> newCurrentStreak >= 14 || newBestStreak >= 14
                "streak_30" -> newCurrentStreak >= 30 || newBestStreak >= 30
                "deep_diver" -> maxSessionMinutes >= 120
                "focus_5_sessions" -> completedSessions.size >= 5
                "focus_300_min" -> totalFocusMinutes >= 300
                "limit_setter" -> enabledLimitsCount >= 3
                "goal_crusher" -> completedGoalsCount >= 1
                "iron_will" -> escapeAttempts.size >= 10 || (enabledLimitsCount >= 1 && completedSessions.size >= 3)
                else -> false
            }

            if (shouldUnlock) {
                val unlockedAch = ach.copy(
                    isUnlocked = true,
                    unlockedAt = now
                )
                repository.insertAchievement(unlockedAch)
                earnedXpDelta += unlockedAch.xpReward
                latestUnlocked = unlockedAch
                _unlockEvents.tryEmit(unlockedAch)
            }
        }

        if (latestUnlocked != null) {
            _newlyUnlockedAchievement.value = latestUnlocked
        }

        // 4. Update UserSettings if streak, bestStreak, or XP changed
        var updatedXp = settings.xp + earnedXpDelta
        var updatedLevel = settings.level
        while (updatedXp >= updatedLevel * 100) {
            updatedXp -= (updatedLevel * 100)
            updatedLevel += 1
        }

        if (newCurrentStreak != settings.currentStreak ||
            newBestStreak != settings.bestStreak ||
            newLastReset != settings.lastResetDateString ||
            updatedXp != settings.xp ||
            updatedLevel != settings.level
        ) {
            repository.updateSettings(
                settings.copy(
                    currentStreak = newCurrentStreak,
                    bestStreak = newBestStreak,
                    consecutiveSuccessfulDays = maxOf(settings.consecutiveSuccessfulDays, newCurrentStreak),
                    lastResetDateString = if (newLastReset.isNotEmpty()) newLastReset else settings.lastResetDateString,
                    xp = updatedXp,
                    level = updatedLevel
                )
            )
        }
    }

    private fun computeConsecutiveStreakFromDates(activeDates: Set<String>, todayStr: String): Int {
        if (activeDates.isEmpty()) return 0
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Start from today if today has activity; otherwise check from yesterday
        if (!activeDates.contains(todayStr)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)
            if (!activeDates.contains(yesterdayStr)) {
                return 0
            }
        }

        var streak = 0
        while (true) {
            val checkStr = dateFormat.format(cal.time)
            if (activeDates.contains(checkStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    /**
     * Builds UI model for Streak progress, 7-day activity dots, and Gen Z status copy.
     */
    fun buildStreakMilestoneInfo(
        settings: UserSettings?,
        sessions: List<FocusSession>,
        goals: List<Goal> = emptyList()
    ): StreakMilestoneInfo {
        val currentStreak = settings?.currentStreak ?: 0
        val bestStreak = maxOf(settings?.bestStreak ?: 0, currentStreak)
        val completedSessions = sessions.filter { it.isCompleted }
        val totalFocusMinutes = completedSessions.sumOf { it.durationMinutes }

        val nextMilestone = milestoneTargets.firstOrNull { it > currentStreak } ?: 100
        val prevMilestone = milestoneTargets.lastOrNull { it <= currentStreak } ?: 0
        val span = (nextMilestone - prevMilestone).coerceAtLeast(1)
        val progress = ((currentStreak - prevMilestone).toFloat() / span.toFloat()).coerceIn(0f, 1f)

        val (headline, subtitle) = when {
            currentStreak >= 14 -> "Unstoppable flow ⚡" to "You're in your peak focus era. Keep the streak alive!"
            currentStreak >= 7 -> "You're on a roll! 🔥" to "$currentStreak days locked in. Next milestone: ${nextMilestone}d badge."
            currentStreak >= 3 -> "You're on a roll! 🔥" to "Building real momentum — ${nextMilestone - currentStreak}d to your next badge."
            currentStreak >= 1 -> "Locked in 🔒" to "Streak started! Complete a session tomorrow to keep it rolling."
            else -> "Let's lock in 🔒" to "Finish a focus session today to ignite your daily streak."
        }

        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        val completedDates = completedSessions
            .map { dateFormat.format(Date(it.startTime)) }
            .toMutableSet()

        if (goals.any { it.isCompleted }) {
            completedDates.add(todayStr)
        }

        val days = mutableListOf<StreakDayActivity>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            val label = dayLabelFormat.format(cal.time).take(3)
            val isToday = (i == 0)
            // Also reflect currentStreak for recent consecutive days if streak was persisted in UserSettings
            val isCompletedByStreak = currentStreak > 0 && i < currentStreak && (completedDates.contains(todayStr) || i > 0)
            days.add(
                StreakDayActivity(
                    dayLabel = label,
                    dateString = dateStr,
                    isCompleted = completedDates.contains(dateStr) || isCompletedByStreak,
                    isToday = isToday
                )
            )
        }

        return StreakMilestoneInfo(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            nextMilestoneDays = nextMilestone,
            progressToMilestone = progress,
            statusHeadline = headline,
            statusSubtitle = subtitle,
            last7Days = days,
            totalCompletedSessions = completedSessions.size,
            totalFocusMinutes = totalFocusMinutes
        )
    }

    /**
     * Computes real progress for every achievement so badges show accurate progress bars and history.
     */
    fun buildAchievementProgressList(
        achievements: List<Achievement>,
        settings: UserSettings?,
        sessions: List<FocusSession>,
        limits: List<AppLimit>,
        goals: List<Goal>,
        escapeAttemptsCount: Int = 0
    ): List<AchievementProgress> {
        val completedSessions = sessions.filter { it.isCompleted }
        val totalMinutes = completedSessions.sumOf { it.durationMinutes }
        val maxSession = completedSessions.maxOfOrNull { it.durationMinutes } ?: 0
        val bestStreak = maxOf(settings?.bestStreak ?: 0, settings?.currentStreak ?: 0)
        val activeLimitsCount = limits.count { it.isEnabled }
        val completedGoalsCount = goals.count { it.isCompleted || (it.targetValue > 0 && it.currentValue >= it.targetValue) }

        val sourceList = if (achievements.isNotEmpty()) achievements else defaultCatalog()

        return sourceList.map { ach ->
            val (cur, target, unit, category) = when (ach.id) {
                "first_step" -> Quad(completedSessions.size.coerceAtMost(1), 1, "session", "FOCUS")
                "streak_3" -> Quad(bestStreak.coerceAtMost(3), 3, "days", "STREAK")
                "consistency" -> Quad(bestStreak.coerceAtMost(7), 7, "days", "STREAK")
                "streak_14" -> Quad(bestStreak.coerceAtMost(14), 14, "days", "STREAK")
                "streak_30" -> Quad(bestStreak.coerceAtMost(30), 30, "days", "STREAK")
                "deep_diver" -> Quad(maxSession.coerceAtMost(120), 120, "min", "DEEP WORK")
                "focus_5_sessions" -> Quad(completedSessions.size.coerceAtMost(5), 5, "sessions", "FOCUS")
                "focus_300_min" -> Quad(totalMinutes.coerceAtMost(300), 300, "min", "MILESTONE")
                "limit_setter" -> Quad(activeLimitsCount.coerceAtMost(3), 3, "apps", "SHIELD")
                "goal_crusher" -> Quad(completedGoalsCount.coerceAtMost(1), 1, "goal", "GOALS")
                "iron_will" -> {
                    val v = maxOf(escapeAttemptsCount, completedSessions.size * 3).coerceAtMost(10)
                    Quad(v, 10, "blocks", "DISCIPLINE")
                }
                else -> Quad(if (ach.isUnlocked) 1 else 0, 1, "milestone", "BADGE")
            }

            val effectiveCur = if (ach.isUnlocked) target else cur
            val fraction = if (target > 0) (effectiveCur.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f

            AchievementProgress(
                achievement = ach,
                currentVal = effectiveCur,
                targetVal = target,
                progressFraction = fraction,
                progressLabel = "$effectiveCur / $target $unit",
                categoryTag = category
            )
        }.sortedWith(
            compareByDescending<AchievementProgress> { it.achievement.isUnlocked }
                .thenByDescending { it.progressFraction }
                .thenBy { it.achievement.xpReward }
        )
    }

    private data class Quad(val a: Int, val b: Int, val c: String, val d: String)
}
