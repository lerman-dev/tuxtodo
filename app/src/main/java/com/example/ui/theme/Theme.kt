package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val TuxDarkColorScheme = darkColorScheme(
    primary = TuxDarkPrimary,
    onPrimary = TuxDarkOnPrimary,
    primaryContainer = TuxDarkPrimaryContainer,
    onPrimaryContainer = TuxDarkOnPrimaryContainer,
    secondary = TuxDarkSecondary,
    onSecondary = TuxDarkOnSecondary,
    secondaryContainer = TuxDarkSecondaryContainer,
    onSecondaryContainer = TuxDarkOnSecondaryContainer,
    tertiary = TuxDarkTertiary,
    onTertiary = TuxDarkOnTertiary,
    background = TuxDarkBackground,
    onBackground = TuxDarkOnBackground,
    surface = TuxDarkSurface,
    onSurface = TuxDarkOnSurface,
    surfaceVariant = TuxDarkSurfaceVariant,
    onSurfaceVariant = TuxDarkOnSurfaceVariant
)

private val TuxLightColorScheme = lightColorScheme(
    primary = TuxLightPrimary,
    onPrimary = TuxLightOnPrimary,
    primaryContainer = TuxLightPrimaryContainer,
    onPrimaryContainer = TuxLightOnPrimaryContainer,
    secondary = TuxLightSecondary,
    onSecondary = TuxLightOnSecondary,
    secondaryContainer = TuxLightSecondaryContainer,
    onSecondaryContainer = TuxLightOnSecondaryContainer,
    tertiary = TuxLightTertiary,
    onTertiary = TuxLightOnTertiary,
    background = TuxLightBackground,
    onBackground = TuxLightOnBackground,
    surface = TuxLightSurface,
    onSurface = TuxLightOnSurface,
    surfaceVariant = TuxLightSurfaceVariant,
    onSurfaceVariant = TuxLightOnSurfaceVariant
)

@Composable
fun TuxTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (Material You)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TuxDarkColorScheme
        else -> TuxLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
