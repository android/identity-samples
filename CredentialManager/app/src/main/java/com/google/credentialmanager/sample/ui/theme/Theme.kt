package com.google.credentialmanager.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SampleColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
)

@Composable
fun CredentialManagerSampleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SampleColorScheme,
        content = content
    )
}
