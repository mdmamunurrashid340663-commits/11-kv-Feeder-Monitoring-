package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.Esp32GuideScreen
import com.example.ui.screens.FaultProtectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GridDarkNavy
import com.example.ui.theme.GridSurfaceNavy
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusFaultRed
import com.example.ui.theme.StatusNormalGreen
import com.example.ui.viewmodel.ConnectionStatus
import com.example.ui.viewmodel.GridViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: GridViewModel = viewModel()
) {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    val isBengali by viewModel.isBengali.collectAsState()
    val gridState by viewModel.gridState.collectAsState()
    val connStatus by viewModel.connectionStatus.collectAsState()
    val espConfig by viewModel.espConfig.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Grid Icon",
                            tint = ElectricGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isBengali) "১১ কেভি গ্রিড ও ফল্ট মনিটর" else "11kV Grid & Fault Monitor",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (espConfig.isSimulationMode) "SIMULATION MODE • 11.0 kV Feeder" else "ESP32 IoT (${espConfig.ipAddress})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (espConfig.isSimulationMode) ElectricCyan else StatusNormalGreen
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Badge Button
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("app_bar_lang_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isBengali) "EN" else "বাং",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ElectricCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GridSurfaceNavy
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = GridSurfaceNavy,
                tonalElevation = 8.dp
            ) {
                // Tab 0: Dashboard
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = {
                        Text(
                            text = if (isBengali) "ড্যাশবোর্ড" else "Dashboard",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = navItemColors(),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                // Tab 1: Protection & Fault History (With alert badge if fault active)
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        if (gridState.hasActiveFault) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = StatusFaultRed) {
                                        Text("!", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Protection"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Protection"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = if (isBengali) "ফল্ট প্রোটেকশন" else "Protection",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = navItemColors(),
                    modifier = Modifier.testTag("nav_protection")
                )

                // Tab 2: ESP32 Hardware Guide & Arduino Code
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "ESP32 Guide"
                        )
                    },
                    label = {
                        Text(
                            text = if (isBengali) "ESP32 গাইড" else "ESP32 Code",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = navItemColors(),
                    modifier = Modifier.testTag("nav_esp32_guide")
                )

                // Tab 3: Settings
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = {
                        Text(
                            text = if (isBengali) "সেটিংস" else "Settings",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = navItemColors(),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GridDarkNavy)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(viewModel = viewModel)
                1 -> FaultProtectionScreen(viewModel = viewModel)
                2 -> Esp32GuideScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ElectricGold,
    selectedTextColor = ElectricGold,
    unselectedIconColor = Color(0xFF64748B),
    unselectedTextColor = Color(0xFF64748B),
    indicatorColor = ElectricGold.copy(alpha = 0.15f)
)
