package com.articlevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Light theme colors
private val LightPrimary = Color(0xFF1A1A1A)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightBackground = Color(0xFFFFFFFF)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnBackground = Color(0xFF1A1A1A)
private val LightOnSurface = Color(0xFF1A1A1A)
private val LightSurfaceVariant = Color(0xFFF5F5F5)
private val LightOnSurfaceVariant = Color(0xFF9E9E9E)
private val LightOutline = Color(0xFFE0E0E0)

// Dark theme colors
private val DarkPrimary = Color(0xFFFFFFFF)
private val DarkOnPrimary = Color(0xFF1A1A1A)
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkOnBackground = Color(0xFFE0E0E0)
private val DarkOnSurface = Color(0xFFE0E0E0)
private val DarkSurfaceVariant = Color(0xFF2C2C2C)
private val DarkOnSurfaceVariant = Color(0xFF9E9E9E)
private val DarkOutline = Color(0xFF3E3E3E)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFE8E8E8),
    onPrimaryContainer = LightPrimary,
    secondary = LightOnSurfaceVariant,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightPrimary,
    tertiary = LightOnSurfaceVariant,
    onTertiary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    inverseSurface = LightPrimary,
    inverseOnSurface = LightBackground,
    error = Color(0xFFBA1A1A),
    onError = LightOnPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF3E3E3E),
    onPrimaryContainer = DarkPrimary,
    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkPrimary,
    tertiary = DarkOnSurfaceVariant,
    onTertiary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    inverseSurface = DarkPrimary,
    inverseOnSurface = DarkBackground,
    error = Color(0xFFFFB4AB),
    onError = DarkOnPrimary,
)

private val InstapaperTypography = Typography(
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
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun ArticleVaultTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = InstapaperTypography,
        content = content
    )
}
