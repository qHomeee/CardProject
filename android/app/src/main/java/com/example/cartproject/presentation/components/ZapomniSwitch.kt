package com.example.cartproject.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Настройка с переключением по нажатию на любую часть строки. */
@Composable
internal fun ZapomniSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = ZapomniTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = ZapomniSize.touchTarget)
            .toggleable(
                checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = ZapomniSize.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (enabled) palette.text else palette.onDisabled,
            style = MaterialTheme.typography.labelLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onPrimary,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.textMuted,
                uncheckedTrackColor = palette.surfaceRaised,
                uncheckedBorderColor = palette.outline,
                disabledCheckedThumbColor = palette.onDisabled,
                disabledCheckedTrackColor = palette.disabled,
                disabledUncheckedThumbColor = palette.onDisabled,
                disabledUncheckedTrackColor = palette.disabled,
                disabledUncheckedBorderColor = palette.border,
            ),
        )
    }
}
