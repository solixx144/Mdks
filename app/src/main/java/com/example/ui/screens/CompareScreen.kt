package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.FaceComparisonResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceViewModel
import com.example.ui.viewmodel.CompareUiState

@Composable
fun CompareScreen(
    viewModel: FaceViewModel,
    modifier: Modifier = Modifier
) {
    val compareBitmap1 by viewModel.compareBitmap1.collectAsState()
    val compareBitmap2 by viewModel.compareBitmap2.collectAsState()
    val compareState by viewModel.compareState.collectAsState()
    val scrollState = rememberScrollState()

    // Activity launchers for Subject 1
    val gallery1Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setCompareBitmap1(viewModel.loadBitmapFromUri(it)) }
    }
    val camera1Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.setCompareBitmap1(it) }
    }

    // Activity launchers for Subject 2
    val gallery2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setCompareBitmap2(viewModel.loadBitmapFromUri(it)) }
    }
    val camera2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.setCompareBitmap2(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        Text(
            text = "BIOMETRIC COMPARATOR",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "COMPARE FACIAL SHAPE COORDINATES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Side-by-side or stacked image pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject 1 Picker Box
            SubjectPickerBox(
                title = "SUBJECT ALPHA",
                bitmap = compareBitmap1,
                onCameraClick = { camera1Launcher.launch() },
                onGalleryClick = { gallery1Launcher.launch("image/*") },
                isAnalyzing = compareState is CompareUiState.Analyzing,
                modifier = Modifier.weight(1f),
                testTagPrefix = "subject_alpha"
            )

            // Subject 2 Picker Box
            SubjectPickerBox(
                title = "SUBJECT BETA",
                bitmap = compareBitmap2,
                onCameraClick = { camera2Launcher.launch() },
                onGalleryClick = { gallery2Launcher.launch("image/*") },
                isAnalyzing = compareState is CompareUiState.Analyzing,
                modifier = Modifier.weight(1f),
                testTagPrefix = "subject_beta"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Compare Trigger Controls
        if (compareState !is CompareUiState.Analyzing) {
            if (compareBitmap1 != null && compareBitmap2 != null) {
                Button(
                    onClick = { viewModel.compareFaces() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = CyberBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("run_comparison_button")
                ) {
                    Icon(imageVector = Icons.Default.CompareArrows, contentDescription = "Compare")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INITIATE RECONCILIATION",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { viewModel.resetComparison() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberOrange),
                    modifier = Modifier.testTag("reset_comparison_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CLEAR ALL SUBJECTS", style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace))
                }
            } else {
                // Info block prompting uploads
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberGridColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield info",
                            tint = NeonBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Please load high-resolution facial images for both Subject Alpha and Subject Beta to initiate cranial matching analysis.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextSecondary,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }

        // Handle states
        when (val state = compareState) {
            is CompareUiState.Analyzing -> {
                Spacer(modifier = Modifier.height(24.dp))
                ComparisonAnalyzingReadout()
            }
            is CompareUiState.Success -> {
                Spacer(modifier = Modifier.height(24.dp))
                ComparisonSuccessView(result = state.result) {
                    viewModel.resetComparison()
                }
            }
            is CompareUiState.Error -> {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ERROR: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {}
        }
    }
}

@Composable
fun SubjectPickerBox(
    title: String,
    bitmap: Bitmap?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isAnalyzing: Boolean,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "subject"
) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .border(
                width = 1.dp,
                color = if (bitmap != null) NeonCyan else NeonBlue.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (bitmap != null) NeonCyan else CyberTextSecondary
                )
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBlack)
                    .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload placeholder",
                        tint = NeonBlue.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (!isAnalyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(CyberCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .testTag("${testTagPrefix}_camera")
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera picker", tint = NeonCyan)
                    }

                    IconButton(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(CyberCardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .testTag("${testTagPrefix}_gallery")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery picker", tint = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonAnalyzingReadout() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        LinearProgressIndicator(
            color = NeonCyan,
            trackColor = CyberGridColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "MAPPING COMPARATIVE CRANIAL NODES...",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ComparisonSuccessView(
    result: FaceComparisonResult,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Report title
        Text(
            text = "RECONCILIATION COMPLETE",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (result.similarityScore > 75) CyberGreen else CyberOrange
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Similarity Gauge / Score Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BIOMETRIC COHERENCE INDEX",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Large Match Status Label
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonBlue.copy(alpha = 0.15f), NeonCyan.copy(alpha = 0.15f))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = result.matchStatus.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (result.similarityScore > 75) CyberGreen else CyberOrange,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Similarity percentage progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MATCH:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { result.similarityScore / 100f },
                        color = if (result.similarityScore > 75) CyberGreen else CyberOrange,
                        trackColor = CyberGridColor,
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${result.similarityScore}%",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (result.similarityScore > 75) CyberGreen else CyberOrange
                        )
                    )
                }
            }
        }

        // Detailed Comparison Text Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(1.dp, CyberOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FORENSIC GEOMETRY READOUT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberOrange
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = result.analysisReport,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // Reset Button
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberCardBg,
                contentColor = NeonCyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "DISMISS AND RE-LOAD CHANNELS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
