package ru.echoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Цвета настроения с учётом темы (индекс 0..4); -1 → приглушённый серый. */
@Composable
fun moodColor(palette: ThemePalette, mood: Int): Color =
    palette.moodColors.getOrElse(mood) { palette.outline }

@Composable
fun EchoTheme(
    palette: ThemePalette,
    dynamicColors: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColors && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamic && palette.isDark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        palette.isDark -> darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.background,
            secondary = palette.primary,
            onSecondary = palette.background,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
        )
        else -> lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.primary,
            background = palette.background,
            onBackground = palette.onBackground,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
        )
    }

    val base = MaterialTheme.typography
    fun TextUnit.scaled() = (this.value * fontScale).sp
    val serif = FontFamily.Serif
    val typography = if (palette.serifEverywhere) {
        base.copy(
            headlineMedium = base.headlineMedium.copy(fontFamily = serif, fontSize = base.headlineMedium.fontSize.scaled()),
            headlineSmall = base.headlineSmall.copy(fontFamily = serif, fontSize = base.headlineSmall.fontSize.scaled()),
            titleMedium = base.titleMedium.copy(fontFamily = serif, fontSize = base.titleMedium.fontSize.scaled()),
            titleSmall = base.titleSmall.copy(fontFamily = serif, fontSize = base.titleSmall.fontSize.scaled()),
            bodyLarge = base.bodyLarge.copy(fontFamily = serif, fontSize = base.bodyLarge.fontSize.scaled()),
            bodyMedium = base.bodyMedium.copy(fontFamily = serif, fontSize = base.bodyMedium.fontSize.scaled()),
            bodySmall = base.bodySmall.copy(fontFamily = serif, fontSize = base.bodySmall.fontSize.scaled()),
        )
    } else {
        base.copy(
            bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize.scaled()),
            bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize.scaled()),
            bodySmall = base.bodySmall.copy(fontSize = base.bodySmall.fontSize.scaled()),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
