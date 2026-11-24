package com.example.cartproject.domain.decks

/** Локальный набор карточек, доступный на устройстве. */
data class Deck(
    val id: Long,
    val title: String,
    val description: String,
)
