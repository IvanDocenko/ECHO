package ru.echoapp.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class StreakCalculatorTest {

    private fun millis(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        set(year, month, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun `empty list gives zero`() {
        assertEquals(0, StreakCalculator.computeStreak(emptyList(), millis(2026, Calendar.NOVEMBER, 12)))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val now = millis(2026, Calendar.NOVEMBER, 12)
        val stamps = listOf(
            millis(2026, Calendar.NOVEMBER, 10),
            millis(2026, Calendar.NOVEMBER, 11),
            millis(2026, Calendar.NOVEMBER, 12),
        )
        assertEquals(3, StreakCalculator.computeStreak(stamps, now))
    }

    @Test
    fun `today without recording does not break streak`() {
        val now = millis(2026, Calendar.NOVEMBER, 12)
        val stamps = listOf(
            millis(2026, Calendar.NOVEMBER, 10),
            millis(2026, Calendar.NOVEMBER, 11),
        )
        assertEquals(2, StreakCalculator.computeStreak(stamps, now))
    }

    @Test
    fun `gap breaks streak`() {
        val now = millis(2026, Calendar.NOVEMBER, 12)
        val stamps = listOf(
            millis(2026, Calendar.NOVEMBER, 9),
            millis(2026, Calendar.NOVEMBER, 11),
            millis(2026, Calendar.NOVEMBER, 12),
        )
        assertEquals(2, StreakCalculator.computeStreak(stamps, now))
    }

    @Test
    fun `several recordings same day count once`() {
        val now = millis(2026, Calendar.NOVEMBER, 12)
        val stamps = listOf(
            millis(2026, Calendar.NOVEMBER, 11),
            millis(2026, Calendar.NOVEMBER, 12),
            millis(2026, Calendar.NOVEMBER, 12) + 3600_000,
        )
        assertEquals(2, StreakCalculator.computeStreak(stamps, now))
    }

    @Test
    fun `raw day offsets variant`() {
        val base = millis(2026, Calendar.JANUARY, 5)
        val stamps = listOf(base - 3 * dayMs, base - 2 * dayMs, base - dayMs)
        assertEquals(3, StreakCalculator.computeStreak(stamps, base))
    }
}
