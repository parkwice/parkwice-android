package com.mintech.parkwiseapp.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mintech.parkwiseapp.ui.theme.*

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -800f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E2C34),
            Color(0xFF2A363E),
            Color(0xFF3A5060),
            Color(0xFF4A6575),
            Color(0xFF3A5060),
            Color(0xFF2A363E),
            Color(0xFF1E2C34),
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 800f, 0f)
    )
}

@Composable
fun ShimmerHistoryItem() {
    val brush = rememberShimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(brush, CircleShape)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .height(15.dp)
                    .fillMaxWidth(0.5f)
                    .background(brush, RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.38f)
                    .background(brush, RoundedCornerShape(6.dp))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(brush, CircleShape)
        )
    }
}

@Composable
fun ShimmerVehicleCard() {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier
            .background(SurfaceLow, RoundedCornerShape(16.dp))
            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier
                .height(14.dp)
                .fillMaxWidth(0.65f)
                .background(brush, RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .height(10.dp)
                .fillMaxWidth(0.4f)
                .background(brush, RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.weight(1f))
        Row {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.3f)
                    .background(brush, RoundedCornerShape(6.dp))
            )
        }
    }
}
