package ru.echoapp.ui.pastweek

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.data.RecordingEntity
import ru.echoapp.util.AnniversaryDates
import ru.echoapp.util.Mood

/** «Неделя из прошлого»: подборка записей за текущую неделю год назад. */
@Composable
fun PastWeekScreen(app: EchoApp, onOpenRecording: (Long) -> Unit) {
    val locale = LocalConfiguration.current.locales.get(0)
    var entries by remember { mutableStateOf<List<RecordingEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val (start, end) = AnniversaryDates.sameWeekLastYearBounds(System.currentTimeMillis())
        entries = withContext(Dispatchers.IO) {
            app.repository.recordingsBetween(start, end)
                .filter { it.kind != "letter" }
        }
        loaded = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.past_week_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        if (loaded && entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.past_week_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(entries, key = { it.id }) { recording ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenRecording(recording.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play),
                            tint = MaterialTheme.colorScheme.primary,
                        )
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
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = recording.transcript?.takeIf { it.isNotBlank() }
                                    ?: AnniversaryDates.formatElapsed(recording.durationMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
