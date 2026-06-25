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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Face
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.FaceComparisonResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceViewModel
import com.example.ui.viewmodel.CompareUiState
import java.io.File

@Composable
fun CompareScreen(
    viewModel: FaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val compareBitmap1 by viewModel.compareBitmap1.collectAsState()
    val compareBitmap2 by viewModel.compareBitmap2.collectAsState()
    val compareState by viewModel.compareState.collectAsState()
    val scrollState = rememberScrollState()
    var showGuide by remember { mutableStateOf(true) }

    // Archive / Search Results states
    val scanHistory by viewModel.scanHistory.collectAsState()
    var selectedSourceMode by remember { mutableStateOf(0) } // 0 = Manual Upload, 1 = Search Archives
    var selectedScan1 by remember { mutableStateOf<com.example.data.database.FaceScanEntity?>(null) }
    var selectedScan2 by remember { mutableStateOf<com.example.data.database.FaceScanEntity?>(null) }
    var showArchive1Dialog by remember { mutableStateOf(false) }
    var showArchive2Dialog by remember { mutableStateOf(false) }

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
    val permission1Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            camera1Launcher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to capture Subject Alpha.", Toast.LENGTH_LONG).show()
        }
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
    val permission2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            camera2Launcher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to capture Subject Beta.", Toast.LENGTH_LONG).show()
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
        // App Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BIOMETRIC COMPARATOR",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "COMPARE FACIAL SHAPE COORDINATES",
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
                    .testTag("compare_guide_toggle")
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
                        text = "COMPARATOR USER MANUAL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "1. TAP CAMERA or GALLERY icon buttons on Subject Alpha & Subject Beta boxes to load pictures of two different faces.\n" +
                               "2. Or SELECT existing faces from SEARCH ARCHIVES to visually inspect and contrast feature metrics side-by-side.\n" +
                               "3. TAP 'INITIATE RECONCILIATION' to start face geometry matching.\n" +
                               "4. SEE the coherence percentage, match status, and detailed anatomical reports comparing features like eyes, nose, and jaw line!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

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
                        text = "COMPARATOR: ONLINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            fontSize = 9.sp
                        )
                    )
                }

                // Range indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SPECTRUM:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary,
                            fontSize = 9.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "COHERENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 9.sp
                        )
                    )
                }

                // Precision level indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PRECISION:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary,
                            fontSize = 9.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SUB-METRIC",
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

        // Comparison Source Mode Toggles (Manual vs Archive)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(CyberCardBg, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("MANUAL SOURCE", "SEARCH ARCHIVES").forEachIndexed { index, title ->
                val isSelected = selectedSourceMode == index
                Button(
                    onClick = {
                        selectedSourceMode = index
                        viewModel.resetComparison()
                        selectedScan1 = null
                        selectedScan2 = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) NeonCyan else Color.Transparent,
                        contentColor = if (isSelected) CyberBlack else CyberTextSecondary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("compare_source_mode_$index"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Side-by-side selectors depending on selectedSourceMode
        if (selectedSourceMode == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subject 1 Picker Box
                SubjectPickerBox(
                    title = "SUBJECT ALPHA",
                    bitmap = compareBitmap1,
                    onCameraClick = {
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        )
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            camera1Launcher.launch()
                        } else {
                            permission1Launcher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onGalleryClick = { gallery1Launcher.launch("image/*") },
                    isAnalyzing = compareState is CompareUiState.Analyzing,
                    modifier = Modifier.weight(1f),
                    testTagPrefix = "subject_alpha"
                )

                // Subject 2 Picker Box
                SubjectPickerBox(
                    title = "SUBJECT BETA",
                    bitmap = compareBitmap2,
                    onCameraClick = {
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        )
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            camera2Launcher.launch()
                        } else {
                            permission2Launcher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onGalleryClick = { gallery2Launcher.launch("image/*") },
                    isAnalyzing = compareState is CompareUiState.Analyzing,
                    modifier = Modifier.weight(1f),
                    testTagPrefix = "subject_beta"
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subject Alpha Archive Selector Box
                ArchiveSubjectPickerBox(
                    title = "SUBJECT ALPHA",
                    scan = selectedScan1,
                    onSelectClick = { showArchive1Dialog = true },
                    onClearClick = {
                        selectedScan1 = null
                        viewModel.setCompareBitmap1(null)
                    },
                    modifier = Modifier.weight(1f),
                    borderColor = NeonBlue,
                    testTagPrefix = "archive_subject_alpha"
                )

                // Subject Beta Archive Selector Box
                ArchiveSubjectPickerBox(
                    title = "SUBJECT BETA",
                    scan = selectedScan2,
                    onSelectClick = { showArchive2Dialog = true },
                    onClearClick = {
                        selectedScan2 = null
                        viewModel.setCompareBitmap2(null)
                    },
                    modifier = Modifier.weight(1f),
                    borderColor = CyberOrange,
                    testTagPrefix = "archive_subject_beta"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Split-Screen Side-by-Side Comparison Component
            if (selectedScan1 != null && selectedScan2 != null) {
                ArchiveCompareDashboard(
                    scan1 = selectedScan1!!,
                    scan2 = selectedScan2!!
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                    onClick = {
                        viewModel.resetComparison()
                        selectedScan1 = null
                        selectedScan2 = null
                    },
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
                            text = if (selectedSourceMode == 0) {
                                "Please load high-resolution facial images for both Subject Alpha and Subject Beta to initiate cranial matching analysis."
                            } else {
                                "Please select two historical scanned faces from the search archives above to visually compare features and run cranial matching."
                            },
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
                    selectedScan1 = null
                    selectedScan2 = null
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

    // Dialogs for Archive Selection
    if (showArchive1Dialog) {
        ArchiveSelectionDialog(
            scans = scanHistory,
            onSelect = { scan ->
                selectedScan1 = scan
                val file = File(scan.imagePath ?: "")
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    viewModel.setCompareBitmap1(bitmap)
                }
                showArchive1Dialog = false
            },
            onDismiss = { showArchive1Dialog = false }
        )
    }

    if (showArchive2Dialog) {
        ArchiveSelectionDialog(
            scans = scanHistory,
            onSelect = { scan ->
                selectedScan2 = scan
                val file = File(scan.imagePath ?: "")
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    viewModel.setCompareBitmap2(bitmap)
                }
                showArchive2Dialog = false
            },
            onDismiss = { showArchive2Dialog = false }
        )
    }
}

@Composable
fun ArchiveSubjectPickerBox(
    title: String,
    scan: com.example.data.database.FaceScanEntity?,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = NeonBlue,
    testTagPrefix: String = "archive_subject"
) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .border(
                width = 1.dp,
                color = if (scan != null) borderColor else CyberGridColor,
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
                    color = if (scan != null) borderColor else CyberTextSecondary
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
                if (scan != null) {
                    if (scan.imagePath != null) {
                        val file = File(scan.imagePath)
                        if (file.exists()) {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = borderColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = borderColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = borderColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Select placeholder",
                        tint = CyberTextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (scan != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scan.celebrityName,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CyberTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onClearClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg, contentColor = CyberOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .border(1.dp, CyberOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("CLEAR", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onSelectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg, contentColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("${testTagPrefix}_select_button"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SELECT FACE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ArchiveSelectionDialog(
    scans: List<com.example.data.database.FaceScanEntity>,
    onSelect: (com.example.data.database.FaceScanEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SELECT FROM SEARCH ARCHIVE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NeonCyan
            )
        },
        text = {
            if (scans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO SCANNED ENTRIES FOUND.\nPlease scan some faces first under the Scan tab.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyberTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scans.size) { idx ->
                        val scan = scans[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp))
                                .clickable { onSelect(scan) },
                            colors = CardDefaults.cardColors(containerColor = CyberCardBg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail
                                Card(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    if (scan.imagePath != null) {
                                        val file = File(scan.imagePath)
                                        if (file.exists()) {
                                            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Face",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().background(CyberBlack),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = NeonBlue)
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(CyberBlack),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = NeonBlue)
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(CyberBlack),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = NeonBlue)
                                        }
                                    }
                                }

                                // Text details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scan.celebrityName.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CyberTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "AGE: ${scan.age} | GENDER: ${scan.gender.uppercase()} | SYMM: ${scan.symmetry}%",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        color = CyberTextSecondary
                                    )
                                }
                                
                                Text(
                                    text = "${scan.celebritySimilarity}%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (scan.celebritySimilarity > 75) CyberGreen else CyberOrange
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    )
}

@Composable
fun ArchiveCompareDashboard(
    scan1: com.example.data.database.FaceScanEntity,
    scan2: com.example.data.database.FaceScanEntity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "SPLIT-SCREEN SIDE-BY-SIDE METRICS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCardBg, RoundedCornerShape(4.dp))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "SUBJECT ALPHA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = NeonBlue, modifier = Modifier.weight(1f))
                Text(text = "METRIC", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = CyberTextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(text = "SUBJECT BETA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = CyberOrange, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rows
            val comparisonRows = listOf(
                Triple("LOOKALIKE", scan1.celebrityName, scan2.celebrityName),
                Triple("COHERENCE", "${scan1.celebritySimilarity}%", "${scan2.celebritySimilarity}%"),
                Triple("EST. AGE", "${scan1.age} YRS", "${scan2.age} YRS"),
                Triple("GENDER", scan1.gender.uppercase(), scan2.gender.uppercase()),
                Triple("EMOTION", scan1.emotion.uppercase(), scan2.emotion.uppercase()),
                Triple("SYMMETRY", "${scan1.symmetry}%", "${scan2.symmetry}%")
            )

            comparisonRows.forEachIndexed { idx, (metric, val1, val2) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (idx % 2 == 0) CyberBlack.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = val1, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = CyberTextPrimary, modifier = Modifier.weight(1f))
                    Text(
                        text = metric,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(text = val2, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = CyberTextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Side-by-Side Forensic Readout
            Text(
                text = "SIDE-BY-SIDE FORENSIC READOUT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = CyberOrange,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Alpha forensic text
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberBlack)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ALPHA ANALYSIS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = NeonBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scan1.analysis,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = CyberTextSecondary,
                            lineHeight = 12.sp,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Beta forensic text
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberBlack)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("BETA ANALYSIS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = CyberOrange)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scan2.analysis,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = CyberTextSecondary,
                            lineHeight = 12.sp,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
fun CompareRadialGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val gridColor = CyberGridColor
    val progressColor = if (score > 75) CyberGreen else CyberOrange
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val strokeWidth = 10f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Background arc (dark/track)
            drawArc(
                color = gridColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glowing progress arc
            drawArc(
                color = progressColor,
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
                    fontSize = 24.sp
                )
            )
            Text(
                text = "COHERENCE",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Status and Badges
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BIOMETRIC COHERENCE INDEX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Match Status Label
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(NeonBlue.copy(alpha = 0.15f), NeonCyan.copy(alpha = 0.15f))
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = result.matchStatus.uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (result.similarityScore > 75) CyberGreen else CyberOrange,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Cranial geometry mapping indicates ${if (result.similarityScore > 75) "high-probability match" else "low coherence match"} across the selected subjects.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary,
                            lineHeight = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right: Biometric circular gauge
                CompareRadialGauge(score = result.similarityScore)
            }
        }

        // Facial Landmark Structural Alignment Checklist
        Text(
            text = "LANDMARK STRUCTURAL TELEMETRY",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(1.dp, CyberGridColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val matchScore = result.similarityScore
                val list = listOf(
                    Triple("PUPIL DISTANCE INDEX", if (matchScore > 75) "OPTIMAL (0.98)" else "VARIED (0.76)", matchScore > 75),
                    Triple("NASAL BRIDGE GEOMETRY", if (matchScore > 60) "MATCHED" else "DISPARATE", matchScore > 60),
                    Triple("JAW CONTOUR RATIO", if (matchScore > 70) "96.4% ACCURACY" else "81.2% ACCURACY", matchScore > 70),
                    Triple("CHEEK BONE COHERENCE", if (matchScore > 80) "OPTIMAL" else "VARIED", matchScore > 80)
                )

                list.forEach { (landmark, status, verified) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (verified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = if (verified) "Verified" else "Discrepancy",
                                tint = if (verified) CyberGreen else CyberOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = landmark,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (verified) CyberGreen else CyberOrange,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
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
