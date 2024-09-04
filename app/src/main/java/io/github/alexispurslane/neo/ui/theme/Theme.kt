package io.github.alexispurslane.neo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun NeoTheme(
    isLightOverride: Boolean,
    isAMOLED: Boolean,
    content: @Composable () -> Unit,
) {
    val systemThemeIsDark = isSystemInDarkTheme()
    val colorScheme = if (!isLightOverride && systemThemeIsDark) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        dynamicLightColorScheme(LocalContext.current)
    }.let {
        if (isAMOLED) it.copy(
            background = Color.Black
        ) else it
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(
                window,
                view
            ).isAppearanceLightStatusBars = !systemThemeIsDark
            WindowCompat.getInsetsController(
                window,
                view
            ).isAppearanceLightNavigationBars = !systemThemeIsDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}