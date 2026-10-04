package com.example.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.data.AppRepository
import com.example.database.AppLimit
import com.example.database.DailyUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class UsageTimeRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days")
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usedMinutes: Int,
    val usedMillis: Long,
    val launchCount: Int = 0,
    val lastTimeUsed: Long = 0L,
    val category: String = "General",
    val percentageOfTotal: Float = 0f,
    val isLimitActive: Boolean = false,
    val dailyLimitMinutes: Int = 0,
    val isDistracting: Boolean = false
)

data class DailyStat(
    val day: String,
    val minutes: Int,
    val dateString: String = ""
)

data class ScreenTimeSummary(
    val totalScreenTimeMinutes: Int = 0,
    val topApp: AppUsageInfo? = null,
    val totalAppCount: Int = 0,
    val averageDailyMinutes: Int = 0,
    val comparisonText: String = "",
    val appUsageList: List<AppUsageInfo> = emptyList(),
    val dailyStats: List<DailyStat> = emptyList(),
    val categoryDistribution: Map<String, Int> = emptyMap()
)

object UsageStatsHelper {

    private const val TAG = "UsageStatsHelper"

    /**
     * Resolves human-readable category for a package based on Android ApplicationInfo category
     * or known package name identifiers.
     */
    fun getAppCategory(packageManager: PackageManager, packageName: String): String {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (appInfo.category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> return "Social"
                    ApplicationInfo.CATEGORY_GAME -> return "Gaming"
                    ApplicationInfo.CATEGORY_VIDEO -> return "Entertainment"
                    ApplicationInfo.CATEGORY_AUDIO -> return "Music & Audio"
                    ApplicationInfo.CATEGORY_NEWS -> return "News"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "Productivity"
                    ApplicationInfo.CATEGORY_MAPS -> return "Navigation"
                    ApplicationInfo.CATEGORY_IMAGE -> return "Photos"
                }
            }
        } catch (e: Exception) {
            // Ignore package manager resolution errors
        }

        val lowerPkg = packageName.lowercase()
        return when {
            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("tiktok") ||
                    lowerPkg.contains("primevideo") || lowerPkg.contains("disney") || lowerPkg.contains("twitch") ||
                    lowerPkg.contains("hulu") || lowerPkg.contains("video") -> "Entertainment"

            lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
                    lowerPkg.contains("snapchat") || lowerPkg.contains("reddit") || lowerPkg.contains("threads") ||
                    lowerPkg.contains("pinterest") -> "Social"

            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("messenger") ||
                    lowerPkg.contains("discord") || lowerPkg.contains("signal") || lowerPkg.contains("viber") ||
                    lowerPkg.contains("wechat") -> "Communication"

            lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerPkg.contains("roblox") ||
                    lowerPkg.contains("minecraft") || lowerPkg.contains("candy") || lowerPkg.contains("clash") ||
                    lowerPkg.contains("genshin") || lowerPkg.contains("supercell") -> "Gaming"

            lowerPkg.contains("chrome") || lowerPkg.contains("firefox") || lowerPkg.contains("browser") ||
                    lowerPkg.contains("opera") || lowerPkg.contains("edge") -> "Browsing"

            lowerPkg.contains("gmail") || lowerPkg.contains("docs") || lowerPkg.contains("sheets") ||
                    lowerPkg.contains("notion") || lowerPkg.contains("slack") || lowerPkg.contains("teams") ||
                    lowerPkg.contains("trello") || lowerPkg.contains("office") -> "Productivity"

            else -> "General"
        }
    }

    /**
     * Determines whether an app is commonly considered distracting.
     */
    fun isAppDistracting(category: String, packageName: String): Boolean {
        if (category == "Social" || category == "Entertainment" || category == "Gaming") {
            return true
        }
        val lower = packageName.lowercase()
        return lower.contains("instagram") || lower.contains("tiktok") || lower.contains("facebook") ||
                lower.contains("youtube") || lower.contains("reddit") || lower.contains("twitter") ||
                lower.contains("snapchat") || lower.contains("netflix") || lower.contains("twitch")
    }

    /**
     * Internal raw usage metrics parsed directly from Android UsageEvents and UsageStats.
     */
    private data class ParsedUsageTelemetry(
        val appDurations: Map<String, Long>,
        val appLaunchCounts: Map<String, Int>,
        val appLastUsed: Map<String, Long>,
        val totalDeviceScreenTimeMillis: Long
    )

    /**
     * Merges overlapping time intervals into non-overlapping segments to compute
     * exact device screen-on / active phone usage without double counting.
     */
    private fun calculateMergedIntervalsMillis(intervals: List<Pair<Long, Long>>, maxWindowMillis: Long): Long {
        if (intervals.isEmpty()) return 0L
        val sorted = intervals.filter { it.second > it.first }.sortedBy { it.first }
        if (sorted.isEmpty()) return 0L

        var total = 0L
        var curStart = sorted[0].first
        var curEnd = sorted[0].second

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= curEnd) {
                curEnd = maxOf(curEnd, next.second)
            } else {
                total += (curEnd - curStart)
                curStart = next.first
                curEnd = next.second
            }
        }
        total += (curEnd - curStart)
        return minOf(maxWindowMillis, maxOf(0L, total))
    }

    /**
     * Queries Android UsageEvents and UsageStats to accurately compute foreground duration,
     * launch counts, and non-overlapping total device screen time.
     */
    private fun queryTelemetry(
        context: Context,
        startTime: Long,
        endTime: Long
    ): ParsedUsageTelemetry {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ParsedUsageTelemetry(emptyMap(), emptyMap(), emptyMap(), 0L)

        val selfPackage = context.packageName
        val maxWindow = maxOf(0L, endTime - startTime)

        val foregroundDurations = mutableMapOf<String, Long>()
        val launchCounts = mutableMapOf<String, Int>()
        val lastUsedMap = mutableMapOf<String, Long>()
        val timelineIntervals = mutableListOf<Pair<Long, Long>>()

        val activeAppStarts = mutableMapOf<String, Long>()

        // 1. Precise chronological event parsing via UsageEvents
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (pkg == selfPackage || pkg == "com.android.systemui") continue

                val eventType = event.eventType
                val time = event.timeStamp

                when (eventType) {
                    // ACTIVITY_RESUMED or MOVE_TO_FOREGROUND
                    1 -> {
                        launchCounts[pkg] = (launchCounts[pkg] ?: 0) + 1
                        activeAppStarts[pkg] = maxOf(startTime, time)
                        lastUsedMap[pkg] = maxOf(lastUsedMap[pkg] ?: 0L, time)
                    }
                    // ACTIVITY_PAUSED or MOVE_TO_BACKGROUND or ACTIVITY_STOPPED (2 or 23)
                    2, 23 -> {
                        val start = activeAppStarts.remove(pkg)
                        if (start != null) {
                            val end = minOf(endTime, time)
                            if (end > start) {
                                val duration = end - start
                                foregroundDurations[pkg] = (foregroundDurations[pkg] ?: 0L) + duration
                                timelineIntervals.add(Pair(start, end))
                            }
                        } else if (time > startTime) {
                            // App was already in foreground when interval began
                            val duration = minOf(endTime, time) - startTime
                            if (duration > 0) {
                                foregroundDurations[pkg] = (foregroundDurations[pkg] ?: 0L) + duration
                                timelineIntervals.add(Pair(startTime, minOf(endTime, time)))
                                if ((launchCounts[pkg] ?: 0) == 0) {
                                    launchCounts[pkg] = 1
                                }
                            }
                        }
                        lastUsedMap[pkg] = maxOf(lastUsedMap[pkg] ?: 0L, time)
                    }
                }
            }

            // Close any currently active sessions that remained in foreground up to endTime
            val now = System.currentTimeMillis()
            val effectiveEnd = minOf(endTime, now)
            for ((pkg, start) in activeAppStarts) {
                if (effectiveEnd > start) {
                    val duration = effectiveEnd - start
                    foregroundDurations[pkg] = (foregroundDurations[pkg] ?: 0L) + duration
                    timelineIntervals.add(Pair(start, effectiveEnd))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage events", e)
        }

        // 2. Reconcile with queryUsageStats / queryAndAggregateUsageStats for any missing apps
        try {
            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            for (stats in statsList) {
                val pkg = stats.packageName ?: continue
                if (pkg == selfPackage || pkg == "com.android.systemui") continue

                val rawDuration = stats.totalTimeInForeground
                val clamped = minOf(maxWindow, rawDuration)

                if (stats.lastTimeUsed in startTime..endTime) {
                    lastUsedMap[pkg] = maxOf(lastUsedMap[pkg] ?: 0L, stats.lastTimeUsed)
                }

                // If events didn't capture this app or gave 0 while stats has positive usage:
                val eventDuration = foregroundDurations[pkg] ?: 0L
                if (eventDuration <= 0L && clamped > 0L && stats.lastTimeUsed >= startTime) {
                    foregroundDurations[pkg] = clamped
                    timelineIntervals.add(Pair(maxOf(startTime, stats.lastTimeUsed - clamped), stats.lastTimeUsed))
                }

                // Ensure launch count is at least 1 if the app was used
                if ((foregroundDurations[pkg] ?: 0L) > 0L && (launchCounts[pkg] ?: 0) == 0) {
                    val count = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val countMethod = stats.javaClass.getMethod("getAppLaunchCount")
                            (countMethod.invoke(stats) as? Int) ?: 1
                        } catch (e: Exception) {
                            1
                        }
                    } else {
                        1
                    }
                    launchCounts[pkg] = maxOf(1, count)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage stats fallback", e)
        }

        // Clamp each app's duration to the requested interval window
        val finalAppDurations = foregroundDurations.mapValues { (_, dur) ->
            minOf(maxWindow, dur)
        }

        // 3. Compute non-overlapping total device screen time
        val mergedDeviceMillis = calculateMergedIntervalsMillis(timelineIntervals, maxWindow)
        val totalDeviceMillis = if (mergedDeviceMillis > 0L) {
            mergedDeviceMillis
        } else {
            // Fallback: sum of foreground apps capped at total elapsed window
            val sum = finalAppDurations.values.sum()
            minOf(maxWindow, sum)
        }

        return ParsedUsageTelemetry(
            appDurations = finalAppDurations,
            appLaunchCounts = launchCounts,
            appLastUsed = lastUsedMap,
            totalDeviceScreenTimeMillis = totalDeviceMillis
        )
    }

    /**
     * Retrieves detailed app usage for a given time range directly from Android's UsageStatsManager.
     */
    fun getAppUsageList(
        context: Context,
        timeRange: UsageTimeRange,
        limits: List<AppLimit> = emptyList()
    ): List<AppUsageInfo> {
        if (!PermissionHelper.hasUsageAccess(context)) {
            return emptyList()
        }

        val packageManager = context.packageManager
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        val (startTime, endTime) = when (timeRange) {
            UsageTimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            UsageTimeRange.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            UsageTimeRange.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
        }

        val telemetry = queryTelemetry(context, startTime, endTime)
        val limitMap = limits.associateBy { it.packageName }
        val selfPackage = context.packageName

        var totalAppMillis = 0L
        val rawList = mutableListOf<AppUsageInfo>()

        for ((pkg, totalTime) in telemetry.appDurations) {
            if (totalTime < 10_000L || pkg == selfPackage || pkg == "com.android.systemui") {
                continue
            }

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                if (packageManager.getLaunchIntentForPackage(pkg) == null &&
                    !pkg.contains("chrome") && !pkg.contains("youtube") && !pkg.contains("google")
                ) {
                    continue
                }
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                continue
            }

            val usedMinutes = if (totalTime >= 30000L) maxOf(1, ((totalTime + 30000L) / 60000L).toInt()) else (totalTime / 60000L).toInt()
            if (usedMinutes <= 0 && totalTime < 15000L) {
                continue
            }

            totalAppMillis += totalTime
            val category = getAppCategory(packageManager, pkg)
            val limit = limitMap[pkg]
            val isLimitActive = limit?.isEnabled == true
            val rawLaunchCount = telemetry.appLaunchCounts[pkg] ?: 0
            val launchCount = if (usedMinutes > 0 && rawLaunchCount == 0) 1 else rawLaunchCount

            rawList.add(
                AppUsageInfo(
                    packageName = pkg,
                    appName = appName,
                    usedMinutes = maxOf(1, usedMinutes),
                    usedMillis = totalTime,
                    launchCount = launchCount,
                    lastTimeUsed = telemetry.appLastUsed[pkg] ?: 0L,
                    category = category,
                    isLimitActive = isLimitActive,
                    dailyLimitMinutes = if (isLimitActive) (limit?.dailyLimitMinutes ?: 0) else 0,
                    isDistracting = isAppDistracting(category, pkg)
                )
            )
        }

        // Also include apps that have an active limit (even if 0 usage today)
        if (timeRange == UsageTimeRange.TODAY) {
            val existingPackages = rawList.map { it.packageName }.toSet()
            for (limit in limits) {
                if (limit.isEnabled && limit.packageName != selfPackage && !existingPackages.contains(limit.packageName)) {
                    val appName = try {
                        val appInfo = packageManager.getApplicationInfo(limit.packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        if (limit.appName.isNotEmpty()) limit.appName else continue
                    }
                    val category = getAppCategory(packageManager, limit.packageName)
                    rawList.add(
                        AppUsageInfo(
                            packageName = limit.packageName,
                            appName = appName,
                            usedMinutes = 0,
                            usedMillis = 0L,
                            launchCount = 0,
                            lastTimeUsed = 0L,
                            category = category,
                            isLimitActive = true,
                            dailyLimitMinutes = limit.dailyLimitMinutes,
                            isDistracting = isAppDistracting(category, limit.packageName)
                        )
                    )
                }
            }
        }

        // Sort descending by screen time (with active limits prioritized when usage is 0)
        rawList.sortWith(
            compareByDescending<AppUsageInfo> { it.usedMillis }
                .thenByDescending { it.isLimitActive }
                .thenBy { it.appName.lowercase() }
        )

        val denominator = if (totalAppMillis > 0L) totalAppMillis.toFloat() else 1f
        return rawList.map {
            it.copy(percentageOfTotal = (it.usedMillis.toFloat() / denominator).coerceIn(0f, 1f))
        }
    }

    /**
     * Calculates full screen time telemetry summary, top distractor, category distribution,
     * and 7-day daily trend from UsageStatsManager.
     */
    fun getScreenTimeSummary(
        context: Context,
        timeRange: UsageTimeRange,
        limits: List<AppLimit> = emptyList()
    ): ScreenTimeSummary {
        val appList = getAppUsageList(context, timeRange, limits)

        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val (startTime, endTime) = when (timeRange) {
            UsageTimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            UsageTimeRange.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            UsageTimeRange.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
        }

        val telemetry = queryTelemetry(context, startTime, endTime)
        val nonOverlappingTotalMinutes = (telemetry.totalDeviceScreenTimeMillis / 60000L).toInt()
        val sumAppsMinutes = appList.sumOf { it.usedMinutes }
        val finalTotalMinutes = if (nonOverlappingTotalMinutes > 0) {
            nonOverlappingTotalMinutes
        } else {
            val maxAllowedMinutes = ((endTime - startTime) / 60000L).toInt()
            minOf(maxAllowedMinutes, sumAppsMinutes)
        }

        val topApp = appList.firstOrNull()

        // Category distribution
        val categoryMap = mutableMapOf<String, Int>()
        appList.forEach { app ->
            val current = categoryMap.getOrDefault(app.category, 0)
            categoryMap[app.category] = current + app.usedMinutes
        }

        // Daily trend for the last 7 days
        val dailyStats = get7DayDailyStats(context)
        val averageDaily = if (dailyStats.isNotEmpty()) {
            dailyStats.sumOf { it.minutes } / dailyStats.size
        } else {
            finalTotalMinutes
        }

        val comparisonText = if (timeRange == UsageTimeRange.TODAY && dailyStats.size >= 2) {
            val yesterdayMinutes = dailyStats.getOrNull(dailyStats.size - 2)?.minutes ?: 0
            val diff = finalTotalMinutes - yesterdayMinutes
            if (diff > 0) {
                "${FormatUtils.formatHoursMinutes(diff)} more than yesterday"
            } else if (diff < 0) {
                val absDiff = -diff
                "${FormatUtils.formatHoursMinutes(absDiff)} less than yesterday"
            } else {
                "Same as yesterday"
            }
        } else {
            "Daily Average: ${FormatUtils.formatHoursMinutes(averageDaily)}"
        }

        return ScreenTimeSummary(
            totalScreenTimeMinutes = finalTotalMinutes,
            topApp = topApp,
            totalAppCount = appList.size,
            averageDailyMinutes = averageDaily,
            comparisonText = comparisonText,
            appUsageList = appList,
            dailyStats = dailyStats,
            categoryDistribution = categoryMap
        )
    }

    /**
     * Computes real non-overlapping daily usage stats for the last 7 days.
     */
    fun get7DayDailyStats(context: Context): List<DailyStat> {
        if (!PermissionHelper.hasUsageAccess(context)) {
            return emptyList()
        }

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val result = mutableListOf<DailyStat>()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = if (i == 0) System.currentTimeMillis() else cal.timeInMillis

            val dayName = dayFormat.format(Date(dayStart))
            val dateStr = dateFormat.format(Date(dayStart))

            val telemetry = queryTelemetry(context, dayStart, dayEnd)
            val dayMinutes = (telemetry.totalDeviceScreenTimeMillis / 60000L).toInt()

            result.add(DailyStat(day = dayName, minutes = dayMinutes, dateString = dateStr))
        }

        return result
    }

    /**
     * Retrieves exact foreground usage milliseconds for a specific package today.
     * Prevents overcounting or relying on stale multi-day buckets.
     */
    fun getForegroundUsageMillisForPackage(
        context: Context,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Long {
        val telemetry = queryTelemetry(context, startTime, endTime)
        return telemetry.appDurations[packageName] ?: 0L
    }

    /**
     * Automatically imports and synchronizes historical Android usage stats into the local Room database
     * so that all telemetry, charts, and limits immediately display real historical data.
     */
    suspend fun syncHistoricalUsageToDatabase(
        context: Context,
        repository: AppRepository,
        daysBack: Int = 7
    ) = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasUsageAccess(context)) {
            return@withContext
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selfPkg = context.packageName

        try {
            for (i in (daysBack - 1) downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dayStart = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val dayEnd = if (i == 0) System.currentTimeMillis() else cal.timeInMillis

                val dateStr = dateFormat.format(Date(dayStart))
                val telemetry = queryTelemetry(context, dayStart, dayEnd)

                val dailyUsages = mutableListOf<DailyUsage>()
                for ((pkg, totalTime) in telemetry.appDurations) {
                    val minutes = if (totalTime >= 15000L) maxOf(1, ((totalTime + 30000L) / 60000L).toInt()) else 0
                    if (minutes > 0 && pkg != selfPkg && pkg != "com.android.systemui") {
                        dailyUsages.add(
                            DailyUsage(
                                packageName = pkg,
                                dateString = dateStr,
                                usedMinutes = minutes
                            )
                        )
                    }
                }

                repository.deleteUsageForDate(dateStr)
                if (dailyUsages.isNotEmpty()) {
                    repository.insertUsages(dailyUsages)
                }
            }
            Log.d(TAG, "Successfully synced historical UsageStats to Room database for past $daysBack days")
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing historical usage stats", e)
        }
    }
}
