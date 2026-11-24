package com.example.cartproject.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Как набор появился в приложении. */
internal enum class DeckCardOrigin(@get:StringRes val label: Int) {
    Catalog(R.string.deck_origin_catalog),
    Owned(R.string.deck_origin_owned),
    Downloaded(R.string.deck_origin_downloaded),
}

/** Название набора, его происхождение и дополнительная строка. */
@Composable
internal fun DeckCard(
    title: String,
    origin: DeckCardOrigin?,
    modifier: Modifier = Modifier,
    details: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val palette = ZapomniTheme.colors
    val shape = MaterialTheme.shapes.medium
    val downloaded = origin == DeckCardOrigin.Downloaded

    Surface(
        modifier = modifier.fillMaxWidth().clip(shape).then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        shape = shape,
        color = palette.surface,
        contentColor = palette.text,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(
            modifier = Modifier.padding(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space8),
        ) {
            if (origin != null) {
                Text(
                    text = stringResource(origin.label),
                    modifier = Modifier
                        .background(
                            if (downloaded) palette.successSoft else palette.primarySoft,
                            ZapomniSize.pill,
                        )
                        .padding(horizontal = ZapomniSize.space12, vertical = ZapomniSize.space4),
                    color = if (downloaded) palette.success else palette.onPrimarySoft,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!details.isNullOrBlank()) {
                Text(
                    details,
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
