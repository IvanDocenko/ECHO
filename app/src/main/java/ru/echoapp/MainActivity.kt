package ru.echoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ru.echoapp.notifications.NotificationHelper
import ru.echoapp.ui.AppRoot
import ru.echoapp.ui.lock.LockScreen
import ru.echoapp.ui.theme.EchoTheme
import ru.echoapp.ui.theme.ThemePalette

class MainActivity : AppCompatActivity() {

    private val pendingLetterId = mutableStateOf<Long?>(null)
    private val pendingRecordingId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            val app = application as EchoApp
            val settings by app.settingsStore.settings.collectAsState(initial = null)

            NotificationPermissionGate {
                val current = settings
                if (current == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val palette = ThemePalette.byKey(current.themeKey)
                    EchoTheme(
                        palette = palette,
                        dynamicColors = current.dynamicColors,
                        fontScale = current.fontScale,
                    ) {
                        val needsLock = current.pinEnabled && current.pinCode.length == 4
                        var unlocked by remember { mutableStateOf(false) }
                        if (needsLock && !unlocked) {
                            LockScreen(
                                pinCode = current.pinCode,
                                biometricEnabled = current.biometricEnabled,
                                onUnlock = { unlocked = true },
                            )
                        } else {
                            AppRoot(
                                app = app,
                                onboardingDone = current.onboardingDone,
                                pendingLetterId = pendingLetterId,
                                pendingRecordingId = pendingRecordingId,
                                palette = palette,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent ?: return
        val letter = intent.getLongExtra(NotificationHelper.EXTRA_OPEN_LETTER, -1L)
        if (letter != -1L) pendingLetterId.value = letter
        val recording = intent.getLongExtra(NotificationHelper.EXTRA_OPEN_RECORDING, -1L)
        if (recording != -1L) pendingRecordingId.value = recording
    }
}

/** На Android 13+ запрашиваем разрешение на уведомления при первом показе. */
@Composable
private fun NotificationPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    content()
}
