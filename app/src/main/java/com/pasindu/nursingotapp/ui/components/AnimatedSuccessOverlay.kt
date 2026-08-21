package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedSuccessOverlay(
    isVisible: Boolean,
    onAnimationFinished: () -> Unit
) {
    // Upgraded Mind-Blowing Lottie (Celebration/Document Success)
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/a613dccf-b73a-4be0-80a2-f6734d40232b/gK7C4p3y9H.json") // High-energy success animation
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isVisible,
        iterations = 1,
        speed = 1.2f // Slightly faster for snappier feel
    )

    // Infinite animations for the background aura
    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "aura_scale"
    )
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "aura_rot"
    )

    LaunchedEffect(progress) {
        if (progress == 1f && isVisible) {
            delay(500) // Let the user enjoy the mind-blowing effect
            onAnimationFinished()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + scaleIn(tween(500, easing = OvershootInterpolator().toEasing())),
        exit = fadeOut(tween(400)) + scaleOut(tween(400))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)), // Deep cinematic background
            contentAlignment = Alignment.Center
        ) {
            // Dynamic Rotating Aura Layer
            Canvas(modifier = Modifier.size(300.dp)) {
                val center = Offset(size.width / 2, size.height / 2)

                // Pulsing Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1976D2).copy(alpha = 0.4f), Color.Transparent),
                        center = center,
                        radius = (size.width / 2) * auraScale
                    )
                )

                // Rotating Particle Ring
                for (i in 0 until 8) {
                    val angle = Math.toRadians((auraRotation + (i * 45f)).toDouble())
                    val x = center.x + (100f * auraScale) * cos(angle).toFloat()
                    val y = center.y + (100f * auraScale) * sin(angle).toFloat()
                    drawCircle(color = Color(0xFF00FFCC), radius = 6f, center = Offset(x, y))
                }
            }

            // Glassmorphic Content Card
            Column(
                modifier = Modifier
                    .scale(if (progress > 0.1f) 1f else 0.8f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(28.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(180.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PDF Saved & Ready!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(if (progress > 0.4f) 1f else 0f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Saved to Downloads & Opening Share...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.alpha(if (progress > 0.6f) 1f else 0f)
                )
            }
        }
    }
}

// Helper Easing to make it "pop"
private class OvershootInterpolator(private val tension: Float = 2f) {
    fun toEasing(): Easing = Easing { t ->
        t * t * ((tension + 1) * t - tension)
    }
}