package com.example.alarm.core.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import com.example.alarm.core.ui.theme.AnimationDurations

// Animation Specs
val fadeInAnimationSpec: AnimationSpec<Float> = tween(
    durationMillis = AnimationDurations.durationMedium.inWholeMilliseconds.toInt(),
    easing = EaseInOutCubic
)

val slideInAnimationSpec: AnimationSpec<Float> = tween(
    durationMillis = AnimationDurations.durationMedium.inWholeMilliseconds.toInt(),
    easing = EaseInOutCubic
)

val scaleInAnimationSpec: AnimationSpec<Float> = tween(
    durationMillis = AnimationDurations.durationMedium.inWholeMilliseconds.toInt(),
    easing = EaseInOutCubic
)

val bounceAnimationSpec: AnimationSpec<Float> = tween(
    durationMillis = AnimationDurations.durationLong.inWholeMilliseconds.toInt(),
    easing = EaseInOutCubic
)

// Fade In Animation
@Composable
fun FadeInModifier(
    visible: Boolean = true,
    durationMillis: Int = AnimationDurations.durationMedium.inWholeMilliseconds.toInt()
): Modifier {
    val alpha = remember { Animatable(if (visible) 0f else 1f) }

    LaunchedEffect(visible) {
        alpha.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(durationMillis = durationMillis, easing = EaseInOutCubic)
        )
    }

    return Modifier.graphicsLayer(alpha = alpha.value)
}

// Scale In Animation
@Composable
fun ScaleInModifier(
    visible: Boolean = true,
    durationMillis: Int = AnimationDurations.durationMedium.inWholeMilliseconds.toInt()
): Modifier {
    val scale = remember { Animatable(if (visible) 0.8f else 1f) }

    LaunchedEffect(visible) {
        scale.animateTo(
            targetValue = if (visible) 1f else 0.8f,
            animationSpec = tween(durationMillis = durationMillis, easing = EaseInOutCubic)
        )
    }

    return Modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value)
}

// Pulse Animation (for alarm ring)
@Composable
fun PulseModifier(
    enabled: Boolean = true,
    durationMillis: Int = AnimationDurations.durationLong.inWholeMilliseconds.toInt()
): Modifier {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(enabled) {
        if (enabled) {
            while (true) {
                scale.animateTo(
                    targetValue = 1.1f,
                    animationSpec = tween(durationMillis = durationMillis / 2, easing = EaseInOutCubic)
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMillis / 2, easing = EaseInOutCubic)
                )
            }
        }
    }

    return Modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value)
}

// Button Press Animation
@Composable
fun ButtonPressModifier(
    pressed: Boolean = false,
    durationMillis: Int = AnimationDurations.durationShort.inWholeMilliseconds.toInt()
): Modifier {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        scale.animateTo(
            targetValue = if (pressed) 0.95f else 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = EaseInOutCubic)
        )
    }

    return Modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value)
}
