package ru.echoapp.util

import java.util.Calendar
import java.util.Locale

/** Чистая логика дат для годовщин — покрыта unit-тестами. */
object AnniversaryDates {

    /** Добавляет [years] лет к моменту времени с учётом календаря (високосные года, 29 февраля и т.п.). */
    fun plusYears(millis: Long, years: Int = 1): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.add(Calendar.YEAR, years)
        return calendar.timeInMillis
    }

    /** Границы суток (00:00:00 включительно, следующие 00:00:00 исключительно) для даты, содержащей [millis]. */
    fun dayBounds(millis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return start to calendar.timeInMillis
    }

    /** Наступила ли годовщина к моменту [now]. */
    fun isDue(anniversaryAt: Long, now: Long): Boolean = now >= anniversaryAt

    /** Сколько миллисекунд осталось до годовщины (0, если уже наступила). */
    fun delayUntil(anniversaryAt: Long, now: Long): Long = maxOf(0L, anniversaryAt - now)

    /** Границы сутогодовой давности: тот же день и месяц, год назад от [now]. */
    fun sameDayLastYearBounds(now: Long): Pair<Long, Long> = dayBounds(plusYears(now, -1))

    /** Границы календарного года, содержащего [millis] (1 янв. 00:00 — 1 янв. след. года). */
    fun yearBounds(millis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.YEAR, 1)
        return start to calendar.timeInMillis
    }

    /** Границы недели (пн 00:00 — след. пн 00:00) для того же периода год назад от [now]. */
    fun sameWeekLastYearBounds(now: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        // Откатываемся к понедельнику: Calendar.MONDAY = 2.
        val shift = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -shift)
        val start = plusYears(calendar.timeInMillis, -1)
        val end = plusYears(calendar.apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis, -1)
        return start to end
    }

    /** "12 ноября 2026" / "12 November 2026" — для карточек и писем. */
    fun formatFullDate(millis: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = java.text.SimpleDateFormat("d MMMM yyyy", locale)
        return pattern.format(java.util.Date(millis))
    }

    /** "12 ноября" / "12 November" — короткая дата для ленты. */
    fun formatShortDate(millis: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = java.text.SimpleDateFormat("d MMMM", locale)
        return pattern.format(java.util.Date(millis))
    }

    /** "0:42" / "1:05" — длительность и таймер записи. */
    fun formatElapsed(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
