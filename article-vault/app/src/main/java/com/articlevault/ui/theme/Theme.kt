package com.articlevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────
// Brand color: a warm, readable accent that works in both modes
// ──────────────────────────────────────────────
private val AccentLight = Color(0xFFB85C38)
private val AccentDark = Color(0xFFE8A88C)

// ──────────────────────────────────────────────
// Light theme
// ──────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2EDE8),
    onPrimaryContainer = Color(0xFF1A1A1A),

    secondary = Color(0xFF6B6B6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF2A2A2A),

    tertiary = AccentLight,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCEAE0),
    onTertiaryContainer = Color(0xFF5A2A12),

    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F4F1),
    onSurfaceVariant = Color(0xFF6B6B6B),

    surfaceContainer = Color(0xFFF5F4F1),
    surfaceContainerLow = Color(0xFFFAF9F6),
    surfaceContainerHigh = Color(0xFFEFEEE9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE9E7E2),

    outline = Color(0xFFD4D2CD),
    outlineVariant = Color(0xFFE4E2DD),
    inverseSurface = Color(0xFF2A2A2A),
    inverseOnSurface = Color(0xFFF5F4F1),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

// ──────────────────────────────────────────────
// Dark theme
// ──────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8E8E8),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFEEEEEE),

    secondary = Color(0xFFA0A0A0),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2C2C2C),
    onSecondaryContainer = Color(0xFFD4D4D4),

    tertiary = AccentDark,
    onTertiary = Color(0xFF3A1A08),
    tertiaryContainer = Color(0xFF5A2A12),
    onTertiaryContainer = Color(0xFFFCEAE0),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE4E4E4),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE4E4E4),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA8A8A8),

    surfaceContainer = Color(0xFF222222),
    surfaceContainerLow = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerLowest = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF333333),

    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF333333),
    inverseSurface = Color(0xFFE4E4E4),
    inverseOnSurface = Color(0xFF1A1A1A),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
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
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun ArticleVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
