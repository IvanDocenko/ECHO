package ru.echoapp.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.echoapp.util.AnniversaryDates

/** Полоса перемотки: слайдер + метки текущей позиции и длительности. */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0) return

    // Пока тянем слайдер — не даём тику плеера дёргать ползунок.
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    Column(modifier) {
        Slider(
            value = dragFraction ?: (positionMs.toFloat() / durationMs).coerceIn(0f, 1f),
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * durationMs).toLong()) }
                dragFraction = null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = AnniversaryDates.formatElapsed(
                    ((dragFraction ?: (positionMs.toFloat() / durationMs)) * durationMs).toLong(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = AnniversaryDates.formatElapsed(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
