package ru.echoapp.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.echoapp.data.RecordingEntity
import ru.echoapp.notifications.NotificationHelper
import ru.echoapp.util.AnniversaryDates
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val WORK_ANNIVERSARY_PREFIX = "anniversary_"
    private const val WORK_DAILY_REMINDER = "daily_reminder"
    private const val WORK_WEEKLY_RANDOM = "weekly_random"
    private const val WORK_PAST_WEEK = "past_week"
    private const val WORK_SMART_REMINDER = "smart_reminder"

    /** Планирует показ записи ровно через год после её создания. */
    fun scheduleAnniversary(context: Context, recordingId: Long, anniversaryAt: Long) {
        val delay = AnniversaryDates.delayUntil(anniversaryAt, System.currentTimeMillis())
        val request = OneTimeWorkRequestBuilder<AnniversaryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(AnniversaryWorker.KEY_RECORDING_ID, recordingId).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$WORK_ANNIVERSARY_PREFIX$recordingId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelAnniversary(context: Context, recordingId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("$WORK_ANNIVERSARY_PREFIX$recordingId")
    }

    /** Показывает уведомление немедленно (для пропущенных годовщин). */
    fun notifyAnniversary(context: Context, recording: RecordingEntity) {
        NotificationHelper.showAnniversary(context, recording.id)
    }

    /** Ежедневное напоминание; worker сам проверяет выбранные дни недели. */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_DAILY_REMINDER)
        if (!enabled) return

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilTime(hour, minute), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Случайная запись недели — раз в 7 дней. */
    fun scheduleWeeklyRandom(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_WEEKLY_RANDOM)
        if (!enabled) return

        val request = PeriodicWorkRequestBuilder<WeeklyRandomWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextWeekly(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_WEEKLY_RANDOM,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** «Неделя из прошлого» — воскресенье 12:00, отдельно от «записи недели». */
    fun schedulePastWeek(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_PAST_WEEK)
        if (!enabled) return

        val request = PeriodicWorkRequestBuilder<PastWeekWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(weekday = Calendar.SUNDAY, hour = 12), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_PAST_WEEK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Умные напоминания: 20:00 — нет записи к вечеру; 23:00 — серия на грани. */
    fun scheduleSmartReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_SMART_REMINDER)

        val now = System.currentTimeMillis()
        val evening = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val streakHour = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        workManager.enqueueUniquePeriodicWork(
            WORK_SMART_REMINDER + "_evening",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SmartReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(evening - now, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putBoolean(SmartReminderWorker.KEY_STREAK_MODE, false).build())
                .build(),
        )
        workManager.enqueueUniquePeriodicWork(
            WORK_SMART_REMINDER + "_streak",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SmartReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(streakHour - now, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putBoolean(SmartReminderWorker.KEY_STREAK_MODE, true).build())
                .build(),
        )
    }

    /** До ближайшего [weekday] [hour]:00. */
    private fun delayUntilNext(weekday: Int, hour: Int): Long {
        val now = System.currentTimeMillis()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != weekday || timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
        return target - now
    }

    private fun delayUntilTime(hour: Int, minute: Int): Long {        val now = System.currentTimeMillis()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        return target - now
    }

    /** До ближайшей субботы 11:00 — «письмо недели». */
    private fun delayUntilNextWeekly(): Long {
        val now = System.currentTimeMillis()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Calendar: суббота = 7
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY || timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
        return target - now
    }
}
