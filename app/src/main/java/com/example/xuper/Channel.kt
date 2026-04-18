package com.example.xuper

import java.util.UUID

data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val category: String = "Otros",
    val sourceListName: String = ""
)

data class M3UList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String
)

val sampleChannels = listOf<Channel>()
