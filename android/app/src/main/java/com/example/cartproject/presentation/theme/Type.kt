package com.example.cartproject.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private fun roboto(size: Int, lineHeight: Int, tracking: Double, weight: FontWeight) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

private val display = roboto(32, 40, -0.5, FontWeight.Bold)
private val title = roboto(24, 32, 0.0, FontWeight.Bold)
private val heading = roboto(20, 28, 0.0, FontWeight.Medium)
private val body = roboto(16, 24, 0.25, FontWeight.Normal)
private val label = roboto(14, 20, 0.1, FontWeight.Medium)
private val caption = roboto(12, 16, 0.2, FontWeight.Normal)

internal val ZapomniTypography = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = display,
    headlineLarge = title,
    headlineMedium = title,
    headlineSmall = title,
    titleLarge = heading,
    titleMedium = heading,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = label,
    labelSmall = caption,
)
