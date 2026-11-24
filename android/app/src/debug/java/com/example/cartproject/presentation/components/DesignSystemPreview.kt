package com.example.cartproject.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.cartproject.R
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Примеры компонентов для предпросмотра в Android Studio. */
@Preview(name = "Светлая", widthDp = 360, heightDp = 820, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Тёмная", widthDp = 360, heightDp = 820, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun DesignSystemPreview() {
    ZapomniTheme { DesignSystemExamples() }
}

/** Показывает компоненты на данных из дизайн-системы. */
@Composable
internal fun DesignSystemExamples() {
    var selectedTab by remember { mutableStateOf(ZapomniTab.Decks) }
    var gestures by remember { mutableStateOf(false) }
    var deckTitle by remember { mutableStateOf("") }
    var answerVisible by remember { mutableStateOf(false) }
    val actionsPerRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2
    Surface(color = ZapomniTheme.colors.background) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
        ) {
            ZapomniNavigationBar(selectedTab, { selectedTab = it }, Modifier.fillMaxWidth())
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ZapomniSize.space8),
                verticalArrangement = Arrangement.spacedBy(ZapomniSize.space8),
                maxItemsInEachRow = actionsPerRow,
            ) {
                ZapomniButton(
                    label = stringResource(
                        if (answerVisible) {
                            R.string.preview_hide_answer
                        } else {
                            R.string.preview_show_answer
                        },
                    ),
                    onClick = { answerVisible = !answerVisible },
                    modifier = Modifier.weight(1f),
                )
                ZapomniButton(
                    label = stringResource(R.string.preview_saving),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    loading = true,
                )
            }
            ZapomniTextField(
                value = deckTitle,
                onValueChange = { deckTitle = it },
                label = stringResource(R.string.preview_deck_label),
                placeholder = stringResource(R.string.preview_deck_placeholder),
            )
            ZapomniSwitch(
                label = stringResource(R.string.preview_gesture_switch),
                checked = gestures,
                onCheckedChange = { gestures = it },
            )
            DeckCard(
                title = stringResource(R.string.preview_deck_title),
                origin = DeckCardOrigin.Downloaded,
                details = stringResource(R.string.preview_deck_count),
            )
            StudyCard(
                question = stringResource(R.string.preview_question),
                position = 12,
                total = 12,
                answer = if (answerVisible) stringResource(R.string.preview_answer) else null,
            )
            StatusMessage(
                kind = StatusKind.Empty,
                title = stringResource(R.string.my_decks_empty),
                message = stringResource(R.string.preview_empty_message),
            )
        }
    }
}
