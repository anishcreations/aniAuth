package com.aniauth.authenticator.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aniauth.authenticator.ui.theme.DarkBg
import com.aniauth.authenticator.ui.theme.DarkCard
import com.aniauth.authenticator.ui.theme.PurpleAccent
import com.aniauth.authenticator.ui.theme.RawLightBg

/**
 * ScrollHintBox provides a 100% seamless gradient fade matching the exact popup background surface color,
 * plus a plain bouncing arrow icon (White in Dark theme, Purple in Light theme).
 */
@Composable
fun ScrollHintBox(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    fadeColor: Color = DarkCard,
    content: @Composable BoxScope.() -> Unit
) {
    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }

    val infiniteTransition = rememberInfiniteTransition(label = "ScrollHintBounce")
    val bounceOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceY"
    )

    val isLight = MaterialTheme.colorScheme.background == RawLightBg
    val arrowTint = if (isLight) PurpleAccent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)

    Box(modifier = modifier) {
        content()

        // 100% Seamless Gradient Fade matching exact popup background color
        AnimatedVisibility(
            visible = canScrollForward,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, fadeColor)
                        )
                    )
            )
        }

        // Plain Transparent Bouncing Down-Arrow Icon (White in Dark theme)
        AnimatedVisibility(
            visible = canScrollForward,
            enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll down hint",
                tint = arrowTint,
                modifier = Modifier
                    .size(20.dp)
                    .offset(y = bounceOffsetY.dp)
            )
        }
    }
}

/**
 * LazyColumnScrollHintBox provides a 100% seamless theme-matching gradient and bouncing arrow for LazyColumn lists.
 */
@Composable
fun LazyColumnScrollHintBox(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    fadeColor: Color = DarkBg,
    content: @Composable BoxScope.() -> Unit
) {
    val canScrollForward by remember { derivedStateOf { lazyListState.canScrollForward } }

    val infiniteTransition = rememberInfiniteTransition(label = "LazyScrollHintBounce")
    val bounceOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceY"
    )

    val isLight = MaterialTheme.colorScheme.background == RawLightBg
    val arrowTint = if (isLight) PurpleAccent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)

    Box(modifier = modifier) {
        content()

        // 100% Seamless Gradient Fade matching background color
        AnimatedVisibility(
            visible = canScrollForward,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, fadeColor)
                        )
                    )
            )
        }

        // Plain Transparent Bouncing Down-Arrow Icon (White in Dark theme)
        AnimatedVisibility(
            visible = canScrollForward,
            enter = fadeIn(tween(200)) + slideInVertically { it },
            exit = fadeOut(tween(150)) + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll down hint",
                tint = arrowTint,
                modifier = Modifier
                    .size(22.dp)
                    .offset(y = bounceOffsetY.dp)
            )
        }
    }
}
