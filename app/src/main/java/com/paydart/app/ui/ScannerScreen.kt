package com.paydart.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.paydart.app.ui.theme.DartAmber
import com.paydart.app.ui.theme.DartBorder
import com.paydart.app.ui.theme.DartBorderGlow
import com.paydart.app.ui.theme.DartCyan
import com.paydart.app.ui.theme.DartDarkBackground
import com.paydart.app.ui.theme.DartError
import com.paydart.app.ui.theme.DartNeonMint
import com.paydart.app.ui.theme.DartSurface
import com.paydart.app.ui.theme.DartSurfaceVariant
import com.paydart.app.ui.theme.DartTextPrimary
import com.paydart.app.ui.theme.DartTextSecondary
import com.paydart.app.viewmodel.ScannerUiState
import com.paydart.app.viewmodel.ScannerViewModel

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onPickApp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, settings.cameraResolution) {
        viewModel.cameraManager.startCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            resolution = settings.cameraResolution,
            scanner = viewModel.qrScanner
        )
        onDispose {
            viewModel.cameraManager.stopAnalysis()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanImageUri(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DartDarkBackground)
    ) {
        // 1. Camera Preview
        if (uiState !is ScannerUiState.PermissionRequired) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // High-precision Cyber HUD Viewfinder
            ViewfinderOverlay()
        }

        // 2. Top Header Bar
        TopHeaderBar(
            hasTorch = viewModel.cameraManager.hasTorch(),
            isTorchOn = isTorchOn,
            onToggleTorch = { viewModel.toggleTorch() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        )

        // 3. Bottom Dock & Status Overlays
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Chip / Error Message
            when (val state = uiState) {
                is ScannerUiState.PermissionRequired -> {
                    PermissionRequiredCard(onRequestPermission = onRequestPermission)
                }

                is ScannerUiState.Scanning -> {
                    StatusPill(
                        text = "Align UPI QR code within frame",
                        glowColor = DartCyan
                    )
                }

                is ScannerUiState.NoQrHint -> {
                    StatusPill(
                        text = "💡 Move closer or improve lighting",
                        glowColor = DartAmber
                    )
                }

                is ScannerUiState.Error -> {
                    ErrorBanner(
                        message = state.message,
                        isRecoverable = state.isRecoverable,
                        onDismiss = { viewModel.dismissError() },
                        onPickApp = onPickApp
                    )
                }

                is ScannerUiState.Idle -> {
                    StatusPill(
                        text = "Payment launched · Tap to scan again",
                        glowColor = DartNeonMint,
                        onClick = { viewModel.resumeScanning() }
                    )
                }

                is ScannerUiState.ConfirmPending -> {
                    // Modal bottom sheet handles this
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating Cyber Action Dock
            if (uiState !is ScannerUiState.PermissionRequired) {
                FloatingActionDock(
                    onUploadImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    hasTorch: Boolean,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand capsule with electric cyan badge
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DartSurface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, DartBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡",
                    fontSize = 17.sp,
                    color = DartCyan,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "PayDart",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DartTextPrimary,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Torch button
        if (hasTorch) {
            Surface(
                shape = CircleShape,
                color = if (isTorchOn) DartAmber else DartSurface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, if (isTorchOn) DartAmber else DartBorder),
                shadowElevation = 8.dp
            ) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) DartDarkBackground else DartTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingActionDock(
    onUploadImage: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = DartSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, DartBorder),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Upload Image Pill Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DartSurfaceVariant,
                border = BorderStroke(1.dp, DartBorderGlow),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onUploadImage() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Upload QR",
                        tint = DartCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload QR Image",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = DartTextPrimary
                    )
                }
            }

            // Settings Circular Button
            Surface(
                shape = CircleShape,
                color = DartSurfaceVariant,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onOpenSettings() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = DartTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewfinderOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val boxSize = canvasWidth * 0.72f
        val left = (canvasWidth - boxSize) / 2f
        val top = (canvasHeight - boxSize) / 2.4f
        val right = left + boxSize
        val bottom = top + boxSize
        val cornerRadius = 26.dp.toPx()

        val cutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }

        // Deep darkened mask outside the active HUD window
        clipPath(cutPath, clipOp = ClipOp.Difference) {
            drawRect(color = Color(0xB3080B11))
        }

        // Precision HUD brackets
        val strokeWidth = 4.5.dp.toPx()
        val cornerLength = 34.dp.toPx()
        val cornerColor = DartCyan

        // Top-left corner
        drawLine(
            color = cornerColor,
            start = Offset(left + cornerRadius, top),
            end = Offset(left + cornerRadius + cornerLength, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top + cornerRadius),
            end = Offset(left, top + cornerRadius + cornerLength),
            strokeWidth = strokeWidth
        )

        // Top-right corner
        drawLine(
            color = cornerColor,
            start = Offset(right - cornerRadius, top),
            end = Offset(right - cornerRadius - cornerLength, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, top + cornerRadius),
            end = Offset(right, top + cornerRadius + cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom-left corner
        drawLine(
            color = cornerColor,
            start = Offset(left + cornerRadius, bottom),
            end = Offset(left + cornerRadius + cornerLength, bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, bottom - cornerRadius),
            end = Offset(left, bottom - cornerRadius - cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom-right corner
        drawLine(
            color = cornerColor,
            start = Offset(right - cornerRadius, bottom),
            end = Offset(right - cornerRadius - cornerLength, bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, bottom - cornerRadius),
            end = Offset(right, bottom - cornerRadius - cornerLength),
            strokeWidth = strokeWidth
        )

        // Subtle center crosshairs
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val crosshairSize = 10.dp.toPx()
        val crosshairColor = DartCyan.copy(alpha = 0.35f)

        drawLine(crosshairColor, Offset(centerX - crosshairSize, centerY), Offset(centerX + crosshairSize, centerY), 1.5.dp.toPx())
        drawLine(crosshairColor, Offset(centerX, centerY - crosshairSize), Offset(centerX, centerY + crosshairSize), 1.5.dp.toPx())

        // Animated neon laser sweep
        val laserY = top + (boxSize * laserYRatio)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    DartCyan.copy(alpha = 0.85f),
                    Color.White,
                    DartCyan.copy(alpha = 0.85f),
                    Color.Transparent
                ),
                startX = left + 20.dp.toPx(),
                endX = right - 20.dp.toPx()
            ),
            start = Offset(left + 16.dp.toPx(), laserY),
            end = Offset(right - 16.dp.toPx(), laserY),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    glowColor: Color = DartCyan,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DartSurface.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, DartBorder),
        shadowElevation = 6.dp,
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(glowColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = DartTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Resume",
                    tint = DartCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    isRecoverable: Boolean,
    onDismiss: () -> Unit,
    onPickApp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DartSurface),
        border = BorderStroke(1.dp, DartError.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = DartError,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Invalid QR Code",
                    fontWeight = FontWeight.Bold,
                    color = DartTextPrimary,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = DartTextSecondary,
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isRecoverable) {
                    Button(
                        onClick = onPickApp,
                        colors = ButtonDefaults.buttonColors(containerColor = DartCyan)
                    ) {
                        Text("Select App", color = DartDarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DartSurfaceVariant,
                        contentColor = DartTextPrimary
                    )
                ) {
                    Text("Resume Scanning")
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onRequestPermission: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DartSurface),
        border = BorderStroke(1.dp, DartBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📷 Camera Access Required",
                style = MaterialTheme.typography.titleLarge,
                color = DartTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PayDart needs camera access to detect UPI QR codes with zero friction. It never stores frames, images, or payment details.",
                style = MaterialTheme.typography.bodyMedium,
                color = DartTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = DartCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Grant Camera Permission",
                    color = DartDarkBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
