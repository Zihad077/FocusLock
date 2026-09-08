package com.example.service

import com.example.database.AppSchedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleAndEnforcementTest {

    private val schedule = AppSchedule(
        id = 1,
        packageName = "com.instagram.android",
        startTimeMinuteOfDay = 22 * 60, // 22:00 (10:00 PM)
        endTimeMinuteOfDay = 6 * 60,   // 06:00 (6:00 AM next morning)
        daysOfWeek = "2"               // Monday (Calendar.MONDAY = 2)
    )

    private val daytimeSchedule = AppSchedule(
        id = 2,
        packageName = "com.facebook.katana",
        startTimeMinuteOfDay = 9 * 60,  // 09:00
        endTimeMinuteOfDay = 17 * 60,   // 17:00
        daysOfWeek = "2,3,4,5,6"        // Mon-Fri
    )

    private fun testIsScheduleActive(
        schedule: AppSchedule,
        minuteOfDay: Int,
        dayOfWeek: Int
    ): Boolean {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
        }

        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toString()

        return if (schedule.startTimeMinuteOfDay <= schedule.endTimeMinuteOfDay) {
            schedule.daysOfWeek.contains(currentDayOfWeek) &&
                    minuteOfDay in schedule.startTimeMinuteOfDay..schedule.endTimeMinuteOfDay
        } else {
            val startsToday = schedule.daysOfWeek.contains(currentDayOfWeek) &&
                    minuteOfDay >= schedule.startTimeMinuteOfDay

            val prevCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val prevDayOfWeek = prevCal.get(Calendar.DAY_OF_WEEK).toString()
            val startedYesterday = schedule.daysOfWeek.contains(prevDayOfWeek) &&
                    minuteOfDay <= schedule.endTimeMinuteOfDay

            startsToday || startedYesterday
        }
    }

    @Test
    fun overnightSchedule_activeOnScheduledNight() {
        // Monday at 23:00 (11 PM) -> Active
        assertTrue(testIsScheduleActive(schedule, 23 * 60, Calendar.MONDAY))
    }

    @Test
    fun overnightSchedule_activeOnFollowingMorning() {
        // Tuesday morning at 04:30 AM -> Active because overnight window began Monday night
        assertTrue(testIsScheduleActive(schedule, 4 * 60 + 30, Calendar.TUESDAY))
    }

    @Test
    fun overnightSchedule_inactiveAfterEndTime() {
        // Tuesday morning at 06:30 AM -> Inactive (ended at 06:00)
        assertFalse(testIsScheduleActive(schedule, 6 * 60 + 30, Calendar.TUESDAY))
    }

    @Test
    fun overnightSchedule_inactiveOnUnscheduledNight() {
        // Wednesday night at 23:00 -> Inactive (only Monday is scheduled)
        assertFalse(testIsScheduleActive(schedule, 23 * 60, Calendar.WEDNESDAY))
    }

    @Test
    fun daytimeSchedule_activeDuringWorkingHours() {
        // Wednesday at 14:00 (2 PM) -> Active
        assertTrue(testIsScheduleActive(daytimeSchedule, 14 * 60, Calendar.WEDNESDAY))
    }

    @Test
    fun daytimeSchedule_inactiveOnWeekend() {
        // Sunday at 14:00 -> Inactive
        assertFalse(testIsScheduleActive(daytimeSchedule, 14 * 60, Calendar.SUNDAY))
    }

    @Test
    fun daytimeSchedule_inactiveOutsideHours() {
        // Wednesday at 20:00 -> Inactive
        assertFalse(testIsScheduleActive(daytimeSchedule, 20 * 60, Calendar.WEDNESDAY))
    }
}
