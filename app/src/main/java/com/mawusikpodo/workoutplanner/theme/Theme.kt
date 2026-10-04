package com.mawusikpodo.workoutplanner.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** The app is dark-only by design (see the mockups), so there is no light or dynamic scheme. */
private val ColorScheme =
  darkColorScheme(
    primary = Volt,
    onPrimary = Ground,
    secondary = Volt,
    onSecondary = Ground,
    background = Ground,
    onBackground = Ink,
    surface = Ground,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Ground,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface2,
    secondaryContainer = Surface2,
    onSecondaryContainer = Ink,
    outline = Outline,
    outlineVariant = Surface2,
  )

@Composable
fun WorkoutPlannerTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = ColorScheme, typography = Typography, content = content)
}
