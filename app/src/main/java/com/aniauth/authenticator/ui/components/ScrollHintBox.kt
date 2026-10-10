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

/**
 * ScrollHintBox provides a 100% seamless gradient fade matching the exact popup background surface color,
 * plus a static, theme-aware arrow for overflow.
 */
@Composable
fun ScrollHintBox(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    fadeColor: Color = DarkCard,
    content: @Composable BoxScope.() -> Unit
) {
    val canScrollForward by remember { derivedStateOf { scrollState.canScrollForward } }

    val arrowTint = MaterialTheme.colorScheme.onSurfaceVariant

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

        // Theme-aware down-arrow hint
        AnimatedVisibility(
            visible = canScrollForward,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
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
            )
        }
    }
}

/**
 * LazyColumnScrollHintBox provides a 100% seamless theme-matching gradient and static arrow for LazyColumn lists.
 */
@Composable
fun LazyColumnScrollHintBox(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    fadeColor: Color = DarkBg,
    content: @Composable BoxScope.() -> Unit
) {
    val canScrollForward by remember { derivedStateOf { lazyListState.canScrollForward } }

    val arrowTint = MaterialTheme.colorScheme.onSurfaceVariant

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

        // Theme-aware down-arrow hint
        AnimatedVisibility(
            visible = canScrollForward,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
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
            )
        }
    }
}
