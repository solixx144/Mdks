package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Light Scheme Colors (Standard Professional Material 3)
val LightPrimary = Color(0xFF4F46E5)          // Indigo 600
val LightOnPrimary = Color.White
val LightSecondary = Color(0xFF2563EB)        // Blue 600
val LightOnSecondary = Color.White
val LightTertiary = Color(0xFFD97706)         // Amber 600
val LightBackground = Color(0xFFF8FAFC)       // Slate 50
val LightOnBackground = Color(0xFF0F172A)     // Slate 900
val LightSurface = Color.White
val LightOnSurface = Color(0xFF0F172A)       // Slate 900
val LightSurfaceVariant = Color(0xFFF1F5F9)   // Slate 100
val LightOnSurfaceVariant = Color(0xFF475569) // Slate 600

// Dark Scheme Colors (Professional Slate & Indigo Theme)
val DarkBackground = Color(0xFF0F172A)            // Deep slate background
val DarkSurface = Color(0xFF1E293B)               // Muted slate surface
val DarkSurfaceVariant = Color(0xFF334155)        // Dark slate card background
val DarkPrimary = Color(0xFF818CF8)               // Soft indigo primary accent
val DarkSecondary = Color(0xFF60A5FA)             // Soft blue secondary accent
val DarkTertiary = Color(0xFFFBBF24)              // Soft amber tertiary accent
val DarkSuccess = Color(0xFF34D399)               // Soft emerald green
val DarkOnBackground = Color(0xFFF8FAFC)          // Off-white primary text
val DarkOnSurfaceVariant = Color(0xFF94A3B8)      // Muted slate secondary text
val DarkOutlineVariant = Color(0x1F818CF8)        // Subtle indigo border color

// Adaptive Mappings to eliminate hardcoded Cyber/Neon colors and support Light/Dark Mode
val CyberBlack: Color @Composable get() = MaterialTheme.colorScheme.background
val CyberDarkSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val CyberCardBg: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val NeonCyan: Color @Composable get() = MaterialTheme.colorScheme.primary
val NeonBlue: Color @Composable get() = MaterialTheme.colorScheme.secondary
val CyberOrange: Color @Composable get() = MaterialTheme.colorScheme.tertiary
val CyberGreen: Color get() = Color(0xFF10B981) // Emerald Green (standard non-cyber success color)
val CyberTextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val CyberTextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val CyberGridColor: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
