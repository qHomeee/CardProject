package com.example.cartproject.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Тело ответа со списком наборов. */
@Serializable
internal data class CatalogListDto(val items: List<CatalogSummaryDto>)

/** Набор в выдаче каталога. */
@Serializable
internal data class CatalogSummaryDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("card_count") val cardCount: Int,
)

/** Набор и все его карточки в одном ответе. */
@Serializable
internal data class CatalogDetailDto(
    val id: String,
    val title: String,
    val description: String,
    val cards: List<CatalogCardDto>,
)

/** Карточка в ответе каталога. */
@Serializable
internal data class CatalogCardDto(val id: String, val question: String, val answer: String)

/** Общий контейнер ошибки FastAPI. */
@Serializable
internal data class CatalogErrorResponseDto(val error: CatalogErrorDto)

/** Код ошибки, необходимый для сопоставления с доменным результатом. */
@Serializable
internal data class CatalogErrorDto(val code: String)
