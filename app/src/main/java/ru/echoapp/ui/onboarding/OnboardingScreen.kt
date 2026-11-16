package ru.echoapp.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.echoapp.R

private const val SLIDE_COUNT = 3
private const val LOGO_HOLD_MS = 1_900L

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    // Фаза 1 — анимированный логотип, фаза 2 — слайды.
    var showLogo by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(LOGO_HOLD_MS)
        showLogo = false
    }

    Crossfade(targetState = showLogo, animationSpec = tween(600), label = "intro") { logo ->
        if (logo) {
            LogoIntro(onSkip = { showLogo = false })
        } else {
            Slides(onFinished)
        }
    }
}

/** Заставка: слово ECHO с расходящимися кольцами эха. */
@Composable
private fun LogoIntro(onSkip: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "logoRings")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "progress",
    )
    var wordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150)
        wordVisible = true
    }
    val wordScale by animateFloatAsState(
        targetValue = if (wordVisible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "wordScale",
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (wordVisible) 1f else 0f,
        animationSpec = tween(700),
        label = "wordAlpha",
    )

    Box(
        modifier = Modifier.fillMaxSize().clickable(onClick = onSkip),
        contentAlignment = Alignment.Center,
    ) {
        // Три кольца эха с фазовым сдвигом.
        Canvas(modifier = Modifier.size(300.dp)) {
            val maxRadius = size.minDimension / 2.2f
            for (i in 0..2) {
                val p = (progress + i / 3f) % 1f
                drawCircle(
                    color = Color(0xFFFFB74D).copy(alpha = (1f - p) * 0.45f),
                    radius = maxRadius * (0.25f + 0.75f * p),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    scaleX = wordScale
                    scaleY = wordScale
                    alpha = wordAlpha
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_1_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.alpha(wordAlpha),
            )
        }
    }
}

/** Слайды интро с поэтапными анимациями появления. */
@Composable
private fun Slides(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { SLIDE_COUNT })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (slideInVertically(tween(400)) { it / 4 } + fadeIn(tween(400)))
                        .togetherWith(slideOutVertically(tween(300)) { -it / 4 } + fadeOut(tween(300)))
                },
                label = "slide",
            ) { currentPage ->
                SlideContent(
                    icon = slideIcon(currentPage),
                    title = slideTitle(currentPage),
                    text = slideText(currentPage),
                )
            }
        }

        // Индикатор страниц с анимированным размером.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(SLIDE_COUNT) { index ->
                val active = pagerState.currentPage == index
                val dotSize by animateDpAsState(
                    targetValue = if (active) 10.dp else 7.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dot",
                )
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .alpha(if (active) 1f else 0.6f)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        val isLast = pagerState.currentPage == SLIDE_COUNT - 1
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = !isLast,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TextButton(onClick = onFinished) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
            Button(onClick = {
                if (isLast) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }) {
                Text(stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun SlideContent(icon: ImageVector, title: String, text: String) {
    // Поэтапное появление: иконка → заголовок → текст.
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        step = 1
        delay(220)
        step = 2
        delay(220)
        step = 3
    }

    val iconScale by animateFloatAsState(
        targetValue = if (step >= 1) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "icon",
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (step >= 2) 1f else 0f,
        animationSpec = tween(500),
        label = "titleA",
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (step >= 2) 0f else 30f,
        animationSpec = tween(500),
        label = "titleY",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (step >= 3) 1f else 0f,
        animationSpec = tween(600),
        label = "textA",
    )
    val textOffset by animateFloatAsState(
        targetValue = if (step >= 3) 0f else 24f,
        animationSpec = tween(600),
        label = "textY",
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            PulseRings(visible = step >= 1)
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(titleAlpha)
                .graphicsLayer { translationY = titleOffset },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(textAlpha)
                .graphicsLayer { translationY = textOffset },
        )
    }
}

/** Три расходящихся кольца за иконкой слайда. */
@Composable
private fun PulseRings(visible: Boolean) {
    if (!visible) return
    val transition = rememberInfiniteTransition(label = "rings")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "progress",
    )
    Canvas(modifier = Modifier.size(180.dp)) {
        val maxRadius = size.minDimension / 2f
        for (i in 0..2) {
            val p = (progress + i / 3f) % 1f
            drawCircle(
                color = Color(0xFFFFB74D).copy(alpha = (1f - p) * 0.4f),
                radius = maxRadius * (0.3f + 0.7f * p),
            )
        }
    }
}

// --- Слайды: иконки и строки по индексу ---

@Composable
private fun slideIcon(index: Int): ImageVector = when (index) {
    0 -> Icons.Filled.Mic
    1 -> Icons.AutoMirrored.Filled.VolumeUp
    else -> Icons.Filled.MailOutline
}

@Composable
private fun slideTitle(index: Int): String = stringResource(
    when (index) {
        0 -> R.string.onboarding_1_title
        1 -> R.string.onboarding_2_title
        else -> R.string.onboarding_3_title
    },
)

@Composable
private fun slideText(index: Int): String = stringResource(
    when (index) {
        0 -> R.string.onboarding_1_text
        1 -> R.string.onboarding_2_text
        else -> R.string.onboarding_3_text
    },
)
