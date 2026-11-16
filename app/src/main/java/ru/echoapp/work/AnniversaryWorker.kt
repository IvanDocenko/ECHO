package ru.echoapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.echoapp.data.AppDatabase
import ru.echoapp.notifications.NotificationHelper

/** Срабатывает ровно через год после записи и показывает уведомление. */
class AnniversaryWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong(KEY_RECORDING_ID, -1L)
        if (recordingId == -1L) return Result.failure()

        val dao = AppDatabase.get(applicationContext).recordingDao()
        val recording = dao.getById(recordingId) ?: return Result.success()
        if (recording.anniversaryShown) return Result.success()

        NotificationHelper.showAnniversary(applicationContext, recordingId)
        dao.markAnniversaryShown(recordingId)
        return Result.success()
    }

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
    }
}
