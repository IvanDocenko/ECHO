package ru.echoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.echoapp.data.RecordingEntity
import java.util.Calendar
import java.util.Locale

class YearLetterGeneratorTest {

    private fun millis(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        set(year, month, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val now = millis(2026, Calendar.NOVEMBER, 12)

    private fun recording(createdAt: Long, mood: Int = -1, transcript: String? = null) = RecordingEntity(
        id = 0,
        filePath = "",
        createdAt = createdAt,
        durationMs = 30_000,
        anniversaryAt = createdAt,
        mood = mood,
        transcript = transcript,
    )

    @Test
    fun `empty year gives gentle empty letter`() {
        val facts = YearLetterGenerator.factsFrom(emptyList(), now)
        assertEquals(0, facts.recordingsCount)
        val text = YearLetterGenerator.generate(facts, Locale.ENGLISH)
        assertTrue(text.isNotBlank())
    }

    @Test
    fun `facts count recordings of last year only`() {
        val recordings = listOf(
            recording(millis(2025, Calendar.DECEMBER, 1), mood = 3),
            recording(millis(2026, Calendar.NOVEMBER, 2), mood = 3),
            recording(millis(2020, Calendar.JANUARY, 1), mood = 0), // старше года — игнор
        )
        val facts = YearLetterGenerator.factsFrom(recordings, now)
        assertEquals(2, facts.recordingsCount)
        assertEquals(3, facts.topMood)
        assertEquals(2, facts.topMoodCount)
    }

    @Test
    fun `letters are excluded from facts`() {
        val recordings = listOf(
            recording(millis(2026, Calendar.NOVEMBER, 1)),
            recording(millis(2026, Calendar.NOVEMBER, 2)).copy(kind = "letter", transcript = "hello"),
        )
        val facts = YearLetterGenerator.factsFrom(recordings, now)
        assertEquals(1, facts.recordingsCount)
    }

    @Test
    fun `generated letter mentions counts and streak`() {
        val recordings = (0..9).map {
            recording(millis(2026, Calendar.NOVEMBER, 3 + it), mood = 4, transcript = "a fine day indeed")
        }
        val facts = YearLetterGenerator.factsFrom(recordings, now)
        val text = YearLetterGenerator.generate(facts, Locale.ENGLISH)
        assertTrue(text.contains("10"))
        assertTrue(text.contains("streak", ignoreCase = true))
    }

    @Test
    fun `russian letter is generated for russian locale`() {
        val recordings = listOf(recording(millis(2026, Calendar.OCTOBER, 1), mood = 1))
        val text = YearLetterGenerator.generate(YearLetterGenerator.factsFrom(recordings, now), Locale("ru"))
        assertFalse(text.isBlank())
        // Кириллица присутствует.
        assertTrue(text.any { it in 'А'..'я' })
    }

    @Test
    fun `generation is stable for the same facts`() {
        val facts = YearLetterGenerator.factsFrom(
            listOf(recording(millis(2026, Calendar.JUNE, 1), mood = 2)),
            now,
        )
        val first = YearLetterGenerator.generate(facts, Locale.ENGLISH)
        val second = YearLetterGenerator.generate(facts, Locale.ENGLISH)
        assertEquals(first, second)
    }
}
