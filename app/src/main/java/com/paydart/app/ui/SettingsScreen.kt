package com.paydart.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paydart.app.settings.CameraResolution
import com.paydart.app.settings.ScanBehavior
import com.paydart.app.ui.theme.DartBorder
import com.paydart.app.ui.theme.DartCyan
import com.paydart.app.ui.theme.DartDarkBackground
import com.paydart.app.ui.theme.DartSurface
import com.paydart.app.ui.theme.DartSurfaceVariant
import com.paydart.app.ui.theme.DartTextPrimary
import com.paydart.app.ui.theme.DartTextSecondary
import com.paydart.app.upi.UpiAppRegistry
import com.paydart.app.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScannerViewModel,
    onBack: () -> Unit,
    onPickApp: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current
    val supportedResolutions = remember {
        viewModel.cameraManager.getSupportedResolutions()
    }

    val preferredAppName = remember(settings.preferredPackage) {
        UpiAppRegistry.getAppName(context, settings.preferredPackage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Scan Behavior
            SectionHeader(icon = Icons.Default.Speed, title = "Scan Behavior")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DartSurface),
                border = BorderStroke(1.dp, DartBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    ScanBehavior.entries.forEachIndexed { index, behavior ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setScanBehavior(behavior) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.scanBehavior == behavior,
                                onClick = { viewModel.setScanBehavior(behavior) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DartCyan,
                                    unselectedColor = DartTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = behavior.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DartTextPrimary
                                )
                                Text(
                                    text = behavior.description,
                                    fontSize = 13.sp,
                                    color = DartTextSecondary
                                )
                            }
                        }

                        if (index < ScanBehavior.entries.size - 1) {
                            HorizontalDivider(
                                color = DartBorder,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = DartBorder,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Vibration switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = DartTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vibration on Scan",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DartTextPrimary
                                )
                                Text(
                                    text = "Instant tactile confirmation tick",
                                    fontSize = 13.sp,
                                    color = DartTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.vibrationEnabled,
                            onCheckedChange = { viewModel.setVibrationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DartDarkBackground,
                                checkedTrackColor = DartCyan,
                                uncheckedTrackColor = DartSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Preferred UPI App
            SectionHeader(icon = Icons.Default.Payment, title = "UPI Handoff")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DartSurface),
                border = BorderStroke(1.dp, DartBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickApp() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preferred Payment App",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = DartTextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = preferredAppName,
                            fontSize = 14.sp,
                            color = DartCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select",
                        tint = DartTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Camera Pipeline
            SectionHeader(icon = Icons.Default.Videocam, title = "Camera Resolution")
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DartSurface),
                border = BorderStroke(1.dp, DartBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    supportedResolutions.forEachIndexed { index, res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCameraResolution(res) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.cameraResolution == res,
                                onClick = { viewModel.setCameraResolution(res) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DartCyan,
                                    unselectedColor = DartTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = res.title,
                                fontSize = 15.sp,
                                fontWeight = if (settings.cameraResolution == res) FontWeight.Bold else FontWeight.Normal,
                                color = DartTextPrimary
                            )
                        }

                        if (index < supportedResolutions.size - 1) {
                            HorizontalDivider(
                                color = DartBorder,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PayDart v1.0.0",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DartTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lightning Fast · Zero Bloat · Fully Private",
                    fontSize = 12.sp,
                    color = DartTextSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DartCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DartCyan,
            letterSpacing = 0.5.sp
        )
    }
}
