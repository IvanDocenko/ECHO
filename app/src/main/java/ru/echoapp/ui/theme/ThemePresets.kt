package ru.echoapp.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemeKey { CASSETTE, PAPER, MIDNIGHT, FOREST, VINTAGE }

/** Полная палитра пресета: цвета интерфейса + палитра точек настроения для карты года. */
data class ThemePalette(
    val key: ThemeKey,
    val isDark: Boolean,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    /** 5 цветов настроения (по индексу mood 0..4). */
    val moodColors: List<Color>,
    /** Сериф во всём тексте (пресет «Винтаж»). */
    val serifEverywhere: Boolean = false,
) {
    companion object {
        val presets: List<ThemePalette> = listOf(
            ThemePalette(
                key = ThemeKey.CASSETTE,
                isDark = true,
                primary = Color(0xFFFFB74D),
                background = Color(0xFF121212),
                surface = Color(0xFF1C1C1C),
                onBackground = Color(0xFFE8E4DE),
                onSurface = Color(0xFFE8E4DE),
                onSurfaceVariant = Color(0xFF9E9890),
                outline = Color(0xFF2C2C2C),
                moodColors = listOf(
                    Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFFFF176),
                    Color(0xFF81C784), Color(0xFF4FC3F7),
                ),
            ),
            ThemePalette(
                key = ThemeKey.PAPER,
                isDark = false,
                primary = Color(0xFFC4633E),
                background = Color(0xFFF7F3E9),
                surface = Color(0xFFFFFCF5),
                onBackground = Color(0xFF2B2620),
                onSurface = Color(0xFF2B2620),
                onSurfaceVariant = Color(0xFF7A7064),
                outline = Color(0xFFE0D6C2),
                moodColors = listOf(
                    Color(0xFFD32F2F), Color(0xFFE65100), Color(0xFFF9A825),
                    Color(0xFF2E7D32), Color(0xFF0277BD),
                ),
            ),
            ThemePalette(
                key = ThemeKey.MIDNIGHT,
                isDark = true,
                primary = Color(0xFF90E0EF),
                background = Color(0xFF0D1B2A),
                surface = Color(0xFF14263C),
                onBackground = Color(0xFFDCEAF5),
                onSurface = Color(0xFFDCEAF5),
                onSurfaceVariant = Color(0xFF8CA3B8),
                outline = Color(0xFF243B55),
                moodColors = listOf(
                    Color(0xFFEF9A9A), Color(0xFFFFCC80), Color(0xFFFFF59D),
                    Color(0xFFA5D6A7), Color(0xFF90E0EF),
                ),
            ),
            ThemePalette(
                key = ThemeKey.FOREST,
                isDark = true,
                primary = Color(0xFFA3B18A),
                background = Color(0xFF1B2F1B),
                surface = Color(0xFF223A22),
                onBackground = Color(0xFFE4EBDD),
                onSurface = Color(0xFFE4EBDD),
                onSurfaceVariant = Color(0xFF93A488),
                outline = Color(0xFF2E4429),
                moodColors = listOf(
                    Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFDCE775),
                    Color(0xFFA3B18A), Color(0xFF80CBC4),
                ),
            ),
            ThemePalette(
                key = ThemeKey.VINTAGE,
                isDark = false,
                primary = Color(0xFF8C6D4F),
                background = Color(0xFFF0E6D2),
                surface = Color(0xFFF7EFDF),
                onBackground = Color(0xFF4A3B2A),
                onSurface = Color(0xFF4A3B2A),
                onSurfaceVariant = Color(0xFF8A7A64),
                outline = Color(0xFFD8C9AC),
                moodColors = listOf(
                    Color(0xFFA5503C), Color(0xFFB98A2F), Color(0xFF8A9862),
                    Color(0xFF5F7D54), Color(0xFF4F6D7A),
                ),
                serifEverywhere = true,
            ),
        )

        fun byKey(key: String): ThemePalette =
            presets.firstOrNull { it.key.name == key } ?: presets.first()
    }
}
