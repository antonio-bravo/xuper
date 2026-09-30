package com.example.xuper.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clase para gestionar layouts adaptativos según el dispositivo
 */
data class AdaptiveLayoutConfig(
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    val gridColumns: Int,
    val playerHeight: Dp,
    val contentPadding: Dp,
    val itemSpacing: Dp,
    val showMiniPlayer: Boolean
) {
    val isTvMode: Boolean get() = isExpanded
    val isTablet: Boolean get() = isMedium
    val isMobile: Boolean get() = isCompact
}

/**
 * Hook para obtener configuración adaptativa
 */
@Composable
fun rememberAdaptiveLayoutConfig(): AdaptiveLayoutConfig {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    return remember(screenWidth, screenHeight) {
        when {
            // TV / Large Tablet (>840dp)
            screenWidth >= 840.dp -> AdaptiveLayoutConfig(
                isCompact = false,
                isMedium = false,
                isExpanded = true,
                gridColumns = 3,
                playerHeight = (screenHeight * 0.4f).coerceIn(280.dp, 500.dp),
                contentPadding = 16.dp,
                itemSpacing = 12.dp,
                showMiniPlayer = true
            )
            // Tablet / Large Phone (600-840dp)
            screenWidth >= 600.dp -> AdaptiveLayoutConfig(
                isCompact = false,
                isMedium = true,
                isExpanded = false,
                gridColumns = 2,
                playerHeight = (screenHeight * 0.35f).coerceIn(220.dp, 400.dp),
                contentPadding = 12.dp,
                itemSpacing = 10.dp,
                showMiniPlayer = true
            )
            // Phone (<600dp)
            else -> AdaptiveLayoutConfig(
                isCompact = true,
                isMedium = false,
                isExpanded = false,
                gridColumns = 1,
                playerHeight = (screenWidth * 9f / 16f).coerceIn(180.dp, 300.dp),
                contentPadding = 8.dp,
                itemSpacing = 8.dp,
                showMiniPlayer = false
            )
        }
    }
}

/**
 * Configuración de tipografía según dispositivo
 */
@Composable
fun rememberAdaptiveTextConfig(): AdaptiveTextConfig {
    val layoutConfig = rememberAdaptiveLayoutConfig()

    return remember(layoutConfig) {
        when {
            layoutConfig.isExpanded -> AdaptiveTextConfig(
                headlineScale = 1.2f,
                bodyScale = 1.1f,
                labelScale = 1.0f
            )
            layoutConfig.isMedium -> AdaptiveTextConfig(
                headlineScale = 1.1f,
                bodyScale = 1.0f,
                labelScale = 0.95f
            )
            else -> AdaptiveTextConfig(
                headlineScale = 1.0f,
                bodyScale = 1.0f,
                labelScale = 0.9f
            )
        }
    }
}

data class AdaptiveTextConfig(
    val headlineScale: Float,
    val bodyScale: Float,
    val labelScale: Float
)
