package com.example.cartproject.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Цветовые роли дизайн-системы «Запомни». */
@Immutable
internal data class ZapomniColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val onPrimarySoft: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val disabled: Color,
    val onDisabled: Color,
)

internal val LightColors = ZapomniColors(
    background = Color(0xFFF8F7FC),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF1EEF9),
    text = Color(0xFF211D32),
    textMuted = Color(0xFF625A75),
    border = Color(0xFFDFD9EB),
    outline = Color(0xFF777185),
    primary = Color(0xFF5142A2),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFEBE6FF),
    onPrimarySoft = Color(0xFF3D3085),
    success = Color(0xFF12593E),
    successSoft = Color(0xFFE3F6EB),
    danger = Color(0xFFA42A46),
    dangerSoft = Color(0xFFFDE8ED),
    warning = Color(0xFF654600),
    warningSoft = Color(0xFFFFF0C7),
    disabled = Color(0xFFEAE6F1),
    onDisabled = Color(0xFF655D76),
)

internal val DarkColors = ZapomniColors(
    background = Color(0xFF14131C),
    surface = Color(0xFF1E1C2A),
    surfaceRaised = Color(0xFF292636),
    text = Color(0xFFF3EFFA),
    textMuted = Color(0xFFBFB9CF),
    border = Color(0xFF423B55),
    outline = Color(0xFF9188A6),
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF241755),
    primarySoft = Color(0xFF382C63),
    onPrimarySoft = Color(0xFFE8E1FF),
    success = Color(0xFFA2E8C5),
    successSoft = Color(0xFF18392E),
    danger = Color(0xFFFFB1BF),
    dangerSoft = Color(0xFF432435),
    warning = Color(0xFFFFD88A),
    warningSoft = Color(0xFF372B14),
    disabled = Color(0xFF302C40),
    onDisabled = Color(0xFFB5ADC6),
)
