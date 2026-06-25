package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.FaceScanEntity
import com.example.data.database.FaceComparisonEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: FaceViewModel,
    modifier: Modifier = Modifier
) {
    val scanHistory by viewModel.scanHistory.collectAsState()
    val comparisonHistory by viewModel.comparisonHistory.collectAsState()
    
    var selectedFilterTab by remember { mutableStateOf(0) } // 0 = Single Scans, 1 = Comparisons
    var selectedScanForDetail by remember { mutableStateOf<FaceScanEntity?>(null) }
    var selectedCompareForDetail by remember { mutableStateOf<FaceComparisonEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-Tech Sub-Header
        Text(
            text = "FORENSIC ARCHIVES",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "DECRYPTED COGNITIVE BIOMETRIC LOGS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Custom High-Tech Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(CyberDarkSurface, RoundedCornerShape(12.dp))
                .border(1.dp, CyberGridColor, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                text = "IDENTIFIED MATCHES",
                isSelected = selectedFilterTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { selectedFilterTab = 0 }
            )
            TabButton(
                text = "COMPARISONS",
                isSelected = selectedFilterTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { selectedFilterTab = 1 }
            )
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val recordCount = if (selectedFilterTab == 0) scanHistory.size else comparisonHistory.size
            Text(
                text = "TOTAL RECORDS: $recordCount",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            )

            if (recordCount > 0) {
                TextButton(
                    onClick = {
                        if (selectedFilterTab == 0) {
                            viewModel.clearAllScans()
                        } else {
                            viewModel.clearAllComparisons()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberOrange),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear All", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PURGE LOGS", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace))
                }
            }
        }

        // Responsive Grid Layout
        if (selectedFilterTab == 0) {
            if (scanHistory.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Face,
                    text = "No biometric scan records found.\nPerform a face search to compile lookup databases."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scanHistory, key = { it.id }) { scan ->
                        ScanGridItem(
                            scan = scan,
                            onItemClick = { selectedScanForDetail = scan },
                            onDeleteClick = { viewModel.deleteScan(scan.id) }
                        )
                    }
                }
            }
        } else {
            if (comparisonHistory.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.CompareArrows,
                    text = "No comparative analysis records found.\nUpload subject pairs in comparator core."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comparisonHistory, key = { it.id }) { comparison ->
                        ComparisonGridItem(
                            comparison = comparison,
                            onItemClick = { selectedCompareForDetail = comparison },
                            onDeleteClick = { viewModel.deleteComparison(comparison.id) }
                        )
                    }
                }
            }
        }
    }

    // Interactive Detailed Popup Dialogs
    selectedScanForDetail?.let { scan ->
        ScanDetailDialog(
            scan = scan,
            onDismiss = { selectedScanForDetail = null }
        )
    }

    selectedCompareForDetail?.let { comparison ->
        ComparisonDetailDialog(
            comparison = comparison,
            onDismiss = { selectedCompareForDetail = null }
        )
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .background(
                if (isSelected) NeonBlue.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) NeonCyan else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) NeonCyan else CyberTextSecondary
            )
        )
    }
}

@Composable
fun ScanGridItem(
    scan: FaceScanEntity,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGridColor, RoundedCornerShape(12.dp))
            .clickable { onItemClick() }
            .testTag("scan_grid_item_${scan.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
    ) {
        Column {
            // Header Image with Scan Indicator HUD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(CyberBlack)
            ) {
                if (scan.imagePath != null && File(scan.imagePath).exists()) {
                    AsyncImage(
                        model = File(scan.imagePath),
                        contentDescription = scan.celebrityName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "No Image",
                            tint = CyberTextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Confidence Score Overlay HUD
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(CyberBlack.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${scan.celebritySimilarity}% MATCH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // Body Metadata Text Panel
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = scan.celebrityName.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${scan.age} YRS · ${scan.gender.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scan.emotion.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = CyberOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonGridItem(
    comparison: FaceComparisonEntity,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGridColor, RoundedCornerShape(12.dp))
            .clickable { onItemClick() }
            .testTag("comparison_grid_item_${comparison.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface)
    ) {
        Column {
            // Double Image Frame Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(CyberBlack)
            ) {
                // Alpha Image
                Box(modifier = Modifier.weight(1f)) {
                    if (comparison.imagePath1 != null && File(comparison.imagePath1).exists()) {
                        AsyncImage(
                            model = File(comparison.imagePath1),
                            contentDescription = "Alpha",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                // Vertical grid split line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(NeonCyan.copy(alpha = 0.5f))
                )

                // Beta Image
                Box(modifier = Modifier.weight(1f)) {
                    if (comparison.imagePath2 != null && File(comparison.imagePath2).exists()) {
                        AsyncImage(
                            model = File(comparison.imagePath2),
                            contentDescription = "Beta",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Body Info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = comparison.matchStatus.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (comparison.similarityScore > 75) CyberGreen else CyberOrange
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${comparison.similarityScore}% COHERENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            fontSize = 10.sp
                        )
                    )

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = CyberOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty",
            tint = NeonBlue.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScanDetailDialog(
    scan: FaceScanEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBlack)
            ) {
                Text("DISMISS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = "BIOMETRIC FILE READOUT",
                fontFamily = FontFamily.Monospace,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack)
                    .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                // Large Similarity Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "MATCH INDEX:",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CyberTextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${scan.celebritySimilarity}% similarity with ${scan.celebrityName}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberOrange
                        )
                    )
                }

                Divider(color = CyberGridColor, modifier = Modifier.padding(vertical = 12.dp))

                // Detailed Analysis
                Text(
                    text = "FORENSIC ANALYSIS REPORT",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonBlue),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = scan.analysis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        lineHeight = 18.sp
                    )
                )

                // Render web profile matches
                val matches = scan.webMatches.split(";").filter { it.isNotBlank() }
                if (matches.isNotEmpty()) {
                    Divider(color = CyberGridColor, modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = "IDENTIFIED REGISTRY TARGETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonCyan),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    matches.forEach { match ->
                        val parts = match.split("|")
                        if (parts.size >= 2) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Link, contentDescription = "link", tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = parts[0],
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = CyberTextPrimary)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
    )
}

@Composable
fun ComparisonDetailDialog(
    comparison: FaceComparisonEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBlack)
            ) {
                Text("DISMISS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = "COHERENCE SPECTRUM LOG",
                fontFamily = FontFamily.Monospace,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack)
                    .border(1.dp, CyberGridColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "COHERENCE INDEX:",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CyberTextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${comparison.similarityScore}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (comparison.similarityScore > 75) CyberGreen else CyberOrange
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DECISION:",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CyberTextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comparison.matchStatus.uppercase(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (comparison.similarityScore > 75) CyberGreen else CyberOrange
                        )
                    )
                }

                Divider(color = CyberGridColor, modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "CRANIAL GEOMETRY REPORT",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonBlue),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = comparison.analysisReport,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        lineHeight = 18.sp
                    )
                )
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
    )
}
