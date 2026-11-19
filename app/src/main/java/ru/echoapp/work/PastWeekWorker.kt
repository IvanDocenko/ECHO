package ru.echoapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.echoapp.notifications.NotificationHelper
import ru.echoapp.settings.SettingsStore
import ru.echoapp.util.AnniversaryDates
import kotlinx.coroutines.flow.first

/** «Неделя из прошлого»: раз в неделю уведомляет о записях за эту неделю год назад. */
class PastWeekWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext).settings.first()
        if (!settings.pastWeekEnabled) return Result.success()

        val dao = ru.echoapp.data.AppDatabase.get(applicationContext).recordingDao()
        val (start, end) = AnniversaryDates.sameWeekLastYearBounds(System.currentTimeMillis())
        val count = dao.between(start, end).count { it.kind != "letter" }
        if (count == 0) return Result.success()

        NotificationHelper.showPastWeek(applicationContext)
        return Result.success()
    }
}
