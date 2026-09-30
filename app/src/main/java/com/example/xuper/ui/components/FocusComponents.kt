package com.example.xuper.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.xuper.ui.theme.FocusBorder

/**
 * Modificador unificado para manejo de focus en toda la app
 * Proporciona animaciones y estilos consistentes
 */
@Composable
fun Modifier.xuperFocusable(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape,
    focusedScale: Float = 1.05f,
    unfocusedScale: Float = 1f,
    focusBorderWidth: Dp = 3.dp,
    focusBorderColor: Color = FocusBorder,
    animationDurationMs: Int = 150
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else unfocusedScale,
        animationSpec = tween(durationMillis = animationDurationMs),
        label = "focusScale"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) focusBorderWidth else 0.dp,
        animationSpec = tween(durationMillis = animationDurationMs),
        label = "focusBorder"
    )

    return this
        .scale(scale)
        .border(
            width = borderWidth,
            color = focusBorderColor,
            shape = shape
        )
        .focusable(interactionSource = interactionSource)
}

/**
 * Modificador simple de focus con callback
 */
@Composable
fun Modifier.xuperFocusableWithCallback(
    shape: Shape,
    focusedScale: Float = 1.05f,
    focusBorderWidth: Dp = 3.dp,
    focusBorderColor: Color = FocusBorder,
    onFocusChanged: (Boolean) -> Unit
): Modifier {
    return this
        .onFocusChanged { focusState ->
            onFocusChanged(focusState.isFocused)
        }
        .focusable()
}
