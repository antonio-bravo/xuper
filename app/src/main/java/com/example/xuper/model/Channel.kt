package com.example.xuper.model

import java.util.UUID

data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val category: String = "Otros",
    val sourceListName: String = "",
    val isFavorite: Boolean = false
)

data class M3UList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String
)
