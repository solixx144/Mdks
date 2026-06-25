package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberBlack,
    secondary = NeonBlue,
    onSecondary = CyberBlack,
    tertiary = CyberOrange,
    background = CyberBlack,
    onBackground = CyberTextPrimary,
    surface = CyberDarkSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberCardBg,
    onSurfaceVariant = CyberTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the cyber holographic aesthetic
    dynamicColor: Boolean = false, // Disable dynamic theme to enforce the high-tech green/cyan/blue grid appearance
    content: @Composable () -> Unit,
) {
    // We enforce our premium cyber holographic dark theme for the immersive experience
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}
