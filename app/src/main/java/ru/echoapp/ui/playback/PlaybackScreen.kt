package ru.echoapp.ui.playback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.audio.AudioPlayer
import ru.echoapp.audio.WaveformExtractor
import ru.echoapp.data.RecordingEntity
import ru.echoapp.data.ReplyEntity
import ru.echoapp.ui.common.SeekBar
import ru.echoapp.ui.common.WaveformBar
import ru.echoapp.util.AnniversaryDates
import ru.echoapp.util.Mood

/** Экран воспроизведения любой записи: оригинал + цепочка ответов «диалога с прошлым». */
@Composable
fun PlaybackScreen(
    app: EchoApp,
    recordingId: Long,
    onReply: (Long, Long?) -> Unit,
) {
    var recording by remember { mutableStateOf<RecordingEntity?>(null) }
    var replies by remember { mutableStateOf<List<ReplyEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var levels by remember { mutableStateOf<List<Int>>(emptyList()) }

    val player = remember { AudioPlayer() }
    val playback by player.state.collectAsState()
    val locale = LocalConfiguration.current.locales.get(0)

    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    LaunchedEffect(recordingId) {
        recording = app.repository.findById(recordingId)
        replies = app.repository.repliesFor(recordingId)
        loaded = true
    }

    // Настоящий waveform: берём сохранённые амплитуды или декодируем файл один раз.
    LaunchedEffect(recording?.id, recording?.waveform) {
        val entry = recording ?: return@LaunchedEffect
        if (entry.kind == "letter" || entry.filePath.isBlank()) return@LaunchedEffect
        val stored = entry.waveform
        levels = if (stored != null) {
            WaveformExtractor.parse(stored)
        } else {
            val extracted = WaveformExtractor.extract(entry.filePath)
            if (extracted != null) {
                app.repository.updateWaveform(entry.id, extracted)
                WaveformExtractor.parse(extracted)
            } else {
                emptyList()
            }
        }
    }

    val entry = recording
    if (!loaded || entry == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.letter_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val isSealed = entry.sealedUntil != null && entry.sealedUntil > System.currentTimeMillis()
    val isLetter = entry.kind == "letter"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.MailOutline,
            contentDescription = null,
            modifier = Modifier.width(48.dp).height(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = AnniversaryDates.formatFullDate(entry.createdAt, locale),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
        )
        if (Mood.isValid(entry.mood)) {
            Spacer(Modifier.height(4.dp))
            Text(Mood.emoji(entry.mood), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))

        if (isSealed) {
            Text(
                text = stringResource(
                    R.string.record_locked,
                    AnniversaryDates.formatFullDate(entry.sealedUntil!!, locale),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (!isLetter) {
            PlayRow(
                itemId = entry.id,
                durationLabel = AnniversaryDates.formatElapsed(entry.durationMs),
                isPlaying = playback.recordingId == entry.id && playback.isPlaying,
                onToggle = {
                    if (playback.recordingId == entry.id && playback.isPlaying) {
                        player.stop()
                    } else {
                        player.play(entry)
                    }
                },
            )
            if (playback.recordingId == entry.id) {
                Spacer(Modifier.height(4.dp))
                SeekBar(
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs,
                    onSeek = { player.seekTo(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (levels.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val progress = if (playback.recordingId == entry.id && playback.durationMs > 0) {
                    playback.positionMs.toFloat() / playback.durationMs
                } else {
                    null
                }
                WaveformBar(
                    levels = levels,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    progress = progress,
                )
            }
        }

        if (!entry.transcript.isNullOrBlank() && (!isSealed || isLetter)) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Text(
                    text = entry.transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // Цепочка ответов.
        if (replies.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.replies_title),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val depths = remember(replies) { computeDepths(replies) }
            replies.forEachIndexed { index, reply ->
                val depth = depths[index]
                Row(Modifier.fillMaxWidth().padding(start = (depth * 20).dp)) {
                    Icon(
                        Icons.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.width(14.dp).height(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = AnniversaryDates.formatShortDate(reply.createdAt, locale) +
                                if (Mood.isValid(reply.mood)) "  ${Mood.emoji(reply.mood)}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = reply.transcript?.takeIf { it.isNotBlank() }
                                ?: AnniversaryDates.formatElapsed(reply.durationMs),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                        )
                        PlayRow(
                            itemId = reply.id,
                            durationLabel = AnniversaryDates.formatElapsed(reply.durationMs),
                            isPlaying = playback.recordingId == reply.id && playback.isPlaying,
                            compact = true,
                            onToggle = {
                                if (playback.recordingId == reply.id && playback.isPlaying) {
                                    player.stop()
                                } else {
                                    player.playFile(reply.id, reply.filePath)
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        if (!isLetter) {
            Button(
                onClick = {
                    val lastReplyId = replies.lastOrNull()?.id
                    onReply(entry.id, lastReplyId)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.Reply, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.reply_button))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlayRow(
    itemId: Long,
    durationLabel: String,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    compact: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onToggle, shape = CircleShape) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.cd_pause else R.string.cd_play,
                ),
            )
            if (!compact) {
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = durationLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Глубина каждого ответа в цепочке (по parentReplyId). */
private fun computeDepths(replies: List<ReplyEntity>): List<Int> {
    val byId = replies.associateBy { it.id }
    return replies.map { reply ->
        var depth = 0
        var parent = reply.parentReplyId
        while (parent != null && depth < replies.size) {
            depth++
            parent = byId[parent]?.parentReplyId
        }
        depth
    }
}
