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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.viewmodels.SettingsViewModel

object AppFont {
    val Rubik = FontFamily(
        Font(R.font.rubik_black, FontWeight.Black),
        Font(R.font.rubik_blackitalic, FontWeight.Black, FontStyle.Italic),
        Font(R.font.rubik_extrabold, FontWeight.ExtraBold),
        Font(R.font.rubik_extrabold, FontWeight.ExtraBold),
        Font(R.font.rubik_bold, FontWeight.Bold),
        Font(R.font.rubik_bolditalic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.rubik_semibold, FontWeight.SemiBold),
        Font(
            R.font.rubik_semibolditalic,
            FontWeight.SemiBold,
            FontStyle.Italic
        ),
        Font(R.font.rubik_medium, FontWeight.Medium),
        Font(R.font.rubik_mediumitalic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.rubik_regular, FontWeight.Normal),
        Font(R.font.rubik_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.rubik_light, FontWeight.Light),
        Font(R.font.rubik_lightitalic, FontWeight.Light, FontStyle.Italic),
    )
    val Metropolis = FontFamily(
        Font(R.font.metropolis_black, FontWeight.Black),
        Font(
            R.font.metropolis_blackitalic,
            FontWeight.Black,
            FontStyle.Italic
        ),
        Font(R.font.metropolis_extrabold, FontWeight.ExtraBold),
        Font(
            R.font.metropolis_extrabold,
            FontWeight.ExtraBold,
            FontStyle.Italic
        ),
        Font(R.font.metropolis_semibold, FontWeight.SemiBold),
        Font(
            R.font.metropolis_semibolditalic,
            FontWeight.SemiBold,
            FontStyle.Italic
        ),
        Font(R.font.metropolis_medium, FontWeight.Medium),
        Font(
            R.font.metropolis_mediumitalic,
            FontWeight.Medium,
            FontStyle.Italic
        ),
        Font(R.font.metropolis_regular, FontWeight.Normal),
        Font(
            R.font.metropolis_regularitalic,
            FontWeight.Normal,
            FontStyle.Italic
        ),
        Font(R.font.metropolis_light, FontWeight.Light),
        Font(
            R.font.metropolis_lightitalic,
            FontWeight.Light,
            FontStyle.Italic
        ),
        Font(R.font.metropolis_thin, FontWeight.Thin),
        Font(R.font.metropolis_thinitalic, FontWeight.Thin, FontStyle.Italic),
        Font(R.font.metropolis_extralight, FontWeight.ExtraLight),
        Font(
            R.font.metropolis_extralightitalic,
            FontWeight.ExtraLight,
            FontStyle.Italic
        ),
    )
}

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