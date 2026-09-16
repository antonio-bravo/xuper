package com.example.xuper.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface XuperNavKey : NavKey {
    @Serializable data object Tv : XuperNavKey
    @Serializable data object Lists : XuperNavKey
    @Serializable data object Favorites : XuperNavKey
    @Serializable data object Arena : XuperNavKey
    @Serializable data object Config : XuperNavKey
    @Serializable data class Player(val url: String) : XuperNavKey
}
