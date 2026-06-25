package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.api.FaceAnalysisResult
import com.example.ui.components.HolographicScanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceViewModel
import com.example.ui.viewmodel.ScanUiState
import kotlinx.coroutines.delay

@Composable
fun ScanScreen(
    viewModel: FaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanBitmap by viewModel.scanBitmap.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val scrollState = rememberScrollState()

    // Activity result launchers for camera and gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = viewModel.loadBitmapFromUri(it)
            viewModel.setScanBitmap(bitmap)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.setScanBitmap(it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title Banner
        Text(
            text = "SHERLOCK FACIAL SEARCH",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "SCAN & COMPARE BIOMETRIC NODES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Holographic Viewfinder / Image Frame
        Card(
            modifier = Modifier
                .size(300.dp)
                .border(2.dp, NeonCyan, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            HolographicScanner(
                isScanning = scanState is ScanUiState.Scanning,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (scanBitmap != null) {
                        Image(
                            bitmap = scanBitmap!!.asImageBitmap(),
                            contentDescription = "Target Face",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Empty/Welcome view inside scanner frame
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_scanner_hero),
                                contentDescription = "Scan grid",
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "UPLOAD SOURCE PATTERN",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeonCyan
                                )
                            )
                            Text(
                                text = "Biometric camera core ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Image Selection Controls
        if (scanState !is ScanUiState.Scanning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { cameraLauncher.launch() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCardBg,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(50.dp)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("camera_capture_button")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CAMERA", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCardBg,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(50.dp)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("gallery_picker_button")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GALLERY", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (scanBitmap != null) {
                // Primary Action: Run Biometric Analysis
                Button(
                    onClick = { viewModel.scanFace() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = CyberBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_scan_button")
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Scan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INITIATE BIOMETRIC SEARCH",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { viewModel.resetScan() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberOrange),
                    modifier = Modifier.testTag("reset_scan_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESET SOURCE", style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace))
                }
            }
        }

        // Handle states
        when (val state = scanState) {
            is ScanUiState.Scanning -> {
                Spacer(modifier = Modifier.height(24.dp))
                ScanningReadout()
            }
            is ScanUiState.Success -> {
                Spacer(modifier = Modifier.height(24.dp))
                AnalysisSuccessView(result = state.result, sourceBitmap = scanBitmap) {
                    viewModel.resetScan()
                }
            }
            is ScanUiState.Error -> {
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
fun ScanningReadout() {
    var scanningText by remember { mutableStateOf("INITIALIZING BIOMETRIC SCAN...") }

    LaunchedEffect(Unit) {
        val strings = listOf(
            "ISOLATING CRANIAL GEOMETRY...",
            "CALCULATING FACIAL SYMMETRY INDEX...",
            "COMPILING CELEBRITY LOOKALIKE REGISTRY...",
            "RETRIEVING PUBLIC WEB REGISTRIES...",
            "GENERATING SHERLOCK HISTOGRAM REPORT..."
        )
        for (str in strings) {
            delay(1200)
            scanningText = str
        }
    }

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
            text = scanningText,
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
fun AnalysisSuccessView(
    result: FaceAnalysisResult,
    sourceBitmap: Bitmap?,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // High-Tech Header
        Text(
            text = "BIOMETRIC ANALYSIS COMPLETE",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyberGreen
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Matched Celebrity Lookalike Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CLOSEST CELEBRITY LOOKALIKE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Match Thumbnail
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBlack)
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Face placeholder",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = result.celebrityName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                        Text(
                            text = "SIMILARITY INDEX: ${result.celebritySimilarity}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Similarity percentage bar
                LinearProgressIndicator(
                    progress = { result.celebritySimilarity / 100f },
                    color = CyberOrange,
                    trackColor = CyberGridColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Metrics Grid (Age, Gender, Symmetry, Emotion)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BiometricChip(title = "EST. AGE", value = "${result.age} YRS", modifier = Modifier.weight(1f))
            BiometricChip(title = "GENDER", value = result.gender.uppercase(), modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BiometricChip(title = "EMOTION", value = result.emotion.uppercase(), modifier = Modifier.weight(1f))
            BiometricChip(title = "SYMMETRY", value = "${result.symmetry}%", modifier = Modifier.weight(1f))
        }

        // Investigative Report Text Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, CyberOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FORENSIC GEOMETRY REPORT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberOrange
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = result.analysis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // Web Profile Matches Card
        if (result.webMatches.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WEB REGISTRY PROFILE MATCHES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    result.webMatches.forEach { match ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { /* Open link or dummy search */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Link",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = match.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
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
                text = "CLEAR SCAN AND READY SYSTEM",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BiometricChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, CyberGridColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextSecondary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            )
        }
    }
}
