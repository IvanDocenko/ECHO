package ru.echoapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ru.echoapp.MainActivity
import ru.echoapp.R

object NotificationHelper {

    const val CHANNEL_ANNIVERSARY = "anniversary"
    const val CHANNEL_REMINDER = "reminder"
    const val CHANNEL_SMART = "smart"

    const val EXTRA_OPEN_LETTER = "ru.echoapp.extra.OPEN_LETTER"
    const val EXTRA_OPEN_RECORDING = "ru.echoapp.extra.OPEN_RECORDING"
    const val EXTRA_OPEN_PAST_WEEK = "ru.echoapp.extra.OPEN_PAST_WEEK"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ANNIVERSARY,
                context.getString(R.string.channel_anniversary),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_anniversary_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.channel_reminder),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_reminder_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SMART,
                context.getString(R.string.channel_smart),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_smart_desc) },
        )
    }

    private fun openIntent(context: Context, extraKey: String, id: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(extraKey, id)
        return PendingIntent.getActivity(
            context,
            (extraKey + id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Уведомление-годовщина; нажатие открывает экран «Письмо из прошлого». */
    fun showAnniversary(context: Context, recordingId: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ANNIVERSARY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_anniversary_title))
            .setContentText(context.getString(R.string.notif_anniversary_text))
            .setContentIntent(openIntent(context, EXTRA_OPEN_LETTER, recordingId))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("anniversary".hashCode() + recordingId.toInt(), notification)
    }

    fun showDailyReminder(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(context.getString(R.string.notif_reminder_text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("daily_reminder".hashCode(), notification)
    }

    /** Случайная запись недели; нажатие открывает экран воспроизведения. */
    fun showWeeklyRandom(context: Context, recordingId: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ANNIVERSARY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_weekly_title))
            .setContentText(context.getString(R.string.notif_weekly_text))
            .setContentIntent(openIntent(context, EXTRA_OPEN_RECORDING, recordingId))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("weekly".hashCode(), notification)
    }

    /** «Неделя из прошлого»; нажатие открывает подборку. */
    fun showPastWeek(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_OPEN_PAST_WEEK, true)
        val pending = PendingIntent.getActivity(
            context, "past_week".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SMART)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_past_week_title))
            .setContentText(context.getString(R.string.notif_past_week_text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("past_week".hashCode(), notification)
    }

    /** Настойчивое напоминание для серии на грани; открывает приложение. */
    fun showStreakSave(context: Context, streak: Int) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, "streak_save".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SMART)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_streak_title))
            .setContentText(context.getString(R.string.notif_streak_text, streak))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("streak_save".hashCode(), notification)
    }

    /** Мягкое вечернее напоминание, когда сегодня ещё нет записи. */
    fun showEveningReminder(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(context.getString(R.string.notif_evening_text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("daily_reminder".hashCode(), notification)
    }
}
