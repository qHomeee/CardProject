package com.example.cartproject.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Кнопка основного или дополнительного действия с блокировкой во время операции. */
@Composable
internal fun ZapomniButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
    danger: Boolean = false,
) {
    val palette = ZapomniTheme.colors
    val (background, foreground) = when {
        danger -> palette.dangerSoft to palette.danger
        secondary -> palette.surface to palette.onPrimarySoft
        else -> palette.primary to palette.onPrimary
    }
    val border = if (secondary) {
        BorderStroke(1.dp, if (enabled && !loading) palette.outline else palette.border)
    } else {
        null
    }

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minWidth = ZapomniSize.touchTarget,
            minHeight = ZapomniSize.touchTarget,
        ),
        enabled = enabled && !loading,
        shape = ZapomniSize.pill,
        border = border,
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = foreground,
            disabledContainerColor = if (loading) background else palette.disabled,
            disabledContentColor = if (loading) foreground else palette.onDisabled,
        ),
        contentPadding = PaddingValues(
            horizontal = ZapomniSize.space16,
            vertical = ZapomniSize.space12,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ZapomniSize.space16),
                color = LocalContentColor.current,
                trackColor = Color.Transparent,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(ZapomniSize.space8))
        }
        Text(label, textAlign = TextAlign.Center)
    }
}
