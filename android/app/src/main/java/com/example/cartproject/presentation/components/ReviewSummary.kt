package com.example.cartproject.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.cartproject.R
import com.example.cartproject.domain.study.StudySummary
import java.util.Locale

private val Russian = Locale.forLanguageTag("ru-RU")

/** Показывает количество оценок и долю «Вспомнил» для непустой сводки. */
@Composable
internal fun ReviewSummary(summary: StudySummary, title: String) {
    val percent = summary.rememberedPercent?.let { String.format(Russian, "%.1f", it) }
    StatusMessage(
        kind = StatusKind.Success,
        title = title,
        message = percent?.let { stringResource(R.string.study_percent, it) },
    )
    Text(
        stringResource(R.string.study_review_total, summary.total),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        stringResource(R.string.study_remembered_result, summary.remembered, summary.total),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        stringResource(R.string.study_review_result, summary.needsReview, summary.total),
        style = MaterialTheme.typography.bodyLarge,
    )
}
