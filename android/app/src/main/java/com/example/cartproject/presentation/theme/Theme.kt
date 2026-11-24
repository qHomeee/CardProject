package com.example.cartproject.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalZapomniColors = staticCompositionLocalOf { LightColors }

/** Предоставляет цветовые роли, которые выходят за рамки Material 3. */
internal object ZapomniTheme {
    val colors: ZapomniColors
        @Composable get() = LocalZapomniColors.current
}

/** Применяет палитру, типографику и формы из дизайн-системы. */
@Composable
internal fun ZapomniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val baseScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val scheme = baseScheme.copy(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primarySoft,
        onPrimaryContainer = colors.onPrimarySoft,
        secondary = colors.primary,
        onSecondary = colors.onPrimary,
        secondaryContainer = colors.primarySoft,
        onSecondaryContainer = colors.onPrimarySoft,
        tertiary = colors.success,
        onTertiary = colors.successSoft,
        tertiaryContainer = colors.successSoft,
        onTertiaryContainer = colors.success,
        background = colors.background,
        onBackground = colors.text,
        surface = colors.surface,
        onSurface = colors.text,
        surfaceVariant = colors.surfaceRaised,
        onSurfaceVariant = colors.textMuted,
        surfaceTint = colors.primary,
        surfaceContainerLowest = colors.background,
        surfaceContainerLow = colors.surface,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceRaised,
        surfaceContainerHighest = colors.surfaceRaised,
        error = colors.danger,
        onError = colors.dangerSoft,
        errorContainer = colors.dangerSoft,
        onErrorContainer = colors.danger,
        outline = colors.outline,
        outlineVariant = colors.border,
    )

    CompositionLocalProvider(LocalZapomniColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ZapomniTypography,
            shapes = ZapomniShapes,
            content = content,
        )
    }
}
