package ru.echoapp.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class AudioQuality { ECO, STANDARD, HIGH }

data class AppSettings(
    val onboardingDone: Boolean = false,
    // Напоминания
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    /** Дни недели (1 = пн … 7 = вс), по умолчанию все. */
    val reminderDays: Set<Int> = (1..7).toSet(),
    val weeklyRandomEnabled: Boolean = true,
    // Внешний вид
    val themeKey: String = "CASSETTE",
    val dynamicColors: Boolean = false,
    val fontScale: Float = 1f,
    // Приватность
    val pinEnabled: Boolean = false,
    val pinCode: String = "",
    val biometricEnabled: Boolean = false,
    val hidePreviews: Boolean = false,
    // Запись
    val audioQuality: AudioQuality = AudioQuality.STANDARD,
    val autoStopSilence: Boolean = false,
    // Данные
    val retentionMonths: Int = 0,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_DAYS = stringSetPreferencesKey("reminder_days")
        val WEEKLY_ENABLED = booleanPreferencesKey("weekly_random_enabled")
        val THEME_KEY = stringPreferencesKey("theme_key")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val HIDE_PREVIEWS = booleanPreferencesKey("hide_previews")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val AUTO_STOP_SILENCE = booleanPreferencesKey("auto_stop_silence")
        val RETENTION_MONTHS = intPreferencesKey("retention_months")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 21,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            reminderDays = prefs[Keys.REMINDER_DAYS]
                ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: (1..7).toSet(),
            weeklyRandomEnabled = prefs[Keys.WEEKLY_ENABLED] ?: true,
            themeKey = prefs[Keys.THEME_KEY] ?: "CASSETTE",
            dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: false,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1f,
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinCode = prefs[Keys.PIN_CODE] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            hidePreviews = prefs[Keys.HIDE_PREVIEWS] ?: false,
            audioQuality = prefs[Keys.AUDIO_QUALITY]
                ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                ?: AudioQuality.STANDARD,
            autoStopSilence = prefs[Keys.AUTO_STOP_SILENCE] ?: false,
            retentionMonths = prefs[Keys.RETENTION_MONTHS] ?: 0,
        )
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int, days: Set<Int>) {
        context.dataStore.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
            it[Keys.REMINDER_DAYS] = days.map { d -> d.toString() }.toSet()
        }
    }

    suspend fun setWeeklyRandom(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WEEKLY_ENABLED] = enabled }
    }

    suspend fun setTheme(key: String, dynamicColors: Boolean) {
        context.dataStore.edit {
            it[Keys.THEME_KEY] = key
            it[Keys.DYNAMIC_COLORS] = dynamicColors
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    suspend fun setPin(enabled: Boolean, code: String) {
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = enabled
            it[Keys.PIN_CODE] = code
        }
    }

    suspend fun setBiometric(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setHidePreviews(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_PREVIEWS] = enabled }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.AUDIO_QUALITY] = quality.name }
    }

    suspend fun setAutoStopSilence(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_STOP_SILENCE] = enabled }
    }

    suspend fun setRetentionMonths(months: Int) {
        context.dataStore.edit { it[Keys.RETENTION_MONTHS] = months }
    }
}
