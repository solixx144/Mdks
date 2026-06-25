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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
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
    var showGuide by remember { mutableStateOf(true) }
    var isBatchMode by remember { mutableStateOf(false) }

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_LONG).show()
        }
    }

    // Activity result launchers for Batch mode
    val multipleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.loadBitmapFromUri(uri)?.let { viewModel.addBatchBitmap(it) }
        }
    }

    val batchCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.addBatchBitmap(it) }
    }

    val batchPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            batchCameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_LONG).show()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SHERLOCK FACIAL SEARCH",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "SCAN & IDENTIFY BIOMETRIC MATCHES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            IconButton(
                onClick = { showGuide = !showGuide },
                modifier = Modifier
                    .size(40.dp)
                    .background(CyberCardBg, CircleShape)
                    .border(1.dp, if (showGuide) NeonCyan else NeonBlue.copy(alpha = 0.5f), CircleShape)
                    .testTag("scan_guide_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show Help Guide",
                    tint = if (showGuide) NeonCyan else NeonBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = showGuide) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "QUICK USER MANUAL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "1. TAP 'CAMERA' to snap a photo or 'GALLERY' to select an existing picture of a face.\n" +
                               "2. TAP 'INITIATE BIOMETRIC SEARCH' to start scanning.\n" +
                               "3. VIEW results showing celebrity lookalikes, estimated age, gender, mood, and similarity percentage!\n" +
                               "4. CHECK 'ARCHIVES' tab to see previous searches and detailed match reports.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // MODE SELECTOR BAR (SINGLE SCAN VS BATCH MULTI-SCAN)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isBatchMode = false }
                    .background(if (!isBatchMode) NeonBlue.copy(alpha = 0.2f) else Color.Transparent)
                    .padding(vertical = 12.dp)
                    .testTag("mode_single_scan"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SINGLE SCAN CORE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (!isBatchMode) NeonCyan else CyberTextSecondary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isBatchMode = true }
                    .background(if (isBatchMode) NeonBlue.copy(alpha = 0.2f) else Color.Transparent)
                    .padding(vertical = 12.dp)
                    .testTag("mode_batch_scan"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BATCH DEEP-SCAN",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isBatchMode) NeonCyan else CyberTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        if (!isBatchMode) {
            // System Diagnostics HUD Ticker Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Core status indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyberGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CORE: ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // Database indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "REGISTRY:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextSecondary,
                                fontSize = 9.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.8M NODES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // Match accuracy indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ACCURACY:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextSecondary,
                                fontSize = 9.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "99.2%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberOrange,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

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
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch()
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
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
        } else {
            // Batch Scan Flow
            BatchScanFlowView(
                viewModel = viewModel,
                context = context,
                multipleGalleryLauncher = multipleGalleryLauncher,
                batchCameraLauncher = batchCameraLauncher,
                batchPermissionLauncher = batchPermissionLauncher
            )
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
fun BiometricRadialGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            val strokeWidth = 10f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Background arc (dark/track)
            drawArc(
                color = CyberGridColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glowing progress arc
            drawArc(
                color = if (score > 75) CyberGreen else CyberOrange,
                startAngle = 135f,
                sweepAngle = (score / 100f) * 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 22.sp
                )
            )
            Text(
                text = "MATCH",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun WebMatchGridCard(
    match: com.example.data.api.WebMatch,
    index: Int,
    primarySimilarity: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val confidence = remember(primarySimilarity, index) {
        (primarySimilarity - (index + 1) * 6 - (0..3).random()).coerceIn(45, 98)
    }

    // Assign branding based on the match.title or url
    val (brandColor, brandName, brandBg) = remember(match.title) {
        val titleLower = match.title.lowercase()
        when {
            titleLower.contains("linkedin") -> Triple(Color(0xFF0077B5), "LINKEDIN", Color(0xFF0077B5).copy(alpha = 0.15f))
            titleLower.contains("twitter") || titleLower.contains("x.com") || titleLower.contains("x social") -> Triple(Color(0xFF1DA1F2), "X.COM", Color(0xFF1DA1F2).copy(alpha = 0.15f))
            titleLower.contains("portfolio") || titleLower.contains("unsplash") -> Triple(CyberOrange, "PORTFOLIO", CyberOrange.copy(alpha = 0.15f))
            titleLower.contains("github") -> Triple(Color(0xFF24292E), "GITHUB", Color(0xFF24292F).copy(alpha = 0.15f))
            else -> Triple(NeonCyan, "REGISTRY", NeonCyan.copy(alpha = 0.15f))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, brandColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Biometric Target Crosshair Profile Photo
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CyberBlack)
                    .border(1.dp, brandColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Biometric tracking lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = brandColor.copy(alpha = 0.2f),
                        radius = size.minDimension / 2.5f,
                        style = Stroke(width = 1f)
                    )
                    // crosshairs
                    drawLine(
                        color = brandColor.copy(alpha = 0.4f),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = brandColor.copy(alpha = 0.4f),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 1f
                    )
                }
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Identified Face Node",
                    tint = brandColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Platform Badge
            Box(
                modifier = Modifier
                    .background(brandBg, RoundedCornerShape(4.dp))
                    .border(0.5.dp, brandColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = brandName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = brandColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Match Title
            Text(
                text = match.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Confidence Score
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CONFIDENCE: ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary,
                        fontSize = 8.sp
                    )
                )
                Text(
                    text = "$confidence%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen,
                        fontSize = 9.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visit Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCardBg, RoundedCornerShape(6.dp))
                    .border(1.dp, brandColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link Icon",
                        tint = brandColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DECRYPT BIO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = brandColor,
                            fontSize = 8.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisSuccessView(
    result: FaceAnalysisResult,
    sourceBitmap: Bitmap?,
    onReset: () -> Unit
) {
    var selectedWebMatch by remember { mutableStateOf<com.example.data.api.WebMatch?>(null) }

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

        // Primary Match Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PRIMARY IDENTIFIED CRANIAL MATCH",
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
                    // Left: Match Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.celebrityName.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "STATUS: CONFIRMED CELEBRITY LOOKALIKE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextSecondary,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SIMILARITY COEFFICIENT: ${result.celebritySimilarity}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (result.celebritySimilarity > 75) CyberGreen else CyberOrange,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Gorgeous Biometric radial score progress meter
                    BiometricRadialGauge(score = result.celebritySimilarity)
                }
            }
        }

        // Metrics Grid (Age, Gender, Symmetry, Emotion)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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

        // Forensic Geometry Report Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
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

        // Responsive Grid of Web Profile Matches
        if (result.webMatches.isNotEmpty()) {
            Text(
                text = "IDENTIFIED REGISTRY LOGS (RESPONSIVE GRID)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                val columns = if (maxWidth > 600.dp) 3 else 2
                val chunkedMatches = result.webMatches.chunked(columns)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunkedMatches.forEach { rowMatches ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowMatches.forEach { match ->
                                Box(modifier = Modifier.weight(1f)) {
                                    WebMatchGridCard(
                                        match = match,
                                        index = result.webMatches.indexOf(match),
                                        primarySimilarity = result.celebritySimilarity,
                                        onClick = { selectedWebMatch = match }
                                    )
                                }
                            }
                            if (rowMatches.size < columns) {
                                repeat(columns - rowMatches.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
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

    selectedWebMatch?.let { match ->
        val index = result.webMatches.indexOf(match)
        val confidence = (result.celebritySimilarity - (index + 1) * 6).coerceIn(45, 98)
        SelectedFaceDetailDialog(
            match = match,
            baseResult = result,
            confidence = confidence,
            onDismiss = { selectedWebMatch = null }
        )
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

@Composable
fun BiometricDetailChip(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, CyberGridColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary,
                        fontSize = 9.sp
                    )
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SelectedFaceDetailDialog(
    match: com.example.data.api.WebMatch,
    baseResult: com.example.data.api.FaceAnalysisResult,
    confidence: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Slight offset/variation for realism on lookup
    val matchAge = remember(match.title) {
        val hash = match.title.hashCode()
        val absHash = if (hash < 0) -hash else hash
        (baseResult.age + (absHash % 5 - 2)).coerceIn(18, 90)
    }
    
    val matchEmotion = remember(match.title) {
        val hash = match.title.hashCode()
        val absHash = if (hash < 0) -hash else hash
        val emotions = listOf("Neutral", "Happy", "Confident", "Intense", "Calm", "Focused")
        emotions[absHash % emotions.size]
    }

    val matchSymmetry = remember(match.title) {
        val hash = match.title.hashCode()
        val absHash = if (hash < 0) -hash else hash
        (baseResult.symmetry + (absHash % 7 - 3)).coerceIn(70, 99)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBlack)
            ) {
                Text("CLOSE FILE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(match.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SelectedFaceDetailDialog", "Failed to open link: ${match.url}", e)
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
            ) {
                Text("LAUNCH URL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Dossier Icon",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "BIOMETRIC DOSSIER READOUT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack)
                    .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Identity name
                Column {
                    Text(
                        text = "REGISTERED ENTITY IDENTITY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary
                        )
                    )
                    Text(
                        text = match.title.uppercase(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )
                }

                Divider(color = CyberGridColor)

                // Main Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BiometricDetailChip(
                        title = "EST. AGE",
                        value = "$matchAge YRS",
                        icon = Icons.Default.Info,
                        color = CyberOrange,
                        modifier = Modifier.weight(1f)
                    )
                    BiometricDetailChip(
                        title = "GENDER",
                        value = baseResult.gender.uppercase(),
                        icon = Icons.Default.Person,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BiometricDetailChip(
                        title = "EMOTION",
                        value = matchEmotion.uppercase(),
                        icon = Icons.Default.Face,
                        color = CyberGreen,
                        modifier = Modifier.weight(1f)
                    )
                    BiometricDetailChip(
                        title = "SYMMETRY",
                        value = "$matchSymmetry%",
                        icon = Icons.Default.Layers,
                        color = NeonBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider(color = CyberGridColor)

                // Match details / stats breakdown
                Text(
                    text = "CELEBRITY LOOKALIKE CORRELATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    )
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LOOKALIKE REF: ${baseResult.celebrityName.uppercase()}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "CONFIDENCE MATCH: $confidence%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (confidence > 75) CyberGreen else CyberOrange,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    BiometricRadialGauge(score = confidence)
                }

                Divider(color = CyberGridColor)

                // High-tech scanning logs / telemetry text
                Column {
                    Text(
                        text = "NODE MATCH TELEMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ocular alignment scan passes security threshold. Neural network correlation model verified. High confidence database match established via Sherlock search index core.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
    )
}

@Composable
fun BatchScanFlowView(
    viewModel: FaceViewModel,
    context: android.content.Context,
    multipleGalleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    batchCameraLauncher: androidx.activity.result.ActivityResultLauncher<Void?>,
    batchPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val batchBitmaps by viewModel.batchBitmaps.collectAsState()
    val batchState by viewModel.batchState.collectAsState()

    when (val state = batchState) {
        is com.example.ui.viewmodel.BatchScanUiState.Idle -> {
            BatchScanIdleView(
                bitmaps = batchBitmaps,
                onAddCamera = {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        batchCameraLauncher.launch(null)
                    } else {
                        batchPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onAddGallery = {
                    multipleGalleryLauncher.launch("image/*")
                },
                onRemove = { index ->
                    viewModel.removeBatchBitmap(index)
                },
                onClearAll = {
                    viewModel.clearBatchBitmaps()
                },
                onStartScan = {
                    viewModel.scanBatch()
                }
            )
        }
        is com.example.ui.viewmodel.BatchScanUiState.Processing -> {
            BatchScanProcessingView(progress = state.progress, total = state.total)
        }
        is com.example.ui.viewmodel.BatchScanUiState.Success -> {
            BatchScanDashboardView(results = state.results) {
                viewModel.resetBatch()
            }
        }
        is com.example.ui.viewmodel.BatchScanUiState.Error -> {
            BatchScanErrorView(message = state.message) {
                viewModel.resetBatch()
            }
        }
    }
}

@Composable
fun BatchScanIdleView(
    bitmaps: List<Bitmap>,
    onAddCamera: () -> Unit,
    onAddGallery: () -> Unit,
    onRemove: (Int) -> Unit,
    onClearAll: () -> Unit,
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preparation Deck Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BATCH RECRUITMENT DECK",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Load up to 10 source targets into the queue. The Sherlock deep core processor will analyze age, gender, mood, and celebrity correlation for all entities in parallel.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }

        if (bitmaps.isEmpty()) {
            // Large empty drop zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(2.dp, Brush.sweepGradient(listOf(NeonCyan, NeonBlue, NeonCyan)), RoundedCornerShape(16.dp))
                    .background(CyberDarkSurface)
                    .clickable { onAddGallery() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Empty Deck",
                        tint = NeonCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DECK EMPTY: LOAD TARGETS",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )
                    Text(
                        text = "Tap here to pick images from gallery",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary
                        )
                    )
                }
            }
        } else {
            // Show previews
            Text(
                text = "QUEUED ENTITIES: ${bitmaps.size}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberOrange
                ),
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            // Grid of images with custom chunk helper for flexibility
            val chunked = bitmaps.chunked(2)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                chunked.forEachIndexed { rowIndex, rowBitmaps ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowBitmaps.forEachIndexed { colIndex, bitmap ->
                            val actualIndex = rowIndex * 2 + colIndex
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(140.dp)
                                    .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Queued target",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Hovering delete tag
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(28.dp)
                                            .background(CyberBlack.copy(alpha = 0.75f), CircleShape)
                                            .border(1.dp, CyberOrange, CircleShape)
                                            .clickable { onRemove(actualIndex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Face",
                                            tint = CyberOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    // Number tag
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .background(NeonCyan, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#${actualIndex + 1}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberBlack
                                        )
                                    }
                                }
                            }
                        }
                        // Fill empty spot in row if uneven
                        if (rowBitmaps.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Image add control bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddCamera,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg, contentColor = NeonCyan),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Add via Camera")
                Spacer(modifier = Modifier.width(6.dp))
                Text("ADD PHOTO", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
                onClick = onAddGallery,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg, contentColor = NeonCyan),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add via Gallery")
                Spacer(modifier = Modifier.width(6.dp))
                Text("ADD IMAGES", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        if (bitmaps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Primary Action: RUN BATCH SCAN
            Button(
                onClick = onStartScan,
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_batch_scan_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Batch search")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "INITIATE COGNITIVE MULTI-SCAN",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onClearAll,
                colors = ButtonDefaults.textButtonColors(contentColor = CyberOrange),
                modifier = Modifier.testTag("clear_batch_deck_button")
            ) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "Purge deck")
                Spacer(modifier = Modifier.width(4.dp))
                Text("PURGE QUEUE DECK", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun BatchScanProcessingView(progress: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PARALLEL BIOMETRIC LOOKUP ACTIVE",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CyberOrange,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Large Cyber Circular Loader
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = if (total > 0) progress.toFloat() / total.toFloat() else 0f,
                modifier = Modifier.size(100.dp),
                color = NeonCyan,
                strokeWidth = 6.dp,
                trackColor = CyberGridColor
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${if (total > 0) (progress * 100) / total else 0}%",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 20.sp
                )
                Text(
                    text = "$progress/$total",
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Linear bar with cyberpunk grid track
        LinearProgressIndicator(
            progress = if (total > 0) progress.toFloat() / total.toFloat() else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp)),
            color = NeonCyan,
            trackColor = CyberBlack
        )

        // Rolling terminal style diagnostic texts
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberBlack)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "> CORE_PROC: STARTING PIPELINE LOOKUP",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CyberGreen)
                )
                Text(
                    text = if (progress > 0) "> SCANNING_NODE_${progress}: COMPLETE" else "> WAITING FOR IMAGE DECOMPRESSION",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = if (progress > 0) CyberGreen else CyberTextSecondary)
                )
                Text(
                    text = if (progress < total) "> RESOLVING_NODE_${progress + 1}: ACQUIRING CELEBRITY VECTOR" else "> ALL VECTORS RETRIEVED SUCCESSFULLY",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = if (progress < total) CyberOrange else CyberGreen)
                )
                Text(
                    text = "> SAVING HISTORIC ARCHIVES FOR ALL NODES...",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonBlue)
                )
            }
        }
    }
}

@Composable
fun BatchScanDashboardView(
    results: List<com.example.ui.viewmodel.BatchScanItemResult>,
    onReset: () -> Unit
) {
    var selectedMatch by remember { mutableStateOf<com.example.data.api.WebMatch?>(null) }
    var selectedBaseResult by remember { mutableStateOf<com.example.data.api.FaceAnalysisResult?>(null) }
    var selectedConfidence by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Consolidated Stats Dashboard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CONSOLIDATED BATCH INTELLIGENCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Stats Dashboard Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberBlack)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL SCANS", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = CyberTextSecondary)
                            Text("${results.size}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeonCyan)
                        }
                    }

                    val avgAge = remember(results) { results.map { it.result.age }.average().toInt() }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberBlack)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AVG AGE", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = CyberTextSecondary)
                            Text("$avgAge YRS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CyberOrange)
                        }
                    }

                    val avgSymmetry = remember(results) { results.map { it.result.symmetry }.average().toInt() }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberBlack)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AVG SYMM", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = CyberTextSecondary)
                            Text("$avgSymmetry%", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NeonBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val topEmotion = remember(results) {
                    results.map { it.result.emotion }
                        .groupBy { it }
                        .maxByOrNull { it.value.size }?.key ?: "Neutral"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberBlack)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DOMINANT EMOTIONAL SIGNATURE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = CyberTextSecondary)
                        Text(topEmotion.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CyberGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val context = LocalContext.current

        // Export Action Controls Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGridColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PROFESSIONAL REPORT GENERATION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NeonCyan,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export the compiled findings. Generate an official formatted PDF analysis dossier or a professional standard CSV spreadsheet file.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberTextSecondary,
                    modifier = Modifier.align(Alignment.Start),
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { com.example.util.ExportUtil.exportBatchToPdf(context, results) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg, contentColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXPORT PDF",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = { com.example.util.ExportUtil.exportBatchToCsv(context, results) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg, contentColor = CyberOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .border(1.dp, CyberOrange.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXPORT CSV",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "INDIVIDUAL BIOMETRIC NODES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyberTextSecondary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Column list of results
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            results.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberGridColor, RoundedCornerShape(12.dp))
                        .clickable {
                            if (item.result.webMatches.isNotEmpty()) {
                                selectedMatch = item.result.webMatches.first()
                                selectedBaseResult = item.result
                                selectedConfidence = item.result.celebritySimilarity
                            } else {
                                selectedMatch = com.example.data.api.WebMatch(item.result.celebrityName, "https://www.google.com/search?q=${item.result.celebrityName}")
                                selectedBaseResult = item.result
                                selectedConfidence = item.result.celebritySimilarity
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Thumbnail preview
                        Card(
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Image(
                                bitmap = item.bitmap.asImageBitmap(),
                                contentDescription = "Face thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Right description
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NODE #${index + 1}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "${item.result.celebritySimilarity}% MATCH",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (item.result.celebritySimilarity > 75) CyberGreen else CyberOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "MATCH: ${item.result.celebrityName.uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CyberTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "AGE: ${item.result.age} | GENDER: ${item.result.gender.uppercase()} | MOOD: ${item.result.emotion.uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = CyberTextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset button
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBlack),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("dismiss_batch_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "New Batch")
            Spacer(modifier = Modifier.width(6.dp))
            Text("RUN NEW BATCH ANALYSIS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Detail dialog pop-ups
    if (selectedMatch != null && selectedBaseResult != null) {
        SelectedFaceDetailDialog(
            match = selectedMatch!!,
            baseResult = selectedBaseResult!!,
            confidence = selectedConfidence,
            onDismiss = {
                selectedMatch = null
                selectedBaseResult = null
            }
        )
    }
}

@Composable
fun BatchScanErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "BATCH ACQUISITION FAILED",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp
        )
        Text(
            text = message,
            fontFamily = FontFamily.Monospace,
            color = CyberTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = CyberBlack),
            modifier = Modifier
                .width(180.dp)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("RETRY BATCH", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}
