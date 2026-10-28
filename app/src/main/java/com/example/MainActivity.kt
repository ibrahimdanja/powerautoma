package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.BatteryScreen
import com.example.ui.screens.GalaxyGuideScreen
import com.example.ui.screens.QuickPowerScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SCHEDULE(
        title = "Schedule",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule,
        testTag = "nav_schedule"
    ),
    QUICK_POWER(
        title = "Controls",
        selectedIcon = Icons.Filled.PowerSettingsNew,
        unselectedIcon = Icons.Outlined.PowerSettingsNew,
        testTag = "nav_quick_power"
    ),
    BATTERY(
        title = "Battery",
        selectedIcon = Icons.Filled.BatteryChargingFull,
        unselectedIcon = Icons.Outlined.BatteryChargingFull,
        testTag = "nav_battery"
    ),
    GUIDE(
        title = "A10s Guide",
        selectedIcon = Icons.Filled.HelpOutline,
        unselectedIcon = Icons.Outlined.HelpOutline,
        testTag = "nav_guide"
    )
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                // Request Notification Permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                MainScreenContent(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}

@Composable
fun MainScreenContent(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var currentTab by rememberSaveable { mutableStateOf(NavigationTab.SCHEDULE) }

    // Custom back handler to pop back to Schedule screen if in a sub-tab
    if (currentTab != NavigationTab.SCHEDULE) {
        BackHandler {
            currentTab = NavigationTab.SCHEDULE
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    NavigationTab.SCHEDULE -> {
                        ScheduleScreen(
                            state = uiState,
                            onSetSleepTime = { h, m -> viewModel.setSleepTime(h, m) },
                            onSetWakeTime = { h, m -> viewModel.setWakeTime(h, m) },
                            onToggleSleep = { viewModel.toggleSleep(it) },
                            onToggleWake = { viewModel.toggleWake(it) },
                            onToggleDay = { viewModel.toggleDay(it) },
                            onSelectAllDays = {
                                viewModel.updateConfig(
                                    uiState.config.copy(activeDays = (1..7).toSet())
                                )
                            },
                            onSelectWeekdays = {
                                viewModel.updateConfig(
                                    uiState.config.copy(
                                        activeDays = setOf(2, 3, 4, 5, 6)
                                    )
                                )
                            },
                            onSelectSleepAction = { viewModel.setSleepAction(it) },
                            onToggleDnd = { viewModel.setDndEnabled(it) },
                            onToggleCountdown = { viewModel.setCountdownEnabled(it) },
                            onToggleWakeSound = { viewModel.setWakeSoundEnabled(it) },
                            onToggleWakeVibration = { viewModel.setWakeVibrationEnabled(it) },
                            onTestSleep = { viewModel.testSleepIn10Seconds() },
                            onTestWake = { viewModel.testWakeIn10Seconds() },
                            onDismissTestMessage = { viewModel.clearTestMessage() },
                            onNavigateToGuide = { currentTab = NavigationTab.GUIDE }
                        )
                    }

                    NavigationTab.QUICK_POWER -> {
                        QuickPowerScreen(
                            state = uiState,
                            onLockScreen = { viewModel.lockScreenNow() },
                            onOpenPowerMenu = { viewModel.openPowerMenuNow() },
                            onOpenSamsungDeviceCare = { viewModel.openSamsungDeviceCare() },
                            onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                            onRequestDeviceAdmin = { viewModel.requestDeviceAdmin() },
                            onSleepNow = { viewModel.testSleepIn10Seconds() }
                        )
                    }

                    NavigationTab.BATTERY -> {
                        BatteryScreen(state = uiState)
                    }

                    NavigationTab.GUIDE -> {
                        GalaxyGuideScreen(
                            state = uiState,
                            onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                            onRequestDeviceAdmin = { viewModel.requestDeviceAdmin() },
                            onOpenDndSettings = { viewModel.openDndSettings() },
                            onOpenExactAlarmSettings = { viewModel.openExactAlarmSettings() },
                            onOpenSamsungDeviceCare = { viewModel.openSamsungDeviceCare() },
                            onRefreshPermissions = { viewModel.refreshPermissions() }
                        )
                    }
                }
            }
        }
    }
}
