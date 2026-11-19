package ru.echoapp.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap

/**
 * Статический waveform из реальных амплитуд (0..100).
 * [progress] (0..1) подсвечивает сыгранную часть; null — без подсветки.
 */
@Composable
fun WaveformBar(
    levels: List<Int>,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    barCount: Int = 48,
) {
    if (levels.isEmpty()) return

    val playedColor = MaterialTheme.colorScheme.primary
    val pendingColor = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2)
        if (barWidth <= 0f) return@Canvas
        val centerY = size.height / 2
        for (i in 0 until barCount) {
            val levelIndex = i * levels.size / barCount
            val level = levels[levelIndex] / 100f
            val x = (i * 2 + 1) * barWidth
            val halfHeight = (level * size.height / 2).coerceAtLeast(2f)
            val fraction = i.toFloat() / barCount
            val color = if (progress != null && fraction <= progress) playedColor else pendingColor
            drawLine(
                color = color,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round,
            )
        }
    }
}
