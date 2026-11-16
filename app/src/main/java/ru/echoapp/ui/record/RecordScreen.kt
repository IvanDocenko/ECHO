package ru.echoapp.ui.record

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.settings.AudioQuality
import ru.echoapp.util.AnniversaryDates
import ru.echoapp.util.Mood
import ru.echoapp.util.StreakCalculator
import java.util.Calendar

private const val BAR_COUNT = 32

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    app: EchoApp,
    rootReplyId: Long?,
    parentReplyId: Long?,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Скоуп activity: запись не прерывается при навигации на другие экраны.
    val vm: RecordViewModel = viewModel(context as ComponentActivity)
    val settings by app.settingsStore.settings.collectAsState(initial = null)
    val locale = LocalConfiguration.current.locales.get(0)

    val recordings by app.repository.recordings.collectAsState(initial = emptyList())
    val streak = remember(recordings) {
        StreakCalculator.computeStreak(recordings.map { it.createdAt }, System.currentTimeMillis())
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }

    val prompts = stringArrayResource(R.array.prompts)

    // Разгорающийся огонёк при старте записи.
    val flameScale by animateFloatAsState(
        targetValue = if (vm.isRecording) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "flame",
    )

    val headlineRes = when {
        vm.saving -> R.string.record_saving
        vm.isRecording -> R.string.record_title_recording
        rootReplyId != null -> R.string.reply_hint
        else -> R.string.record_title_idle
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(headlineRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (streak > 0) {
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "🔥 $streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.graphicsLayer {
                            scaleX = flameScale
                            scaleY = flameScale
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    vm.saving -> ""
                    vm.isRecording -> AnniversaryDates.formatElapsed(vm.elapsedMs)
                    else -> stringResource(R.string.record_subtitle_idle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedContent(
                targetState = vm.error,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "error",
            ) { err ->
                Column {
                    if (err != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(err, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Промпт дня при молчании.
        AnimatedVisibility(
            visible = vm.showPrompt && vm.isRecording,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = prompts.getOrElse(vm.promptIndex) { "" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        Waveform(
            active = vm.isRecording,
            amplitudeProvider = { vm.maxAmplitude() },
            modifier = Modifier.fillMaxWidth().height(96.dp),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            vm.sealedUntil?.let { until ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.clickable { showDatePicker = true },
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.seal_chip, AnniversaryDates.formatShortDate(until, locale)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (rootReplyId == null && !vm.isRecording) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(if (vm.sealedUntil == null) R.string.seal_button else R.string.seal_chip))
                }
            }
            RecordButton(
                recording = vm.isRecording,
                enabled = !vm.saving,
                onClick = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        vm.toggleRecording(
                            quality = settings?.audioQuality ?: AudioQuality.STANDARD,
                            locale = locale,
                            autoStopSilence = settings?.autoStopSilence == true,
                        )
                    }
                },
            )
        }
    }

    // Выбор настроения после остановки.
    if (vm.showMoodPicker) {
        AlertDialog(
            onDismissRequest = {
                vm.dismissMoodPicker()
                scope.launch { vm.save(-1, rootReplyId, parentReplyId, onSaved) }
            },
            title = { Text(stringResource(R.string.mood_pick_title), fontFamily = FontFamily.Serif) },
            text = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Mood.emojis.forEachIndexed { index, emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .clickable {
                                    vm.dismissMoodPicker()
                                    scope.launch { vm.save(index, rootReplyId, parentReplyId, onSaved) }
                                }
                                .padding(4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissMoodPicker()
                    scope.launch { vm.save(-1, rootReplyId, parentReplyId, onSaved) }
                }) { Text(stringResource(R.string.mood_skip)) }
            },
        )
    }

    // Выбор даты запечатывания.
    if (showDatePicker) {
        val todayMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = vm.sealedUntil ?: todayMillis + 365L * 24 * 60 * 60 * 1000,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis > todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setSeal(dateState.selectedDateMillis)
                    showDatePicker = false
                }) { Text(stringResource(R.string.onboarding_next)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.setSeal(null)
                    showDatePicker = false
                }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}

/** Пульсирующая кнопка-микрофон с «эхо»-кольцами во время записи. */
@Composable
private fun RecordButton(
    recording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )

    val stateTransition = updateTransition(recording, label = "record")
    val buttonScale by stateTransition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy) },
        label = "scale",
    ) { if (it) 0.92f else 1f }

    Box(contentAlignment = Alignment.Center) {
        if (recording) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val radius = size.minDimension / 2 * pulse
                drawCircle(
                    color = Color(0xFFFFB74D).copy(alpha = 0.15f * (2f - pulse).coerceIn(0f, 1f)),
                    radius = radius,
                )
            }
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(104.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = stringResource(
                    if (recording) R.string.record_cd_stop else R.string.record_cd_record,
                ),
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/** Простой waveform на Canvas: столбики следят за амплитудой микрофона. */
@Composable
private fun Waveform(
    active: Boolean,
    amplitudeProvider: () -> Int,
    modifier: Modifier = Modifier,
) {
    val bars = remember { Array(BAR_COUNT) { 0.08f } }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(active) {
        while (active) {
            val amplitude = amplitudeProvider() / 32767f
            for (i in 0 until BAR_COUNT - 1) bars[i] = bars[i + 1]
            bars[BAR_COUNT - 1] = (bars[BAR_COUNT - 1] * 0.4f + amplitude * 0.6f).coerceIn(0.05f, 1f)
            tick++
            delay(60)
        }
        if (!active) {
            for (i in bars.indices) bars[i] = 0.08f
            tick++
        }
    }

    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier) {
        val currentTick = tick
        val barWidth = size.width / (BAR_COUNT * 2)
        val centerY = size.height / 2
        for (i in 0 until BAR_COUNT) {
            val x = (i * 2 + 1) * barWidth
            val halfHeight = (bars[i] * size.height / 2).coerceAtLeast(4f)
            drawLine(
                color = trackColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = barColor,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round,
            )
        }
    }
}
