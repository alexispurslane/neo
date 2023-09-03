package io.github.alexispurslane.bloc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.bloc.viewmodels.SettingsViewModel

private val DarkColorScheme = darkColorScheme(
    primary = EngineeringOrange,
    secondary = Color(0xFF0EB3B3),
    surface = Color(0xFF0f0f0f),
    secondaryContainer = Color(0xFF0f0f0f),
    onSecondaryContainer = Color.LightGray,

    background = Color(0xFF000000),
    onPrimary = Color.LightGray
)

private val LightColorScheme = lightColorScheme(
    primary = EngineeringOrange,
    secondary = Color(0xFF0EB3B3),
    surface = Color(0xFF0f0f0f),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color.Black,

    background = Color(0xFFFFFBFE),
)

@Composable
fun BlocTheme(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val settingsState by settingsViewModel.uiState.collectAsState()

    val systemTheme = isSystemInDarkTheme()
    LaunchedEffect(systemTheme) {
        settingsViewModel.toggleDarkTheme(systemTheme)
    }

    val colorScheme = if (settingsState.darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
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
            ).isAppearanceLightStatusBars = !settingsState.darkTheme
            WindowCompat.getInsetsController(
                window,
                view
            ).isAppearanceLightNavigationBars = !settingsState.darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}