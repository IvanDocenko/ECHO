package ru.echoapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.echoapp.data.AppDatabase
import ru.echoapp.notifications.NotificationHelper
import ru.echoapp.util.AnniversaryDates
import ru.echoapp.util.StreakCalculator

/**
 * Умные напоминания. Два режима (через KEY_STREAK_MODE):
 *  - вечерний (20:00): если сегодня ещё нет записи — мягкое напоминание;
 *  - «серия на грани» (23:00): если серия ≥ 3 дней и записи сегодня нет —
 *    настойчивое уведомление «Не сгори сегодня!».
 */
class SmartReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).recordingDao()
        val now = System.currentTimeMillis()
        val (dayStart, dayEnd) = AnniversaryDates.dayBounds(now)
        val hasToday = dao.between(dayStart, dayEnd).any { it.kind != "letter" }

        if (inputData.getBoolean(KEY_STREAK_MODE, false)) {
            if (hasToday) return Result.success()
            val streak = StreakCalculator.computeStreak(
                dao.getAllOnce().filter { it.kind != "letter" }.map { it.createdAt },
                now,
            )
            if (streak >= 3) {
                NotificationHelper.showStreakSave(applicationContext, streak)
            }
        } else if (!hasToday) {
            NotificationHelper.showEveningReminder(applicationContext)
        }
        return Result.success()
    }

    companion object {
        const val KEY_STREAK_MODE = "streak_mode"
    }
}
