package ru.echoapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.echoapp.notifications.NotificationHelper

/** Раз в неделю присылает случайную запись прошлого. */
class WeeklyRandomWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = ru.echoapp.data.AppDatabase
            .get(applicationContext)
            .recordingDao()
            .let { ru.echoapp.data.RecordingRepository(applicationContext, it) }
        // Запись не моложе 90 дней — «прошлое», а не вчерашний день.
        val recording = repository.randomPastRecording(minAgeMs = 90L * 24 * 60 * 60 * 1000)
            ?: repository.randomPastRecording(minAgeMs = 0)
            ?: return Result.success()
        NotificationHelper.showWeeklyRandom(applicationContext, recording.id)
        return Result.success()
    }
}
