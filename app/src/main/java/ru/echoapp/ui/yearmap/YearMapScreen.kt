package ru.echoapp.ui.yearmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.data.RecordingEntity
import ru.echoapp.ui.theme.ThemePalette
import ru.echoapp.util.AnniversaryDates
import java.util.Calendar

/** Карта года: сетка дней, каждая запись — светящаяся точка цвета настроения. */
@Composable
fun YearMapScreen(app: EchoApp, palette: ThemePalette, onOpenRecording: (Long) -> Unit) {
    val recordings by app.repository.recordings.collectAsState(initial = emptyList())
    val locale = LocalConfiguration.current.locales.get(0)

    val (year, daysInYear, firstDayOffset) = remember { yearLayout() }
    val byDay = remember(recordings) {
        recordings
            .filter { isInYear(it.createdAt, year) }
            .groupBy { dayOfYear(it.createdAt) }
            .mapValues { it.value.first() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.year_title),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Serif,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (byDay.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.year_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(19),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(count = firstDayOffset) {
                    Spacer(Modifier.aspectRatio(1f))
                }
                items(count = daysInYear) { index ->
                    val day = index + 1
                    val recording = byDay[day]
                    YearCell(
                        recording = recording,
                        palette = palette,
                        isToday = day == dayOfYear(System.currentTimeMillis()),
                        onClick = { recording?.let { onOpenRecording(it.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun YearCell(
    recording: RecordingEntity?,
    palette: ThemePalette,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.outline
    val now = System.currentTimeMillis()
    val sealed = recording?.sealedUntil != null && recording.sealedUntil > now

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            recording == null -> Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(outline.copy(alpha = 0.35f)),
            )
            sealed -> Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(outline),
            )
            else -> Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(palette.moodColors.getOrElse(recording.mood) { palette.primary }),
            )
        }
        if (isToday) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
            )
        }
    }
}

private data class YearLayout(val year: Int, val days: Int, val offset: Int)

private fun yearLayout(): YearLayout {
    val now = Calendar.getInstance()
    val year = now.get(Calendar.YEAR)
    val days = now.getActualMaximum(Calendar.DAY_OF_YEAR)
    val first = Calendar.getInstance().apply {
        set(year, 0, 1, 12, 0, 0)
    }
    // Смещение начала сетки: пн = 0 … вс = 6.
    val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return YearLayout(year, days, offset)
}

private fun isInYear(millis: Long, year: Int): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return calendar.get(Calendar.YEAR) == year
}

private fun dayOfYear(millis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return calendar.get(Calendar.DAY_OF_YEAR)
}
