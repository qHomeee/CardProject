package com.example.cartproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Поле с постоянной подписью, контуром фокуса и сообщением об ошибке. */
@Composable
internal fun ZapomniTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val palette = ZapomniTheme.colors
    val type = MaterialTheme.typography
    val shape = MaterialTheme.shapes.small
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val border = when {
        !enabled -> palette.border
        errorMessage != null -> palette.danger
        focused -> palette.primary
        else -> palette.outline
    }
    val textColor = if (enabled) palette.text else palette.onDisabled

    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZapomniSize.space4)) {
        Text(
            text = label,
            modifier = Modifier.clearAndSetSemantics {},
            color = if (errorMessage == null) textColor else palette.danger,
            style = type.labelLarge,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                errorMessage?.let { error(it) }
            },
            enabled = enabled,
            singleLine = singleLine,
            textStyle = type.bodyLarge.copy(color = textColor),
            cursorBrush = SolidColor(palette.primary),
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ZapomniSize.touchTarget)
                        .background(if (enabled) palette.surface else palette.disabled, shape)
                        .border(if (focused) 2.dp else 1.dp, border, shape)
                        .padding(horizontal = ZapomniSize.space12, vertical = ZapomniSize.space12),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ZapomniSize.space8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        leadingIcon?.invoke()
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(placeholder, color = palette.textMuted, style = type.bodyLarge)
                            }
                            inner()
                        }
                    }
                }
            },
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = palette.danger,
                style = type.bodySmall,
            )
        }
    }
}
