package ru.echoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnniversaryDatesMoreTest {

    private fun millis(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        set(year, month, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `year bounds cover january first to next january first`() {
        val (start, end) = AnniversaryDates.yearBounds(millis(2026, Calendar.JUNE, 15))
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, startCal.get(Calendar.MONTH))
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        // Ровно год между границами.
        assertEquals(365L * 24 * 60 * 60 * 1000, end - start)
    }

    @Test
    fun `same week last year is seven days and contains the shifted date`() {
        // 2026-11-12 — четверг; понедельник той недели — 2026-11-09.
        val now = millis(2026, Calendar.NOVEMBER, 12)
        val (start, end) = AnniversaryDates.sameWeekLastYearBounds(now)
        assertTrue(end > start)
        // Неделя — 7 суток.
        assertEquals(7L * 24 * 60 * 60 * 1000, end - start)
        // Сдвиг на календарный год: та же дата год назад попадает внутрь диапазона.
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(2025, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, startCal.get(Calendar.MONTH))
        assertEquals(9, startCal.get(Calendar.DAY_OF_MONTH))
        val lastYearNow = AnniversaryDates.plusYears(now, -1)
        assertTrue(lastYearNow in start until end)
    }
}
