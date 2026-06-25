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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceViewModel
import com.example.ui.viewmodel.FaceViewModelFactory
import com.example.ui.viewmodel.Tab

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database & Repository
        val database = FaceDatabase.getDatabase(this)
        val repository = FaceRepository(database.faceDao())

        setContent {
            val factory = FaceViewModelFactory(application, repository)
            val viewModel: FaceViewModel = viewModel(factory = factory)
            val userDarkThemeSetting by viewModel.isDarkTheme.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = userDarkThemeSetting ?: systemDark

            MyApplicationTheme(darkTheme = isDark) {
                val currentTab by viewModel.currentTab.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Sherlock Face Search",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.toggleTheme(systemDark) },
                                    modifier = Modifier.testTag("theme_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                        contentDescription = "Toggle Dark/Light Mode"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    },
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
                            .background(MaterialTheme.colorScheme.background)
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
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        NavigationBarItem(
            selected = selectedTab == Tab.SCAN,
            onClick = { onTabSelected(Tab.SCAN) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Scan",
                    tint = if (selectedTab == Tab.SCAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    text = "Scan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (selectedTab == Tab.SCAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.testTag("nav_tab_scan")
        )

        NavigationBarItem(
            selected = selectedTab == Tab.COMPARE,
            onClick = { onTabSelected(Tab.COMPARE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = "Compare",
                    tint = if (selectedTab == Tab.COMPARE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    text = "Compare",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (selectedTab == Tab.COMPARE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.testTag("nav_tab_compare")
        )

        NavigationBarItem(
            selected = selectedTab == Tab.HISTORY,
            onClick = { onTabSelected(Tab.HISTORY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = if (selectedTab == Tab.HISTORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    text = "History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (selectedTab == Tab.HISTORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.testTag("nav_tab_history")
        )
    }
}
