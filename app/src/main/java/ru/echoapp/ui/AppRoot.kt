package ru.echoapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.echoapp.EchoApp
import ru.echoapp.R
import ru.echoapp.ui.feed.FeedScreen
import ru.echoapp.ui.letter.LetterScreen
import ru.echoapp.ui.onboarding.OnboardingScreen
import ru.echoapp.ui.playback.PlaybackScreen
import ru.echoapp.ui.record.RecordScreen
import ru.echoapp.ui.settings.SettingsScreen
import ru.echoapp.ui.theme.ThemePalette
import ru.echoapp.ui.yearmap.YearMapScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val RECORD = "record"
    const val FEED = "feed"
    const val LETTER = "letter"
    const val SETTINGS = "settings"
    const val YEAR = "year"
    const val PLAYBACK = "playback/{recordingId}"
    const val RECORD_REPLY = "record?root={root}&parent={parent}"

    fun playback(id: Long) = "playback/$id"
}

private data class TopLevelRoute(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelRoutes = listOf(
    TopLevelRoute(Routes.RECORD, R.string.nav_record, Icons.Filled.FiberManualRecord),
    TopLevelRoute(Routes.FEED, R.string.nav_feed, Icons.Filled.History),
    TopLevelRoute(Routes.YEAR, R.string.nav_year, Icons.Filled.DateRange),
    TopLevelRoute(Routes.LETTER, R.string.nav_letter, Icons.Filled.MailOutline),
    TopLevelRoute(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun AppRoot(
    app: EchoApp,
    onboardingDone: Boolean,
    pendingLetterId: MutableState<Long?>,
    pendingRecordingId: MutableState<Long?>,
    palette: ThemePalette,
) {
    val navController = rememberNavController()
    val startDestination = if (onboardingDone) Routes.RECORD else Routes.ONBOARDING

    // Уведомления ведут на письмо или на экран записи.
    LaunchedEffect(pendingLetterId.value) {
        if (pendingLetterId.value != null) {
            navController.navigate(Routes.LETTER) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    LaunchedEffect(pendingRecordingId.value) {
        val id = pendingRecordingId.value
        if (id != null) {
            navController.navigate(Routes.playback(id)) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            AnimatedVisibility(
                visible = currentRoute != Routes.ONBOARDING &&
                    currentRoute != Routes.PLAYBACK &&
                    currentRoute != "record?root={root}&parent={parent}",
                enter = fadeIn(tween(300)) + slideIn { IntOffset(0, it.height) },
                exit = fadeOut(tween(250)) + slideOut { IntOffset(0, it.height) },
            ) {
                NavigationBar {
                    topLevelRoutes.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(tween(350)) + slideIn(tween(350)) { IntOffset(0, 48) }
            },
            exitTransition = { fadeOut(tween(250)) },
            popEnterTransition = { fadeIn(tween(350)) },
            popExitTransition = {
                fadeOut(tween(250)) + slideOut(tween(250)) { IntOffset(0, 48) }
            },
        ) {
            composable(Routes.ONBOARDING) {
                val scope = rememberCoroutineScope()
                OnboardingScreen(
                    onFinished = {
                        scope.launch { app.settingsStore.setOnboardingDone() }
                        navController.navigate(Routes.RECORD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.RECORD) {
                RecordScreen(app = app, rootReplyId = null, parentReplyId = null) {
                    navController.popBackStack()
                }
            }
            composable(
                route = Routes.RECORD_REPLY,
                arguments = listOf(
                    navArgument("root") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("parent") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                RecordScreen(
                    app = app,
                    rootReplyId = entry.arguments?.getLong("root")?.takeIf { it > 0 },
                    parentReplyId = entry.arguments?.getLong("parent")?.takeIf { it > 0 },
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.FEED) {
                FeedScreen(app = app, onOpenRecording = { id ->
                    navController.navigate(Routes.playback(id))
                })
            }
            composable(
                route = Routes.PLAYBACK,
                arguments = listOf(navArgument("recordingId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("recordingId") ?: -1L
                PlaybackScreen(
                    app = app,
                    recordingId = id,
                    onReply = { root, parent ->
                        navController.navigate("record?root=$root&parent=${parent ?: -1L}")
                    },
                )
            }
            composable(Routes.LETTER) {
                LetterScreen(
                    app = app,
                    onReply = { root, parent ->
                        navController.navigate("record?root=$root&parent=${parent ?: -1L}")
                    },
                )
            }
            composable(Routes.YEAR) {
                YearMapScreen(
                    app = app,
                    palette = palette,
                    onOpenRecording = { id -> navController.navigate(Routes.playback(id)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(app = app)
            }
        }
    }
}
