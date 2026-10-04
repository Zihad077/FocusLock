package com.example.util

object FormatUtils {

    /**
     * Formats total minutes into a clear, user-friendly hours and minutes representation.
     * Prevents displaying raw excessive minutes (like 250m or 0h 250m).
     * Examples:
     * - 0 -> "0m"
     * - 45 -> "45m"
     * - 60 -> "1h"
     * - 175 -> "2h 55m"
     * - 250 -> "4h 10m"
     */
    fun formatHoursMinutes(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0m"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours == 0 -> "${mins}m"
            mins == 0 -> "${hours}h"
            else -> "${hours}h ${mins}m"
        }
    }

    /**
     * Formats total minutes with explicit hours and minutes for comparison (e.g., "2h 55m").
     */
    fun formatDetailedHoursMinutes(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0h 0m"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return "${hours}h ${mins}m"
    }

    /**
     * Formats used vs limit representation (e.g. "2h 55m / 2h used").
     */
    fun formatUsedVsLimit(usedMinutes: Int, limitMinutes: Int): String {
        val used = formatHoursMinutes(usedMinutes)
        val limit = formatHoursMinutes(limitMinutes)
        return "$used / $limit used"
    }

    /**
     * Formats remaining time (e.g. "1h 15m left" or "25m left").
     */
    fun formatRemaining(remainingMinutes: Int): String {
        if (remainingMinutes <= 0) return "0m left"
        return "${formatHoursMinutes(remainingMinutes)} left"
    }

    /**
     * Formats app launch / open counts cleanly (e.g. "1 open", "5 opens").
     */
    fun formatOpens(count: Int): String {
        val safeCount = maxOf(0, count)
        return if (safeCount == 1) "1 open" else "$safeCount opens"
    }
}
