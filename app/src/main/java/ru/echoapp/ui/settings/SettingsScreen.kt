package ru.echoapp.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.echoapp.BuildConfig
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.data.ExportHelper
import ru.echoapp.data.ImportHelper
import ru.echoapp.settings.AppSettings
import ru.echoapp.settings.AudioQuality
import ru.echoapp.ui.lock.PinPad
import ru.echoapp.ui.theme.ThemeKey
import ru.echoapp.ui.theme.ThemePalette
import ru.echoapp.work.WorkScheduler
import java.util.Calendar

@Composable
fun SettingsScreen(app: EchoApp) {
    val settings by app.settingsStore.settings.collectAsState(initial = null)
    val current = settings ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
        )

        RemindersSection(app, current)
        AppearanceSection(app, current)
        LetterAiSection(app, current)
        PrivacySection(app, current)
        RecordingSection(app, current)
        DataSection(app, current)
        AboutCard()

        Text(
            text = stringResource(R.string.settings_offline_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Напоминания ───────────────────────────────────────────────

@Composable
private fun RemindersSection(app: EchoApp, current: AppSettings) {
    val scope = rememberCoroutineScope()
    val dayLabels = listOf(
        R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
        R.string.day_fri, R.string.day_sat, R.string.day_sun,
    )

    fun reschedule() {
        WorkScheduler.scheduleDailyReminder(app, current.reminderHour, current.reminderMinute, current.reminderEnabled)
    }

    SectionCard(stringResource(R.string.section_reminders)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_reminder), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = current.reminderEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        app.settingsStore.setReminder(enabled, current.reminderHour, current.reminderMinute, current.reminderDays)
                        reschedule()
                    }
                },
            )
        }
        if (current.reminderEnabled) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_time, "%02d:%02d".format(current.reminderHour, current.reminderMinute)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = current.reminderHour.toFloat(),
                onValueChange = { h ->
                    scope.launch {
                        app.settingsStore.setReminder(true, h.toInt(), current.reminderMinute, current.reminderDays)
                        reschedule()
                    }
                },
                valueRange = 0f..23f,
                steps = 23,
            )
            Slider(
                value = current.reminderMinute.toFloat(),
                onValueChange = { m ->
                    scope.launch {
                        app.settingsStore.setReminder(true, current.reminderHour, m.toInt(), current.reminderDays)
                        reschedule()
                    }
                },
                valueRange = 0f..59f,
                steps = 59,
            )
            Spacer(Modifier.height(8.dp))
            // Дни недели.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dayLabels.forEachIndexed { index, labelRes ->
                    val day = index + 1
                    val selected = day in current.reminderDays
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                            )
                            .clickable {
                                val newDays = if (selected) {
                                    current.reminderDays - day
                                } else {
                                    current.reminderDays + day
                                }
                                if (newDays.isNotEmpty()) {
                                    scope.launch {
                                        app.settingsStore.setReminder(true, current.reminderHour, current.reminderMinute, newDays)
                                        reschedule()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.weekly_random), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = current.weeklyRandomEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            app.settingsStore.setWeeklyRandom(enabled)
                            WorkScheduler.scheduleWeeklyRandom(app, enabled)
                        }
                    },
                )
            }
        }
        // «Неделя из прошлого» — независимо от ежедневного напоминания.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.past_week_setting), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = current.pastWeekEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        app.settingsStore.setPastWeekEnabled(enabled)
                        WorkScheduler.schedulePastWeek(app, enabled)
                    }
                },
            )
        }
    }
}

// ─── Внешний вид ───────────────────────────────────────────────

@Composable
private fun AppearanceSection(app: EchoApp, current: AppSettings) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    SectionCard(stringResource(R.string.section_appearance)) {
        // Пресеты тем — живые превью-карточки.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePalette.presets.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { palette ->
                        ThemePreviewCard(
                            palette = palette,
                            active = !current.dynamicColors && current.themeKey == palette.key.name,
                            onClick = {
                                scope.launch { app.settingsStore.setTheme(palette.key.name, dynamicColors = false) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            // Авто по времени суток.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { app.settingsStore.setTheme(ThemeKey.AUTO.name, dynamicColors = false) } }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = current.themeKey == ThemeKey.AUTO.name && !current.dynamicColors,
                    onClick = null,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.theme_auto), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(R.string.theme_auto_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Кастомная тема: акцент и фон.
        CustomThemeEditor(app, current)

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.dynamic_colors), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = current.dynamicColors,
                onCheckedChange = { enabled ->
                    scope.launch { app.settingsStore.setTheme(current.themeKey, enabled) }
                },
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.font_size) + ": ${(current.fontScale * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = current.fontScale,
            onValueChange = { scale ->
                scope.launch { app.settingsStore.setFontScale(scale) }
            },
            valueRange = 0.85f..1.3f,
        )

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            listOf("en" to R.string.lang_en, "ru" to R.string.lang_ru).forEach { (tag, labelRes) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentTag == tag,
                            onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = currentTag == tag, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(labelRes))
                }
            }
        }
    }
}

/** Живое превью темы: миниатюра карточки записи в этой палитре. */
@Composable
private fun ThemePreviewCard(
    palette: ThemePalette,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(palette.moodColors[3], CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(paletteNameRes(palette.key)),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = if (palette.serifEverywhere) FontFamily.Serif else FontFamily.Default,
                    color = palette.onSurface,
                )
                if (active) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.theme_active),
                        modifier = Modifier.size(14.dp),
                        tint = palette.primary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "12 November",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Serif,
                color = palette.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            // Мини-waveform.
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                val heights = listOf(6, 12, 8, 14, 10, 16, 7, 11)
                heights.forEach { h ->
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(h.dp)
                            .background(palette.primary, RoundedCornerShape(2.dp)),
                    )
                }
            }
            if (active) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.theme_active),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primary,
                )
            }
        }
    }
}

/** Редактор кастомной темы: выбор акцента и фона из палитры свотчей. */
@Composable
private fun CustomThemeEditor(app: EchoApp, current: AppSettings) {
    val scope = rememberCoroutineScope()
    val isCustom = current.themeKey == ThemeKey.CUSTOM.name && !current.dynamicColors
    val accentSwatches = listOf(
        0xFFFFB74D, 0xFF8F7BFF, 0xFF2E6FB7, 0xFF81C784,
        0xFFFF5C8A, 0xFFE57373, 0xFF4DD0E1, 0xFFA3B18A,
    )
    val backgroundSwatches = listOf(
        0xFF121212, 0xFF050508, 0xFF0D1B2A, 0xFF1B2F1B,
        0xFFF7F3E9, 0xFFFFFFFF, 0xFFF2F7FC, 0xFFF0E6D2,
    )

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isCustom, onClick = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.theme_custom), style = MaterialTheme.typography.bodyMedium)
            if (isCustom) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .padding(1.dp)
                        .background(Color(current.customAccent), CircleShape),
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .size(16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .padding(1.dp)
                        .background(Color(current.customBackground), CircleShape),
                )
            }
        }
        if (isCustom) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.theme_custom_accent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SwatchRow(
                colors = accentSwatches,
                selected = current.customAccent,
                onSelect = { scope.launch { app.settingsStore.setCustomTheme(it, current.customBackground) } },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.theme_custom_background),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SwatchRow(
                colors = backgroundSwatches,
                selected = current.customBackground,
                onSelect = { scope.launch { app.settingsStore.setCustomTheme(current.customAccent, it) } },
            )
        } else {
            TextButton(onClick = {
                scope.launch { app.settingsStore.setTheme(ThemeKey.CUSTOM.name, dynamicColors = false) }
            }) {
                Text(stringResource(R.string.theme_custom_edit))
            }
        }
    }
}

@Composable
private fun SwatchRow(colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { argb ->
            Box(
                Modifier
                    .size(28.dp)
                    .border(1.dp, borderColor, CircleShape)
                    .padding(2.dp)
                    .background(Color(argb), CircleShape)
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (argb == selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.theme_active),
                        tint = if (isDarkColor(argb)) Color.White else Color.Black,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

private fun isDarkColor(argb: Long): Boolean {
    val c = Color(argb)
    return 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue < 0.35f
}

private fun paletteNameRes(key: ThemeKey): Int = when (key) {
    ThemeKey.CASSETTE -> R.string.theme_cassette
    ThemeKey.PAPER -> R.string.theme_paper
    ThemeKey.MIDNIGHT -> R.string.theme_midnight
    ThemeKey.FOREST -> R.string.theme_forest
    ThemeKey.VINTAGE -> R.string.theme_vintage
    ThemeKey.NEON -> R.string.theme_neon
    ThemeKey.FROST -> R.string.theme_frost
    ThemeKey.AUTO -> R.string.theme_auto
    ThemeKey.CUSTOM -> R.string.theme_custom
}

// ─── AI-письмо за год ──────────────────────────────────────────

@Composable
private fun LetterAiSection(app: EchoApp, current: AppSettings) {
    val scope = rememberCoroutineScope()
    SectionCard(stringResource(R.string.section_letter_ai)) {
        Text(
            text = stringResource(R.string.letter_ai_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.selectableGroup()) {
            listOf(
                "OFF" to R.string.letter_ai_off,
                "LOCAL" to R.string.letter_ai_local,
            ).forEach { (mode, labelRes) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = current.letterAi == mode,
                            onClick = { scope.launch { app.settingsStore.setLetterAi(mode) } },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = current.letterAi == mode, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(labelRes))
                }
            }
        }
    }
}

// ─── Приватность ───────────────────────────────────────────────

@Composable
private fun PrivacySection(app: EchoApp, current: AppSettings) {
    val scope = rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }

    SectionCard(stringResource(R.string.section_privacy)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pin_lock), style = MaterialTheme.typography.titleMedium)
            }
            if (current.pinEnabled) {
                Row {
                    TextButton(onClick = { showPinDialog = true }) {
                        Text(stringResource(R.string.pin_change))
                    }
                    TextButton(onClick = {
                        scope.launch { app.settingsStore.setPin(enabled = false, code = "") }
                    }) {
                        Text(stringResource(R.string.pin_disable))
                    }
                }
            } else {
                TextButton(onClick = { showPinDialog = true }) {
                    Text(stringResource(R.string.pin_set))
                }
            }
        }

        if (current.pinEnabled) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.biometric_unlock), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = current.biometricEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { app.settingsStore.setBiometric(enabled) }
                    },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.hide_previews), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = current.hidePreviews,
                onCheckedChange = { enabled ->
                    scope.launch { app.settingsStore.setHidePreviews(enabled) }
                },
            )
        }
    }

    if (showPinDialog) {
        SetPinDialog(
            onDismiss = { showPinDialog = false },
            onSave = { code ->
                scope.launch { app.settingsStore.setPin(enabled = true, code = code) }
                showPinDialog = false
            },
        )
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_create), fontFamily = FontFamily.Serif) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { index ->
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(
                                    color = if (index < pin.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                PinPad(
                    onDigit = { d -> if (pin.length < 4) pin += d },
                    onBackspace = { pin = pin.dropLast(1) },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length == 4, onClick = { onSave(pin) }) {
                Text(stringResource(R.string.onboarding_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ─── Запись ────────────────────────────────────────────────────

@Composable
private fun RecordingSection(app: EchoApp, current: AppSettings) {
    val scope = rememberCoroutineScope()
    SectionCard(stringResource(R.string.section_recording)) {
        Column(Modifier.selectableGroup()) {
            listOf(
                AudioQuality.ECO to R.string.quality_eco,
                AudioQuality.STANDARD to R.string.quality_standard,
                AudioQuality.HIGH to R.string.quality_high,
            ).forEach { (quality, labelRes) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = current.audioQuality == quality,
                            onClick = {
                                scope.launch { app.settingsStore.setAudioQuality(quality) }
                            },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = current.audioQuality == quality, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(labelRes))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.auto_stop_silence), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = current.autoStopSilence,
                onCheckedChange = { enabled ->
                    scope.launch { app.settingsStore.setAutoStopSilence(enabled) }
                },
            )
        }
    }
}

// ─── Данные ────────────────────────────────────────────────────

@Composable
private fun DataSection(app: EchoApp, current: AppSettings) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCleanupConfirm by remember { mutableStateOf(false) }
    var importSummary by remember { mutableStateOf<ImportHelper.Result?>(null) }
    var importFailed by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = ImportHelper.import(context, uri, app.repository)
                if (result == null) importFailed = true else importSummary = result
            }
        }
    }

    fun exportAndShare() {
        scope.launch {
            val recordings = app.repository.allRecordingsOnce()
            if (recordings.isEmpty()) {
                Toast.makeText(context, R.string.export_empty, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val file = ExportHelper.export(context, recordings, app.repository.allRepliesOnce())
            if (file == null) {
                Toast.makeText(context, R.string.record_error_save, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, null)) }
        }
    }

    SectionCard(stringResource(R.string.section_data)) {
        OutlinedButton(
            onClick = { exportAndShare() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.export_zip))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.import_zip))
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.cleanup_run), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.selectableGroup()) {
            listOf(
                0 to R.string.keep_forever,
                3 to R.string.older_than_3m,
                6 to R.string.older_than_6m,
                12 to R.string.older_than_12m,
            ).forEach { (months, labelRes) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = current.retentionMonths == months,
                            onClick = {
                                scope.launch { app.settingsStore.setRetentionMonths(months) }
                            },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = current.retentionMonths == months, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(labelRes))
                }
            }
        }
        OutlinedButton(
            onClick = { showCleanupConfirm = true },
            enabled = current.retentionMonths > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.delete))
        }
    }

    if (showCleanupConfirm) {
        AlertDialog(
            onDismissRequest = { showCleanupConfirm = false },
            title = { Text(stringResource(R.string.cleanup_confirm_title), fontFamily = FontFamily.Serif) },
            text = { Text(stringResource(R.string.cleanup_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showCleanupConfirm = false
                    val months = current.retentionMonths
                    if (months > 0) {
                        scope.launch {
                            val calendar = Calendar.getInstance()
                            calendar.add(Calendar.MONTH, -months)
                            val deleted = app.repository.cleanupOlderThan(calendar.timeInMillis)
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (deleted > 0) R.string.cleanup_done else R.string.cleanup_nothing,
                                    deleted,
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (importSummary != null) {
        val summary = importSummary!!
        AlertDialog(
            onDismissRequest = { importSummary = null },
            title = { Text(stringResource(R.string.import_done_title), fontFamily = FontFamily.Serif) },
            text = {
                Text(
                    stringResource(
                        R.string.import_done_text,
                        summary.imported,
                        summary.skippedDuplicates,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { importSummary = null }) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    if (importFailed) {
        AlertDialog(
            onDismissRequest = { importFailed = false },
            title = { Text(stringResource(R.string.import_failed_title), fontFamily = FontFamily.Serif) },
            text = { Text(stringResource(R.string.import_failed_text)) },
            confirmButton = {
                TextButton(onClick = { importFailed = false }) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

// ─── О приложении ──────────────────────────────────────────────

@Composable
private fun AboutCard() {
    SectionCard(stringResource(R.string.settings_about)) {
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        AboutRow(stringResource(R.string.about_developer), stringResource(R.string.about_developer_name))
        Spacer(Modifier.height(4.dp))
        AboutRow(stringResource(R.string.about_version), BuildConfig.VERSION_NAME)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
