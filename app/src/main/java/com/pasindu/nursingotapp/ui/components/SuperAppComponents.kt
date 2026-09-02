package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pasindu.nursingotapp.ui.theme.AdvancedGradient
import com.pasindu.nursingotapp.ui.theme.ClinicalAiGradient
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.PositiveGradient
import kotlinx.coroutines.delay

@Composable
fun SuperAppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.animateContentSize(),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(NursingDimensions.Radius.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card)
    ) {
        Column(modifier = Modifier.padding(NursingDimensions.Spacing.lg)) { content() }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NursingDimensions.Radius.large),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = NursingDimensions.Elevation.card,
        shadowElevation = NursingDimensions.Elevation.card
    ) {
        Column(modifier = Modifier.padding(NursingDimensions.Spacing.lg)) { content() }
    }
}

@Composable
fun GradientHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    gradient: Brush = ClinicalAiGradient,
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(NursingDimensions.Radius.extraLarge))
            .padding(NursingDimensions.Spacing.xxl)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                subtitle?.let {
                    Spacer(Modifier.height(NursingDimensions.Spacing.sm))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
            content?.invoke()
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    SuperAppCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.md)
        ) {
            icon?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(NursingDimensions.Spacing.xs))
                Text(value, style = MaterialTheme.typography.headlineSmall)
                supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

enum class StatusBadgeTone { Success, Warning, Error, Info }

@Composable
fun StatusBadge(text: String, tone: StatusBadgeTone, modifier: Modifier = Modifier) {
    val (container, contentColor, icon) = when (tone) {
        StatusBadgeTone.Success -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Default.CheckCircle)
        StatusBadgeTone.Warning -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Default.Info)
        StatusBadgeTone.Error -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Default.ErrorOutline)
        StatusBadgeTone.Info -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Default.Info)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(NursingDimensions.Radius.pill), color = container) {
        Row(
            modifier = Modifier.padding(horizontal = NursingDimensions.Spacing.md, vertical = NursingDimensions.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(NursingDimensions.Icon.small))
            Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        subtitle?.let {
            Spacer(Modifier.height(NursingDimensions.Spacing.xs))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AnimatedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(120)
            pressed = false
        }
    }
    Button(onClick = { pressed = true; onClick() }, enabled = enabled, modifier = modifier.scale(scale)) {
        Text(text)
    }
}

@Composable
fun AnimatedCounter(value: Int, modifier: Modifier = Modifier, label: String? = null) {
    val animatedValue by animateIntAsState(value, tween(900, easing = FastOutSlowInEasing), label = "counter")
    Column(modifier = modifier) {
        AnimatedContent(targetState = animatedValue, label = "counter_content") { current ->
            Text(current.toString(), style = MaterialTheme.typography.headlineMedium)
        }
        label?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun AnimatedProgress(progress: Float, modifier: Modifier = Modifier, label: String? = null) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing), label = "progress")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs)) {
        label?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun AnimatedCardEntrance(
    visible: Boolean,
    modifier: Modifier = Modifier,
    fromLeft: Boolean = false,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(350)) + if (fromLeft) slideInHorizontally(tween(400)) { -it / 3 } else slideInVertically(tween(400)) { it / 4 } + scaleIn(tween(400), initialScale = 0.92f),
        exit = fadeOut(tween(200)) + if (fromLeft) slideOutHorizontally(tween(200)) { -it / 5 } else slideOutVertically(tween(200)) { it / 5 } + scaleOut(tween(200), targetScale = 0.96f)
    ) { content() }
}

@Composable
fun PulsingGlow(modifier: Modifier = Modifier, color: Color = Color.White, enabled: Boolean = true) {
    val alpha by animateFloatAsState(if (enabled) 0.28f else 0f, tween(1200), label = "glow")
    Box(modifier = modifier.background(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent)), CircleShape))
}

@Composable
fun GradientPulseBadge(text: String, modifier: Modifier = Modifier, gradient: Brush = PositiveGradient) {
    val transition = rememberInfiniteTransition(label = "badge_pulse")
    val scale by transition.animateFloat(1f, 1.04f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "badge_scale")
    Box(
        modifier = modifier
            .scale(scale)
            .background(gradient, RoundedCornerShape(NursingDimensions.Radius.pill))
            .padding(horizontal = NursingDimensions.Spacing.md, vertical = NursingDimensions.Spacing.xs)
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White) }
}

@Composable
fun GradientProgressCard(
    title: String,
    progress: Float,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    gradient: Brush = AdvancedGradient
) {
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1200, easing = FastOutSlowInEasing), label = "gradient_progress")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(NursingDimensions.Radius.extraLarge))
            .padding(NursingDimensions.Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.88f)) }
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha = 0.22f))
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(NursingDimensions.Spacing.section), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info, null, modifier = Modifier.size(NursingDimensions.Icon.large), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(NursingDimensions.Spacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(NursingDimensions.Spacing.xs))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Loading…") {
    Column(modifier = modifier.fillMaxWidth().padding(NursingDimensions.Spacing.section), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(NursingDimensions.Spacing.md))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(0.35f, 0.72f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "skeleton_alpha")
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha), RoundedCornerShape(NursingDimensions.Radius.medium)))
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(NursingDimensions.Spacing.section), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(NursingDimensions.Icon.large), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(NursingDimensions.Spacing.md))
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(NursingDimensions.Spacing.xs))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        onRetry?.let {
            Spacer(Modifier.height(NursingDimensions.Spacing.md))
            OutlinedButton(onClick = it) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.size(NursingDimensions.Spacing.xs))
                Text("Retry")
            }
        }
    }
}
