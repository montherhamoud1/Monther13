package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RestoAmber500,
    onPrimary = RestoNavy900,
    primaryContainer = RestoNavy800,
    onPrimaryContainer = RestoAmber100,
    secondary = RestoBlue400,
    onSecondary = RestoNavy900,
    secondaryContainer = RestoNavy700,
    onSecondaryContainer = RestoBlue100,
    tertiary = RestoEmerald500,
    onTertiary = Color.White,
    background = RestoBgDark,
    onBackground = RestoTextPrimaryDark,
    surface = RestoSurfaceDark,
    onSurface = RestoTextPrimaryDark,
    surfaceVariant = RestoSurfaceVariantDark,
    onSurfaceVariant = RestoTextSecondaryDark,
    outline = RestoNavy600,
    error = RestoRose500,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = RestoNavy900,
    onPrimary = Color.White,
    primaryContainer = RestoBlue100,
    onPrimaryContainer = RestoNavy900,
    secondary = RestoAmber600,
    onSecondary = Color.White,
    secondaryContainer = RestoAmber100,
    onSecondaryContainer = RestoNavy900,
    tertiary = RestoEmerald600,
    onTertiary = Color.White,
    background = RestoBgLight,
    onBackground = RestoTextPrimaryLight,
    surface = RestoSurfaceLight,
    onSurface = RestoTextPrimaryLight,
    surfaceVariant = RestoSurfaceVariantLight,
    onSurfaceVariant = RestoTextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = RestoRose600,
    onError = Color.White
)

@Composable
fun RestoOpsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional corporate theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
