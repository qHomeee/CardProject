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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
    val density = LocalDensity.current
    val largeText = density.fontScale >= 1.5f
    val labelPadding = if (largeText) 2.dp else ZapomniSize.space4
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
    val minIconWidth = with(density) { 56.dp.roundToPx() }
    val horizontalPadding = with(density) { (labelPadding * 2).roundToPx() }

    Row(
        modifier = modifier
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .selectableGroup()
            .heightIn(min = 74.dp)
            .padding(ZapomniSize.space4),
        verticalAlignment = if (largeText) Alignment.Top else Alignment.CenterVertically,
    ) {
        for (tab in ZapomniTab.entries) {
            val label = stringResource(tab.title)
            val weight = if (largeText) {
                val longestWord = label.split(' ').maxOf {
                    textMeasurer.measure(it, style = labelStyle, softWrap = false).size.width
                }
                (maxOf(longestWord, minIconWidth) + horizontalPadding).toFloat()
            } else {
                1f
            }
            NavigationTab(
                tab = tab,
                label = label,
                labelPadding = labelPadding,
                active = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(weight),
            )
        }
    }
}

@Composable
private fun NavigationTab(
    tab: ZapomniTab,
    label: String,
    labelPadding: Dp,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = ZapomniTheme.colors
    val color = if (active) palette.onPrimarySoft else palette.textMuted
    Column(
        modifier = modifier
            .heightIn(min = ZapomniSize.touchTarget)
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .padding(horizontal = labelPadding, vertical = ZapomniSize.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZapomniSize.space4, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(width = 56.dp, height = 30.dp)
                .background(
                    if (active) palette.primarySoft else Color.Transparent,
                    ZapomniSize.pill,
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
            text = label,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
