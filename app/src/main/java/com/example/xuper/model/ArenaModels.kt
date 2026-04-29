package com.example.xuper.model

data class ArenaEvent(
    val time: String,
    val sport: String,
    val title: String,
    val competition: String,
    val channels: List<String>,
)

data class ArenaStream(
    val name: String,
    val url: String,
)
