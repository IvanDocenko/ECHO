package ru.echoapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.echoapp.notifications.NotificationHelper
import ru.echoapp.settings.SettingsStore
import kotlinx.coroutines.flow.first
import java.util.Calendar

/** Ежедневное напоминание сделать запись (срабатывает только в выбранные дни недели). */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext).settings.first()
        if (!settings.reminderEnabled) return Result.success()

        // Calendar: воскресенье = 1 … суббота = 7; наши дни: пн = 1 … вс = 7.
        val dayOfWeek = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
        if (dayOfWeek !in settings.reminderDays) return Result.success()

        NotificationHelper.showDailyReminder(applicationContext)
        return Result.success()
    }
}
