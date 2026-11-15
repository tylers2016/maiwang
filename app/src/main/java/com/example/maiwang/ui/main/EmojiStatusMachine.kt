package com.example.maiwang.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiStatusMachine(
    currentTab: String,
    isYiwenLoading: Boolean,
    isXiaochouLoading: Boolean,
    isTupuLoading: Boolean,
    xiaochouStep: Int,
    modifier: Modifier = Modifier
) {
    // 1. Infinite Transition definitions
    val infiniteTransition = rememberInfiniteTransition(label = "EmojiStatusTransition")

    // Pulse Animation (0.95f to 1.05f) - Used for 🌸
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Elastic Pulse Animation (0.8f to 1.2f) - Used for 🔍
    val elasticPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "elasticPulseScale"
    )

    // Infinite Spin Animation (0f to 360f) - Used for 🔄
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinRotation"
    )

    // Float Animation (-2f to 2f animated as float, used as offset y in dp) - Used for 📚, 📖, 🕸️
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // Flip Timer Animation (0 to 180 degrees: static for 1.7s, flip in 0.3s) - Used for ⏳
    val flipRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0 using LinearEasing
                0f at 1700 using FastOutSlowInEasing
                180f at 2000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "flipRotation"
    )

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        when (currentTab) {
            "藝文" -> {
                if (isYiwenLoading) {
                    Text(
                        text = "🔍",
                        fontSize = 26.sp,
                        modifier = Modifier.scale(elasticPulseScale)
                    )
                } else {
                    Text(
                        text = "🌸",
                        fontSize = 26.sp,
                        modifier = Modifier.scale(pulseScale)
                    )
                }
            }
            "校讎" -> {
                if (isXiaochouLoading) {
                    Text(
                        text = "🔄",
                        fontSize = 26.sp,
                        modifier = Modifier.rotate(spinRotation)
                    )
                } else if (xiaochouStep == 2) {
                    Text(
                        text = "📖",
                        fontSize = 26.sp,
                        modifier = Modifier.offset(y = floatOffset.dp)
                    )
                } else {
                    Text(
                        text = "📚",
                        fontSize = 26.sp,
                        modifier = Modifier.offset(y = floatOffset.dp)
                    )
                }
            }
            "圖譜" -> {
                if (isTupuLoading) {
                    Text(
                        text = "⏳",
                        fontSize = 26.sp,
                        modifier = Modifier.rotate(flipRotation)
                    )
                } else {
                    Text(
                        text = "🕸️",
                        fontSize = 26.sp,
                        modifier = Modifier.offset(y = floatOffset.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = "🌸",
                    fontSize = 26.sp,
                    modifier = Modifier.scale(pulseScale)
                )
            }
        }
    }
}
