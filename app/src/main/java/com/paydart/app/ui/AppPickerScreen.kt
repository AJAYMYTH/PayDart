package com.paydart.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.paydart.app.ui.theme.DartBorder
import com.paydart.app.ui.theme.DartCyan
import com.paydart.app.ui.theme.DartDarkBackground
import com.paydart.app.ui.theme.DartSurface
import com.paydart.app.ui.theme.DartSurfaceVariant
import com.paydart.app.ui.theme.DartTextPrimary
import com.paydart.app.ui.theme.DartTextSecondary
import com.paydart.app.upi.InstalledUpiApp
import com.paydart.app.upi.UpiAppRegistry
import com.paydart.app.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    viewModel: ScannerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    val installedApps = remember {
        UpiAppRegistry.getInstalledUpiApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Preferred UPI App",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DartTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DartTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DartDarkBackground
                )
            )
        },
        containerColor = DartDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Select which app should automatically handle scanned UPI codes, or choose 'System Chooser' to decide on each scan.",
                    fontSize = 13.sp,
                    color = DartTextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Option 1: System Chooser (Default)
            item {
                val isSelected = settings.preferredPackage == null
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DartSurface),
                    border = BorderStroke(1.dp, if (isSelected) DartCyan else DartBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable {
                            viewModel.setPreferredPackage(null)
                            onBack()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DartSurfaceVariant,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = DartCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Always ask (System Chooser)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = DartTextPrimary
                            )
                            Text(
                                text = "Android dialog with all installed UPI apps",
                                fontSize = 12.sp,
                                color = DartTextSecondary
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                viewModel.setPreferredPackage(null)
                                onBack()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = DartCyan,
                                unselectedColor = DartTextSecondary
                            )
                        )
                    }
                }
            }

            // Installed UPI Apps
            if (installedApps.isNotEmpty()) {
                item {
                    Text(
                        text = "INSTALLED UPI APPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DartCyan,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 12.dp, start = 4.dp)
                    )
                }

                items(installedApps) { app ->
                    val isSelected = settings.preferredPackage == app.packageName
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DartSurface),
                        border = BorderStroke(1.dp, if (isSelected) DartCyan else DartBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable {
                                viewModel.setPreferredPackage(app.packageName)
                                onBack()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                val bitmap = remember(app.packageName) {
                                    runCatching { app.icon.toBitmap(96, 96) }.getOrNull()
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = app.appName,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    AppFallbackIcon()
                                }
                            } else {
                                AppFallbackIcon()
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = app.appName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = DartTextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setPreferredPackage(app.packageName)
                                    onBack()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DartCyan,
                                    unselectedColor = DartTextSecondary
                                )
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DartSurface),
                        border = BorderStroke(1.dp, DartBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No dedicated UPI apps detected",
                                fontWeight = FontWeight.Bold,
                                color = DartTextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PayDart will hand off payment intents to Android's system chooser.",
                                color = DartTextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppFallbackIcon() {
    Surface(
        shape = CircleShape,
        color = DartSurfaceVariant,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = DartTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
