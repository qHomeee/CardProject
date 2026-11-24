package com.example.cartproject.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
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
) {
    val palette = ZapomniTheme.colors
    val type = MaterialTheme.typography
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
                HorizontalDivider(color = palette.border)
                Text(
                    stringResource(R.string.study_answer),
                    color = palette.onPrimarySoft,
                    style = type.labelLarge,
                )
                Text(answer, style = type.bodyLarge)
            }
        }
    }
}
