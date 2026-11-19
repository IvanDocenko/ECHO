package ru.echoapp.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.echoapp.MainActivity
import ru.echoapp.R
import ru.echoapp.data.AppDatabase
import ru.echoapp.util.StreakCalculator

/** Виджет рабочего стола: огонёк серии + кнопка «Записать» (открывает экран записи). */
class RecordWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dao = AppDatabase.get(context).recordingDao()
                val streak = StreakCalculator.computeStreak(
                    dao.getAllOnce().filter { it.kind != "letter" }.map { it.createdAt },
                    System.currentTimeMillis(),
                )
                val views = RemoteViews(context.packageName, R.layout.widget_record).apply {
                    setTextViewText(R.id.widget_streak, if (streak > 0) "🔥 $streak" else "")
                    setOnClickPendingIntent(R.id.widget_record_button, openRecordIntent(context))
                }
                withContext(Dispatchers.Main) {
                    appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
                }
            }
            pending.finish()
        }
    }

    private fun openRecordIntent(context: Context): android.app.PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return android.app.PendingIntent.getActivity(
            context, "widget".hashCode(), intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Обновляет все экземпляры виджета (вызывается после сохранения/удаления записи). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, RecordWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            val intent = Intent(context, RecordWidgetProvider::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
