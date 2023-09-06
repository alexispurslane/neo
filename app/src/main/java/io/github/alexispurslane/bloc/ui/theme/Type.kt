package io.github.alexispurslane.bloc.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
private val defaultTypography = Typography()
val Typography = Typography(
    // Huge hero jumbotron text
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = AppFont.Metropolis,
        fontWeight = FontWeight.Black
    ),

    // Used to separate main sections of something, for short text only
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = AppFont.Metropolis,
        fontWeight = FontWeight.Black,
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = AppFont.Metropolis,
        fontWeight = FontWeight.Black,
        color = Color.DarkGray,
        fontSize = 15.sp
    ),

    // Also short text only, separates secondary sections
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = AppFont.Metropolis,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = AppFont.Metropolis,
        fontWeight = FontWeight.SemiBold,
    ),

    // Main body font
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = AppFont.Rubik,
        lineHeight = 1.3.em
    ),

    // Used to add context or introduce things
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = AppFont.Metropolis,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp
    ),

    labelMedium = defaultTypography.labelLarge.copy(
        fontFamily = AppFont.Rubik,
        fontWeight = FontWeight.SemiBold,
        color = Color.Gray
    ),
)