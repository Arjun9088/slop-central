package com.expensetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A6B3C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA4F5B7),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF506352),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8D2),
    onSecondaryContainer = Color(0xFF0E1F12),
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE0E4DA),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF89D99D),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF00522C),
    onPrimaryContainer = Color(0xFFA4F5B7),
    secondary = Color(0xFFB7CCB7),
    onSecondary = Color(0xFF233426),
    secondaryContainer = Color(0xFF394B3C),
    onSecondaryContainer = Color(0xFFD3E8D2),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BD),
    outline = Color(0xFF8D9388),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
