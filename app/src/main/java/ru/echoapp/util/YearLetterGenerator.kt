package ru.echoapp.util

import ru.echoapp.data.RecordingEntity
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Локальный генератор «Письма за год»: собирает факты из записей года
 * и складывает из них тёплое письмо от прошлого себя. Без сети, без моделей —
 * чистая статистика + шаблоны, выбор вариантов — по хэшу фактов, чтобы
 * письмо было стабильным при повторной генерации в тот же день.
 */
object YearLetterGenerator {

    data class YearFacts(
        val recordingsCount: Int,
        val daysWithRecordings: Int,
        val topMood: Int,
        val topMoodCount: Int,
        val longestStreak: Int,
        val firstDate: Long?,
        val lastDate: Long?,
        val quote: String?,
        val sealedCount: Int,
        val totalSeconds: Long,
    )

    /** Собирает факты по записям за последние 365 дней (письма исключаем). */
    fun factsFrom(recordings: List<RecordingEntity>, now: Long = System.currentTimeMillis()): YearFacts {
        val year = recordings
            .filter { it.kind != "letter" && it.createdAt >= now - 365L * 24 * 60 * 60 * 1000 }
            .sortedBy { it.createdAt }
        if (year.isEmpty()) {
            return YearFacts(0, 0, -1, 0, 0, null, null, null, 0, 0)
        }
        val moodCounts = year.filter { it.mood >= 0 }.groupingBy { it.mood }.eachCount()
        val topMood = moodCounts.maxByOrNull { it.value }?.key ?: -1
        val streak = StreakCalculator.computeStreak(year.map { it.createdAt }, now)
        val quote = year.mapNotNull { it.transcript?.trim() }
            .firstOrNull { it.length in 12..180 }
        return YearFacts(
            recordingsCount = year.size,
            daysWithRecordings = year.map { dayOf(it.createdAt) }.distinct().size,
            topMood = topMood,
            topMoodCount = moodCounts[topMood] ?: 0,
            longestStreak = streak,
            firstDate = year.first().createdAt,
            lastDate = year.last().createdAt,
            quote = quote,
            sealedCount = year.count { it.sealedUntil != null && it.sealedUntil > now },
            totalSeconds = year.sumOf { it.durationMs } / 1000,
        )
    }

    fun generate(facts: YearFacts, locale: Locale = Locale.getDefault()): String {
        val russian = locale.language == "ru"
        return if (facts.recordingsCount == 0) {
            if (russian) {
                "Это письмо пустое — за год не оказалось ни одной записи. " +
                    "Но оно всё равно пришло, потому что следующий год можно начать сегодня. " +
                    "Тридцати секунд хватит."
            } else {
                "This letter is empty — there were no recordings this year. " +
                    "But it still arrived, because the next year can start today. " +
                    "Thirty seconds is enough."
            }
        } else {
            buildString {
                append(opening(facts, russian)).append("\n\n")
                append(body(facts, russian)).append("\n\n")
                append(closing(facts, russian))
            }
        }
    }

    private fun pick(facts: YearFacts, variants: List<String>): String {
        val seed = facts.recordingsCount * 31 + facts.daysWithRecordings * 7 + facts.topMoodCount
        return variants[abs(seed) % variants.size]
    }

    private fun opening(facts: YearFacts, ru: Boolean): String = if (ru) {
        pick(
            facts,
            listOf(
                "Год назад ты боялся этого. Смотри, что получилось.",
                "Привет. Это ты — через год.",
                "Ты просил(а) напомнить. Год пролетел.",
            ),
        )
    } else {
        pick(
            facts,
            listOf(
                "A year ago you were afraid of this. Look what happened.",
                "Hi. It's you — one year later.",
                "You asked to be reminded. The year flew by.",
            ),
        )
    }

    private fun body(facts: YearFacts, ru: Boolean): String {
        val parts = mutableListOf<String>()
        if (ru) {
            parts += "За этот год ты сделал(а) ${facts.recordingsCount} " + plural(facts.recordingsCount, "запись", "записи", "записей") +
                " — это ${facts.daysWithRecordings} " + plural(facts.daysWithRecordings, "день", "дня", "дней") + " голосом."
            if (facts.topMood >= 0) {
                parts += "Чаще всего рядом с твоим голосом было настроение ${Mood.emoji(facts.topMood)} — " +
                    "${facts.topMoodCount} " + plural(facts.topMoodCount, "раз", "раза", "раз") + "."
            }
            if (facts.longestStreak >= 3) {
                parts += "Самая длинная серия — ${facts.longestStreak} " +
                    plural(facts.longestStreak, "день", "дня", "дней") + " подряд. Ты умеешь держать ритм."
            }
            if (facts.totalSeconds > 0) {
                val minutes = facts.totalSeconds / 60
                parts += "В сумме это примерно $minutes " + plural(minutes.toInt(), "минута", "минуты", "минут") + " твоей жизни, записанной вслух."
            }
            facts.quote?.let { parts += "Помнишь, ты говорил(а): «$it»" }
            if (facts.sealedCount > 0) {
                parts += "И ещё ${facts.sealedCount} " +
                    plural(facts.sealedCount, "запись ждёт", "записи ждут", "записей ждут") + " своего часа — ты запечатал(а) их для будущего."
            }
        } else {
            parts += "Over this year you made ${facts.recordingsCount} recording(s) — ${facts.daysWithRecordings} days with your own voice."
            if (facts.topMood >= 0) {
                parts += "Most often your voice was accompanied by ${Mood.emoji(facts.topMood)} — ${facts.topMoodCount} times."
            }
            if (facts.longestStreak >= 3) {
                parts += "Your longest streak was ${facts.longestStreak} days in a row. You know how to keep a rhythm."
            }
            if (facts.totalSeconds > 0) {
                parts += "In total it's about ${facts.totalSeconds / 60} minutes of your life recorded out loud."
            }
            facts.quote?.let { parts += "Remember you said: \"$it\"" }
            if (facts.sealedCount > 0) {
                parts += "And ${facts.sealedCount} more recording(s) are still waiting for their moment — you sealed them for the future."
            }
        }
        return parts.joinToString("\n")
    }

    private fun closing(facts: YearFacts, ru: Boolean): String = if (ru) {
        pick(
            facts,
            listOf(
                "Год назад ты оставил(а) эти слова себе. Надеюсь, будущее оправдало ожидания. — Ты из прошлого",
                "Храни этот голос. Следующее письмо придёт через год. — Ты из прошлого",
                "Спасибо, что записывал(а). Слышишь, как далеко ты зашёл(ла)? — Ты из прошлого",
            ),
        )
    } else {
        pick(
            facts,
            listOf(
                "A year ago you left these words for yourself. I hope the future lived up to them. — You, from the past",
                "Keep this voice safe. The next letter arrives in a year. — You, from the past",
                "Thank you for recording. Can you hear how far you've come? — You, from the past",
            ),
        )
    }

    private fun dayOf(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return calendar.get(Calendar.YEAR) * 1000L + calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun plural(n: Int, one: String, few: String, many: String): String {
        val mod100 = n % 100
        val mod10 = n % 10
        return when {
            mod100 in 11..19 -> many
            mod10 == 1 -> one
            mod10 in 2..4 -> few
            else -> many
        }
    }
}
