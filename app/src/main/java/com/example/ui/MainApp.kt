package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.InteractiveGuideDialog
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MerchantProfileScreen
import com.example.ui.screens.PrinterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.QrisRed
import com.example.ui.theme.QrisTeal

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: QrisViewModel = viewModel()
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val showGuide by viewModel.showGuide.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime permissions launcher (Bluetooth & Notifications)
    val appPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permission result handled */ }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (permissionsToRequest.isNotEmpty()) {
            appPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearUiMessage()
        }
    }

    val navItems = remember {
        listOf(
            NavItem("Kalkulator", Icons.Filled.Calculate, Icons.Outlined.Calculate, "nav_calculator"),
            NavItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
            NavItem("Riwayat", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
            NavItem("Master QRIS", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "nav_upload"),
            NavItem("Printer", Icons.Filled.Print, Icons.Outlined.Print, "nav_printer"),
            NavItem("Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    var selectedNavIndex by remember { mutableIntStateOf(0) }

    MyApplicationTheme(darkTheme = isDark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedNavIndex) {
                                0 -> "Kalkulator QRIS"
                                1 -> "Dashboard Bisnis"
                                2 -> "Riwayat Transaksi"
                                3 -> "Master QRIS Toko"
                                4 -> "Printer Bluetooth"
                                else -> "Pengaturan & Info"
                            },
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.openGuide() }) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Panduan Interaktif",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    navItems.forEachIndexed { index, item ->
                        val selected = selectedNavIndex == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedNavIndex = index },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedNavIndex) {
                    0 -> CalculatorScreen(
                        viewModel = viewModel,
                        onNavigateToUpload = { selectedNavIndex = 3 }
                    )
                    1 -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCalculator = { selectedNavIndex = 0 },
                        onNavigateToUpload = { selectedNavIndex = 3 },
                        onNavigateToHistory = { selectedNavIndex = 2 },
                        onNavigateToPrinter = { selectedNavIndex = 4 },
                        onOpenGuide = { viewModel.openGuide() }
                    )
                    2 -> HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToCalculator = { selectedNavIndex = 0 }
                    )
                    3 -> MerchantProfileScreen(
                        viewModel = viewModel,
                        onSavedNavigate = { selectedNavIndex = 0 }
                    )
                    4 -> PrinterScreen(
                        viewModel = viewModel
                    )
                    5 -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenGuide = { viewModel.openGuide() }
                    )
                }
            }

            // Interactive Onboarding / User Guide
            if (showGuide) {
                InteractiveGuideDialog(
                    onDismiss = { viewModel.dismissGuide() }
                )
            }
        }
    }
}
