package com.example.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

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

        val aggregatedStats = try {
            usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying aggregated usage stats", e)
            emptyMap<String, UsageStats>()
        }

        val limitMap = limits.associateBy { it.packageName }
        val selfPackage = context.packageName

        var totalForegroundMillis = 0L
        val rawList = mutableListOf<AppUsageInfo>()

        for ((pkg, stats) in aggregatedStats) {
            val totalTime = stats.totalTimeInForeground
            // Ignore 0 usage, self, and system launcher components that aren't real user apps
            if (totalTime < 10_000L || pkg == selfPackage || pkg == "com.android.systemui") {
                continue
            }

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                // Filter out non-launchable background system daemons
                if (packageManager.getLaunchIntentForPackage(pkg) == null &&
                    !pkg.contains("chrome") && !pkg.contains("youtube") && !pkg.contains("google")
                ) {
                    continue
                }
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                // If application info not found, skip non-installed remnants
                continue
            }

            val usedMinutes = if (totalTime >= 30000L) maxOf(1, ((totalTime + 30000L) / 60000L).toInt()) else (totalTime / 60000L).toInt()
            if (usedMinutes <= 0 && totalTime < 15000L) {
                continue
            }

            totalForegroundMillis += totalTime
            val category = getAppCategory(packageManager, pkg)
            val limit = limitMap[pkg]

            rawList.add(
                AppUsageInfo(
                    packageName = pkg,
                    appName = appName,
                    usedMinutes = maxOf(1, usedMinutes),
                    usedMillis = totalTime,
                    lastTimeUsed = stats.lastTimeUsed,
                    category = category,
                    isLimitActive = limit?.isEnabled == true,
                    dailyLimitMinutes = limit?.dailyLimitMinutes ?: 0,
                    isDistracting = isAppDistracting(category, pkg)
                )
            )
        }

        // Sort descending by screen time
        rawList.sortByDescending { it.usedMillis }

        // Compute percentage of total screen time
        return if (totalForegroundMillis > 0L) {
            rawList.map {
                it.copy(percentageOfTotal = (it.usedMillis.toFloat() / totalForegroundMillis.toFloat()).coerceIn(0f, 1f))
            }
        } else {
            rawList
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
        val totalMinutes = appList.sumOf { it.usedMinutes }
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
            totalMinutes
        }

        val comparisonText = if (timeRange == UsageTimeRange.TODAY && dailyStats.size >= 2) {
            val yesterdayMinutes = dailyStats.getOrNull(dailyStats.size - 2)?.minutes ?: 0
            val diff = totalMinutes - yesterdayMinutes
            if (diff > 0) {
                "${diff / 60}h ${diff % 60}m more than yesterday"
            } else if (diff < 0) {
                val absDiff = -diff
                "${absDiff / 60}h ${absDiff % 60}m less than yesterday"
            } else {
                "Same as yesterday"
            }
        } else {
            "Daily Average: ${averageDaily / 60}h ${averageDaily % 60}m"
        }

        return ScreenTimeSummary(
            totalScreenTimeMinutes = totalMinutes,
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
     * Computes real daily usage stats for the last 7 days.
     */
    fun get7DayDailyStats(context: Context): List<DailyStat> {
        if (!PermissionHelper.hasUsageAccess(context)) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

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

            val statsMap = try {
                usageStatsManager.queryAndAggregateUsageStats(dayStart, dayEnd)
            } catch (e: Exception) {
                emptyMap<String, UsageStats>()
            }

            var totalDayMillis = 0L
            val selfPkg = context.packageName
            for ((pkg, stats) in statsMap) {
                if (pkg != selfPkg && pkg != "com.android.systemui") {
                    totalDayMillis += stats.totalTimeInForeground
                }
            }

            val dayMinutes = (totalDayMillis / 60000L).toInt()
            result.add(DailyStat(day = dayName, minutes = dayMinutes, dateString = dateStr))
        }

        return result
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

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext

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
                val statsMap = usageStatsManager.queryAndAggregateUsageStats(dayStart, dayEnd)

                val dailyUsages = mutableListOf<DailyUsage>()
                for ((pkg, stats) in statsMap) {
                    val foregroundMillis = stats.totalTimeInForeground
                    val minutes = if (foregroundMillis >= 30000L) maxOf(1, ((foregroundMillis + 30000L) / 60000L).toInt()) else (foregroundMillis / 60000L).toInt()
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
