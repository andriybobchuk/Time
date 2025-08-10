package com.andriybobchuk.time.core.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
private val LightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    tertiary = Color.Black,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF5F5F5), // Light gray for cards
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0), // Slightly darker gray for bottom sheets
    onSurfaceVariant = Color.Black,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFCCCCCC),
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = Color.White
)

// Dark theme colors
private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    tertiary = Color.White,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A), // Dark gray for cards
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A), // Slightly lighter gray for bottom sheets
    onSurfaceVariant = Color.White,
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF606060),
    scrim = Color.White.copy(alpha = 0.32f),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Color.Black
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// Extension functions for easy access to theme colors
@Composable
fun ColorScheme.cardBackground(): Color = surface

@Composable
fun ColorScheme.bottomSheetBackground(): Color = surfaceVariant

@Composable
fun ColorScheme.buttonBackground(): Color = primary

@Composable
fun ColorScheme.buttonTextColor(): Color = onPrimary

@Composable
fun ColorScheme.textColor(): Color = onBackground

@Composable
fun ColorScheme.secondaryTextColor(): Color = onSurfaceVariant

@Composable
fun ColorScheme.borderColor(): Color = outline

@Composable
fun ColorScheme.dividerColor(): Color = outlineVariant 