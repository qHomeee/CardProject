package com.example.cartproject.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Карточка занятия с вопросом и необязательным открытым ответом. */
@Composable
internal fun StudyCard(
    question: String,
    position: Int,
    total: Int,
    modifier: Modifier = Modifier,
    answer: String? = null,
    hint: String? = null,
) {
    val palette = ZapomniTheme.colors
    val type = MaterialTheme.typography
    val answerHeading = remember { BringIntoViewRequester() }
    var answerBroughtIntoView by rememberSaveable(position, total) { mutableStateOf(false) }
    LaunchedEffect(answer != null) {
        if (answer == null) {
            answerBroughtIntoView = false
        } else if (!answerBroughtIntoView) {
            withFrameNanos {}
            answerHeading.bringIntoView()
            answerBroughtIntoView = true
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = palette.surface,
        contentColor = palette.text,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(
            modifier = Modifier.padding(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
        ) {
            Text(
                stringResource(R.string.study_card_position, position, total),
                style = type.bodySmall,
                color = palette.textMuted,
            )
            Text(question, modifier = Modifier.semantics { heading() }, style = type.headlineSmall)
            if (answer != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().semantics {
                        liveRegion =
                            LiveRegionMode.Polite
                    },
                    color = palette.primarySoft,
                    contentColor = palette.onPrimarySoft,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(
                        modifier = Modifier.padding(ZapomniSize.space12),
                        verticalArrangement = Arrangement.spacedBy(ZapomniSize.space4),
                    ) {
                        Text(
                            stringResource(R.string.study_answer),
                            style = type.labelLarge,
                            modifier = Modifier.bringIntoViewRequester(answerHeading).semantics {
                                heading()
                            },
                        )
                        Text(answer, style = type.bodyLarge)
                    }
                }
            } else if (hint != null) {
                Text(hint, color = palette.textMuted, style = type.bodyLarge)
            }
        }
    }
}
