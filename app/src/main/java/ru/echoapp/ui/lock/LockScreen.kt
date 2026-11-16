package ru.echoapp.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.echoapp.R

/** Экран разблокировки при старте приложения. */
@Composable
fun LockScreen(pinCode: String, biometricEnabled: Boolean, onUnlock: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit() {
        if (pin == pinCode) {
            onUnlock()
        } else {
            error = true
            pin = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.pin_enter),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
        )
        Spacer(Modifier.height(24.dp))

        // Индикатор из 4 точек.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .size(14.dp)
                        .background(
                            color = if (index < pin.length) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }
        if (error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.pin_wrong),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(32.dp))

        PinPad(
            onDigit = { d ->
                error = false
                if (pin.length < 4) pin += d
                if (pin.length == 4) submit()
            },
            onBackspace = { pin = pin.dropLast(1) },
        )

        if (biometricEnabled) {
            Spacer(Modifier.height(24.dp))
            val canAuthenticate = remember {
                BiometricManager.from(context)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                    BiometricManager.BIOMETRIC_SUCCESS
            }
            if (canAuthenticate) {
                IconButton(onClick = {
                    val activity = context as? FragmentActivity ?: return@IconButton
                    val executor = ContextCompat.getMainExecutor(context)
                    val prompt = BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                onUnlock()
                            }
                        },
                    )
                    val info = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(context.getString(R.string.app_name))
                        .setSubtitle(context.getString(R.string.pin_enter))
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        .build()
                    runCatching { prompt.authenticate(info) }
                }) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = stringResource(R.string.biometric_unlock),
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Цифровая клавиатура PIN-пада. */
@Composable
fun PinPad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable(enabled = key.isNotEmpty()) {
                                if (key == "⌫") onBackspace() else onDigit(key)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (key == "⌫") {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
