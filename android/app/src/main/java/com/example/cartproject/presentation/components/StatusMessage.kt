package com.example.cartproject.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Виды сообщений о состоянии экрана. */
internal enum class StatusKind(@get:DrawableRes val icon: Int?) {
    Loading(null),
    Empty(R.drawable.ic_status_empty),
    Offline(R.drawable.ic_status_offline),
    Error(R.drawable.ic_status_error),
    Success(R.drawable.ic_status_success),
    GesturesUnavailable(R.drawable.ic_status_gesture),
}

/** Дополнительное действие в сообщении, например повтор запроса. */
internal data class StatusAction(val label: String, val onClick: () -> Unit)

/** Сообщение с заголовком, пояснением и необязательным действием. */
@Composable
internal fun StatusMessage(
    kind: StatusKind,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: StatusAction? = null,
) {
    val palette = ZapomniTheme.colors
    val (background, foreground) = when (kind) {
        StatusKind.Loading, StatusKind.Empty -> palette.surfaceRaised to palette.text
        StatusKind.Offline -> palette.warningSoft to palette.warning
        StatusKind.Error -> palette.dangerSoft to palette.danger
        StatusKind.Success -> palette.successSoft to palette.success
        StatusKind.GesturesUnavailable -> palette.primarySoft to palette.onPrimarySoft
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = background,
        contentColor = foreground,
    ) {
        Column(
            modifier = Modifier.padding(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space8),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ZapomniSize.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusIcon(kind, foreground)
                Text(title, style = MaterialTheme.typography.labelLarge)
            }
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
            if (action != null) {
                ZapomniButton(
                    label = action.label,
                    onClick = action.onClick,
                    modifier = Modifier.fillMaxWidth(),
                    secondary = true,
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(kind: StatusKind, foreground: Color) {
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        val icon = kind.icon
        if (icon == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(ZapomniSize.space16),
                color = foreground,
                trackColor = Color.Transparent,
                strokeWidth = 2.dp,
            )
        } else {
            val size = if (kind == StatusKind.Empty) {
                Modifier.size(14.8333.dp, 13.1667.dp)
            } else {
                Modifier.size(20.dp)
            }
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = size,
                tint = foreground,
            )
        }
    }
}
