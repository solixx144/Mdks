package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.FaceDatabase
import com.example.data.repository.FaceRepository
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceViewModel
import com.example.ui.viewmodel.FaceViewModelFactory
import com.example.ui.viewmodel.Tab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database & Repository
        val database = FaceDatabase.getDatabase(this)
        val repository = FaceRepository(database.faceDao())

        setContent {
            MyApplicationTheme {
                val factory = FaceViewModelFactory(application, repository)
                val viewModel: FaceViewModel = viewModel(factory = factory)
                val currentTab by viewModel.currentTab.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigationBar(
                            selectedTab = currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(CyberBlack)
                    ) {
                        when (currentTab) {
                            Tab.SCAN -> ScanScreen(viewModel = viewModel)
                            Tab.COMPARE -> CompareScreen(viewModel = viewModel)
                            Tab.HISTORY -> HistoryScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit
) {
    NavigationBar(
        containerColor = CyberDarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = CyberGridColor, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        NavigationBarItem(
            selected = selectedTab == Tab.SCAN,
            onClick = { onTabSelected(Tab.SCAN) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Scan Core",
                    tint = if (selectedTab == Tab.SCAN) NeonCyan else CyberTextSecondary
                )
            },
            label = {
                Text(
                    text = "SCAN CORE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (selectedTab == Tab.SCAN) NeonCyan else CyberTextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = NeonBlue.copy(alpha = 0.25f)
            ),
            modifier = Modifier.testTag("nav_tab_scan")
        )

        NavigationBarItem(
            selected = selectedTab == Tab.COMPARE,
            onClick = { onTabSelected(Tab.COMPARE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = "Comparator",
                    tint = if (selectedTab == Tab.COMPARE) NeonCyan else CyberTextSecondary
                )
            },
            label = {
                Text(
                    text = "COMPARATOR",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (selectedTab == Tab.COMPARE) NeonCyan else CyberTextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = NeonBlue.copy(alpha = 0.25f)
            ),
            modifier = Modifier.testTag("nav_tab_compare")
        )

        NavigationBarItem(
            selected = selectedTab == Tab.HISTORY,
            onClick = { onTabSelected(Tab.HISTORY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Archives",
                    tint = if (selectedTab == Tab.HISTORY) NeonCyan else CyberTextSecondary
                )
            },
            label = {
                Text(
                    text = "ARCHIVES",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (selectedTab == Tab.HISTORY) NeonCyan else CyberTextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = NeonBlue.copy(alpha = 0.25f)
            ),
            modifier = Modifier.testTag("nav_tab_history")
        )
    }
}
