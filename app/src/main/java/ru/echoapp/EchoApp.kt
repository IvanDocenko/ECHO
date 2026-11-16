package ru.echoapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.echoapp.data.AppDatabase
import ru.echoapp.data.RecordingRepository
import ru.echoapp.notifications.NotificationHelper
import ru.echoapp.settings.SettingsStore
import ru.echoapp.work.WorkScheduler

class EchoApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: RecordingRepository by lazy {
        RecordingRepository(this, database.recordingDao())
    }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        appScope.launch {
            val settings = settingsStore.settings.first()
            // Старт на английском при первом запуске: система может помнить per-app язык
            // с прошлой установки (Android 13+ хранит его за пределами данных приложения),
            // поэтому опираемся на флаг онбординга, который стирается вместе с приложением.
            if (!settings.onboardingDone) {
                runCatching {
                    if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != "en") {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                    }
                }
            }
            // Подстраховка: если WorkManager не успел сработать — показываем пропущенные годовщины.
            repository.processMissedAnniversaries()
            WorkScheduler.scheduleDailyReminder(
                this@EchoApp,
                settings.reminderHour,
                settings.reminderMinute,
                settings.reminderEnabled,
            )
            WorkScheduler.scheduleWeeklyRandom(this@EchoApp, settings.weeklyRandomEnabled)
        }
    }
}
