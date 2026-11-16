package ru.echoapp.ui.letter

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.audio.AudioPlayer
import ru.echoapp.data.RecordingEntity
import ru.echoapp.util.AnniversaryDates
import ru.echoapp.util.Mood

@Composable
fun LetterScreen(app: EchoApp, onReply: (Long, Long?) -> Unit) {
    var recording by remember { mutableStateOf<RecordingEntity?>(null) }
    var loaded by remember { mutableStateOf(false) }

    val player = remember { AudioPlayer() }
    val playback by player.state.collectAsState()
    val locale = LocalConfiguration.current.locales.get(0)

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    // Ищем запись ровно годовой давности: тот же день и месяц, год назад.
    LaunchedEffect(Unit) {
        val (start, end) = AnniversaryDates.sameDayLastYearBounds(System.currentTimeMillis())
        val candidates = withContext(Dispatchers.IO) {
            app.repository.recordingsBetween(start, end)
        }
        recording = candidates.firstOrNull()
        loaded = true
    }

    // Плавное «раскрытие конверта»: пружинный масштаб + fade, когда контент готов.
    val contentScale by animateFloatAsState(
        targetValue = if (loaded) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(600),
        label = "alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
                alpha = contentAlpha
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (!loaded) {
            Text(
                stringResource(R.string.letter_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val entry = recording
        if (entry == null) {
            EmptyLetter()
        } else {
            val isSealed = entry.sealedUntil != null && entry.sealedUntil > System.currentTimeMillis()
            Icon(
                imageVector = Icons.Filled.MailOutline,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.letter_exactly_year_ago),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = AnniversaryDates.formatFullDate(entry.createdAt, locale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.letter_you_recorded),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            if (isSealed) {
                Text(
                    text = stringResource(
                        R.string.record_locked,
                        AnniversaryDates.formatFullDate(entry.sealedUntil!!, locale),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            } else {
                val isPlaying = playback.recordingId == entry.id && playback.isPlaying
                Button(
                    onClick = {
                        if (isPlaying) player.stop() else player.play(entry)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.cd_pause else R.string.cd_play,
                        ),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(if (isPlaying) R.string.cd_pause else R.string.letter_listen))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (Mood.isValid(entry.mood)) {
                Text(Mood.emoji(entry.mood), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
            }
            if (!entry.transcript.isNullOrBlank() && !isSealed) {
                Text(
                    text = entry.transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = { onReply(entry.id, null) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(Icons.Filled.Reply, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.reply_button))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.letter_duration,
                    AnniversaryDates.formatElapsed(entry.durationMs),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyLetter() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.MailOutline,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.letter_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.letter_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
