package ru.echoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnniversaryDatesTest {

    private fun millis(
        year: Int, month: Int, day: Int,
        hour: Int = 0, minute: Int = 0,
    ): Long = Calendar.getInstance().apply {
        set(year, month, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `plusYears adds exactly one calendar year`() {
        val d = millis(2025, Calendar.NOVEMBER, 11, 20, 30)
        val result = Calendar.getInstance().apply { timeInMillis = AnniversaryDates.plusYears(d) }
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH))
        assertEquals(11, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, result.get(Calendar.MINUTE))
    }

    @Test
    fun `plusYears handles leap day`() {
        val leap = millis(2024, Calendar.FEBRUARY, 29)
        val result = Calendar.getInstance().apply { timeInMillis = AnniversaryDates.plusYears(leap) }
        // 29 февраля 2025 не существует — Calendar переносит на 28 февраля
        assertEquals(2025, result.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, result.get(Calendar.MONTH))
        assertEquals(28, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `plusYears negative goes to the past`() {
        val d = millis(2026, Calendar.JUNE, 15)
        val result = Calendar.getInstance().apply { timeInMillis = AnniversaryDates.plusYears(d, -1) }
        assertEquals(2025, result.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH))
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `dayBounds covers whole day`() {
        val noon = millis(2026, Calendar.MARCH, 10, 12, 0)
        val (start, end) = AnniversaryDates.dayBounds(noon)
        assertTrue(noon in start until end)
        assertEquals(24 * 60 * 60 * 1000L, end - start)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
    }

    @Test
    fun `isDue compares against now`() {
        val t = millis(2026, Calendar.JANUARY, 1)
        assertFalse(AnniversaryDates.isDue(t + 1000, t))
        assertTrue(AnniversaryDates.isDue(t, t))
        assertTrue(AnniversaryDates.isDue(t - 1000, t))
    }

    @Test
    fun `delayUntil clamps at zero`() {
        val t = millis(2026, Calendar.JANUARY, 1)
        assertEquals(5000L, AnniversaryDates.delayUntil(t + 5000, t))
        assertEquals(0L, AnniversaryDates.delayUntil(t - 5000, t))
    }

    @Test
    fun `sameDayLastYearBounds shifts year only`() {
        val now = millis(2026, Calendar.NOVEMBER, 11, 18, 0)
        val (start, end) = AnniversaryDates.sameDayLastYearBounds(now)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(2025, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, startCal.get(Calendar.MONTH))
        assertEquals(11, startCal.get(Calendar.DAY_OF_MONTH))
        val lastYearNoon = millis(2025, Calendar.NOVEMBER, 11, 12, 0)
        assertTrue(lastYearNoon in start until end)
    }

    @Test
    fun `formatElapsed renders minutes and seconds`() {
        assertEquals("0:00", AnniversaryDates.formatElapsed(0))
        assertEquals("0:42", AnniversaryDates.formatElapsed(42_000))
        assertEquals("1:05", AnniversaryDates.formatElapsed(65_000))
        assertEquals("2:00", AnniversaryDates.formatElapsed(120_000))
    }
}
