package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun FaceScanner(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    
    // Animate scan line position from 0f to 1f
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    // Animate glowing grid pulse
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gridPulse"
    )

    Box(modifier = modifier) {
        // Underlay the image picker or camera preview
        content()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw High-Tech Grid Overlay
            val gridRows = 8
            val gridCols = 8
            val rowStep = h / gridRows
            val colStep = w / gridCols
            val gridAlpha = if (isScanning) pulseAlpha * 0.35f else 0.12f

            for (i in 1 until gridRows) {
                val y = i * rowStep
                drawLine(
                    color = primaryColor.copy(alpha = gridAlpha),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }
            for (j in 1 until gridCols) {
                val x = j * colStep
                drawLine(
                    color = primaryColor.copy(alpha = gridAlpha),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Corner Accents (Hologram Look)
            val cornerLength = size.minDimension * 0.15f
            val strokeW = 6f
            val padding = 15f
            val cornerColor = if (isScanning) primaryColor else primaryColor.copy(alpha = 0.5f)

            // Top Left
            drawLine(
                color = cornerColor,
                start = Offset(padding, padding),
                end = Offset(padding + cornerLength, padding),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(padding, padding),
                end = Offset(padding, padding + cornerLength),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Top Right
            drawLine(
                color = cornerColor,
                start = Offset(w - padding, padding),
                end = Offset(w - padding - cornerLength, padding),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(w - padding, padding),
                end = Offset(w - padding, padding + cornerLength),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Bottom Left
            drawLine(
                color = cornerColor,
                start = Offset(padding, h - padding),
                end = Offset(padding + cornerLength, h - padding),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(padding, h - padding),
                end = Offset(padding, h - padding - cornerLength),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Bottom Right
            drawLine(
                color = cornerColor,
                start = Offset(w - padding, h - padding),
                end = Offset(w - padding - cornerLength, h - padding),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(w - padding, h - padding),
                end = Offset(w - padding, h - padding - cornerLength),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // 4. Draw Animated Face Proportions / Crosshairs
            val centerX = w / 2
            val centerY = h / 2
            val radius = size.minDimension * 0.35f
            val reticleColor = if (isScanning) secondaryColor else secondaryColor.copy(alpha = 0.3f)

            // Biometric eye scanning indicators
            drawCircle(
                color = reticleColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f)))
            )

            // Outer fine ring
            drawCircle(
                color = reticleColor.copy(alpha = 0.2f),
                radius = radius + 20f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            // Central targeting dot
            drawCircle(
                color = if (isScanning) primaryColor else primaryColor.copy(alpha = 0.4f),
                radius = 6f,
                center = Offset(centerX, centerY)
            )

            // Draw scanning specific lines
            if (isScanning) {
                // Moving Scan Line
                val scanY = h * scanProgress
                val scanLineHeight = 30f

                // Glowing scan band gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, scanY - scanLineHeight / 2),
                    size = Size(w, scanLineHeight)
                )

                // Solid laser core line
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, scanY),
                    end = Offset(w, scanY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
