package com.paydart.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DartCyan,
    onPrimary = DartDarkBackground,
    primaryContainer = DartSurfaceVariant,
    onPrimaryContainer = DartCyan,
    secondary = DartElectricViolet,
    onSecondary = DartTextPrimary,
    background = DartDarkBackground,
    onBackground = DartTextPrimary,
    surface = DartSurface,
    onSurface = DartTextPrimary,
    surfaceVariant = DartSurfaceVariant,
    onSurfaceVariant = DartTextSecondary,
    outline = DartBorder,
    error = DartError,
    onError = DartDarkBackground
)

@Composable
fun PayDartTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = colorScheme.background.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = colorScheme.background.toArgb()
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
