package ru.echoapp.ui.feed

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.audio.AudioPlayer
import ru.echoapp.audio.WaveformExtractor
import ru.echoapp.data.RecordingEntity
import ru.echoapp.ui.common.WaveformBar
import ru.echoapp.util.AnniversaryDates
import ru.echoapp.util.Mood

@Composable
fun FeedScreen(app: EchoApp, onOpenRecording: (Long) -> Unit) {
    val settings by app.settingsStore.settings.collectAsState(initial = null)
    var query by remember { mutableStateOf("") }
    val recordings by app.repository.search(query).collectAsState(initial = emptyList())
    val player = remember { AudioPlayer() }
    val playback by player.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales.get(0)
    val now = System.currentTimeMillis()
    var levelsMap by remember { mutableStateOf(mapOf<Long, List<Int>>()) }

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        AnimatedVisibility(
            visible = recordings.isEmpty(),
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
            exit = fadeOut(),
        ) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.feed_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = recordings.isNotEmpty(),
            enter = fadeIn(tween(500)),
            exit = fadeOut(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(recordings, key = { it.id }) { recording ->
                    val showWave = recording.kind != "letter" &&
                        (recording.sealedUntil == null || recording.sealedUntil <= now)
                    val levels = levelsMap[recording.id]
                    LaunchedEffect(recording.id, recording.waveform) {
                        if (showWave && levels == null) {
                            val stored = recording.waveform
                            if (stored != null) {
                                levelsMap = levelsMap + (recording.id to WaveformExtractor.parse(stored))
                            } else {
                                val extracted = WaveformExtractor.extract(recording.filePath)
                                if (extracted != null) {
                                    app.repository.updateWaveform(recording.id, extracted)
                                    levelsMap = levelsMap + (recording.id to WaveformExtractor.parse(extracted))
                                }
                            }
                        }
                    }
                    RecordingCard(
                        recording = recording,
                        levels = levels.orEmpty(),
                        isPlaying = playback.recordingId == recording.id && playback.isPlaying,
                        locale = locale,
                        hidePreview = settings?.hidePreviews == true,
                        isSealed = recording.sealedUntil != null && recording.sealedUntil > now,
                        onPlayToggle = {
                            if (playback.recordingId == recording.id && playback.isPlaying) {
                                player.stop()
                            } else {
                                player.play(recording)
                            }
                        },
                        onOpen = { onOpenRecording(recording.id) },
                        onDelete = {
                            scope.launch {
                                val deleted = app.repository.deleteRecording(recording)
                                if (!deleted) {
                                    Toast.makeText(
                                        context,
                                        R.string.delete_blocked_sealed,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    recording: RecordingEntity,
    levels: List<Int>,
    isPlaying: Boolean,
    locale: java.util.Locale,
    hidePreview: Boolean,
    isSealed: Boolean,
    onPlayToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLetter = recording.kind == "letter"
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlayToggle, enabled = !isSealed && !isLetter) {
                Crossfade(targetState = isPlaying, label = "play") { playing ->
                    Icon(
                        imageVector = when {
                            isSealed -> Icons.Filled.Lock
                            isLetter -> Icons.Filled.MailOutline
                            playing -> Icons.Filled.Pause
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = stringResource(
                            when {
                                isSealed -> R.string.seal_button
                                playing -> R.string.cd_pause
                                else -> R.string.cd_play
                            },
                        ),
                        tint = if (isSealed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AnniversaryDates.formatFullDate(recording.createdAt, locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                    )
                    if (Mood.isValid(recording.mood)) {
                        Spacer(Modifier.width(6.dp))
                        Text(Mood.emoji(recording.mood), style = MaterialTheme.typography.titleMedium)
                    }
                    if (isSealed) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.width(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        isSealed -> stringResource(
                            R.string.sealed_badge,
                            AnniversaryDates.formatShortDate(recording.sealedUntil!!, locale),
                        )
                        isLetter -> stringResource(R.string.feed_letter_badge)
                        else -> recording.transcript?.takeIf { it.isNotBlank() && !hidePreview }
                            ?: AnniversaryDates.formatElapsed(recording.durationMs)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (levels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    WaveformBar(
                        levels = levels,
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        progress = if (isPlaying) 1f else null,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
