package com.paydart.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paydart.app.ui.AppPickerScreen
import com.paydart.app.ui.ConfirmPaymentSheet
import com.paydart.app.ui.ScannerScreen
import com.paydart.app.ui.SettingsScreen
import com.paydart.app.ui.theme.PayDartTheme
import com.paydart.app.viewmodel.ScannerUiState
import com.paydart.app.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.attachActivity(this)

        setContent {
            PayDartTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()
                val settings by viewModel.settingsState.collectAsState()

                // Permission launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    viewModel.onPermissionResult(isGranted)
                }

                // Check camera permission on startup
                LaunchedEffect(Unit) {
                    val hasCamPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    viewModel.onPermissionResult(hasCamPermission)
                    if (!hasCamPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "scanner"
                ) {
                    composable("scanner") {
                        ScannerScreen(
                            viewModel = viewModel,
                            onOpenSettings = { navController.navigate("settings") },
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onPickApp = { navController.navigate("app_picker") }
                        )

                        // Modal confirmation sheet when ConfirmPending
                        val confirmState = uiState
                        if (confirmState is ScannerUiState.ConfirmPending) {
                            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            ConfirmPaymentSheet(
                                request = confirmState.request,
                                preferredPackage = settings.preferredPackage,
                                sheetState = sheetState,
                                onConfirm = {
                                    viewModel.confirmPayment(confirmState.request)
                                },
                                onDismiss = {
                                    viewModel.cancelPayment()
                                }
                            )
                        }
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onPickApp = { navController.navigate("app_picker") }
                        )
                    }

                    composable("app_picker") {
                        AppPickerScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.attachActivity(this)
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission && viewModel.uiState.value is ScannerUiState.Idle) {
            viewModel.resumeScanning()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.detachActivity()
    }
}
