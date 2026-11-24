package com.example.cartproject.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Разделы нижней навигации и их ресурсы. */
internal enum class ZapomniTab(@get:StringRes val title: Int, @get:DrawableRes val icon: Int) {
    Catalog(R.string.tab_catalog, R.drawable.ic_nav_catalog),
    Decks(R.string.my_decks_title, R.drawable.ic_nav_decks),
    Settings(R.string.tab_settings, R.drawable.ic_nav_settings),
}

/** Нижняя панель отображает выбранный раздел и передаёт события наружу. */
@Composable
internal fun ZapomniNavigationBar(
    selected: ZapomniTab,
    onSelect: (ZapomniTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = ZapomniTheme.colors
    val shape = MaterialTheme.shapes.medium

    Row(
        modifier = modifier
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .selectableGroup()
            .heightIn(min = 74.dp)
            .padding(ZapomniSize.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (tab in ZapomniTab.entries) {
            val active = tab == selected
            val color = if (active) palette.onPrimarySoft else palette.textMuted
            Column(
                modifier = Modifier.weight(1f)
                    .heightIn(min = ZapomniSize.touchTarget)
                    .selectable(
                        selected = active,
                        role = Role.Tab,
                        onClick = { onSelect(tab) },
                    )
                    .padding(horizontal = ZapomniSize.space4, vertical = ZapomniSize.space4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    ZapomniSize.space4,
                    Alignment.CenterVertically,
                ),
            ) {
                Box(
                    modifier = Modifier.size(width = 56.dp, height = 30.dp).then(
                        if (active) {
                            Modifier.background(palette.primarySoft, ZapomniSize.pill)
                        } else {
                            Modifier
                        },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                        modifier = Modifier.size(ZapomniSize.space24),
                        tint = color,
                    )
                }
                Text(
                    text = stringResource(tab.title),
                    color = color,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
