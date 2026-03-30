package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

@Composable
fun AnimatedSuccessOverlay(
    isVisible: Boolean,
    onAnimationFinished: () -> Unit
) {
    // We fetch a beautiful free Lottie animation directly via URL for instant testing
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/82d9bf89-4a00-4ea5-b541-ab777eb644e5/G9FqGZk6h2.json")
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isVisible,
        iterations = 1 // Play exactly once
    )

    // Automatically trigger the next step (sharing the PDF) when animation finishes
    LaunchedEffect(progress) {
        if (progress == 1f && isVisible) {
            delay(300) // Brief pause so they can admire the checkmark
            onAnimationFinished()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)) + scaleIn(tween(400, delayMillis = 100)),
        exit = fadeOut(tween(400)) + scaleOut(tween(400))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)), // Dark glass background
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The Lottie Animation
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(150.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PDF Generated!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preparing to share your claim...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}