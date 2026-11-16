package ru.echoapp.util

import java.util.Calendar

/** Серия дней с записями подряд — считается по меткам времени записей. */
object StreakCalculator {

    /**
     * Дней подряд с записью, заканчиваясь сегодня или вчера
     * (сегодняшняя запись необязательна — серия не сгорает до конца дня).
     */
    fun computeStreak(timestamps: List<Long>, now: Long): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { dayStart(it) }.toSet()

        val today = dayStart(now)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = today

        // Если сегодня записи ещё нет — начинаем считать со вчера.
        if (today !in days) calendar.add(Calendar.DAY_OF_YEAR, -1)

        var streak = 0
        while (calendar.timeInMillis in days) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun dayStart(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
