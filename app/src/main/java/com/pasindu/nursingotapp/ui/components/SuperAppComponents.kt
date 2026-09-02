package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pasindu.nursingotapp.ui.theme.AppGradients
import com.pasindu.nursingotapp.ui.theme.NursingDimensions

@Composable
fun SuperAppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.animateContentSize(),
        onClick = onClick,
        shape = RoundedCornerShape(NursingDimensions.Radius.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = NursingDimensions.Elevation.card),
        content = { Column(modifier = Modifier.padding(NursingDimensions.Spacing.card), content = { content() }) }
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NursingDimensions.Radius.card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = NursingDimensions.Elevation.card,
        shadowElevation = NursingDimensions.Elevation.card
    ) {
        Column(modifier = Modifier.padding(NursingDimensions.Spacing.card), content = { content() })
    }
}

@Composable
fun GradientHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    gradient: Brush = AppGradients.clinicalAi,
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(NursingDimensions.Radius.large))
            .padding(NursingDimensions.Spacing.large)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.medium)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                subtitle?.let {
                    Spacer(Modifier.height(NursingDimensions.Spacing.small))
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.medium)) {
            icon?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(NursingDimensions.Spacing.xSmall))
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

enum class StatusBadgeTone { Success, Warning, Error, Info }

@Composable
fun StatusBadge(text: String, tone: StatusBadgeTone, modifier: Modifier = Modifier) {
    val (container, contentColor, icon) = when (tone) {
        StatusBadgeTone.Success -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Default.CheckCircle)
        StatusBadgeTone.Warning -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Default.Info)
        StatusBadgeTone.Error -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Default.ErrorOutline)
        StatusBadgeTone.Info -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Default.Info)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(NursingDimensions.Radius.pill), color = container) {
        Row(modifier = Modifier.padding(horizontal = NursingDimensions.Spacing.medium, vertical = NursingDimensions.Spacing.xSmall), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xSmall)) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(NursingDimensions.Icon.small))
            Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Spacer(Modifier.height(NursingDimensions.Spacing.xSmall))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) { Text(text) }
}

@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "counter"
    )
    Column(modifier = modifier) {
        Text(animatedValue.toInt().toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        label?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun AnimatedProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, tween(900, easing = FastOutSlowInEasing), label = "progress")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xSmall)) {
        label?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(NursingDimensions.Spacing.extraLarge), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(NursingDimensions.Icon.large), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(NursingDimensions.Spacing.medium))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(NursingDimensions.Spacing.xSmall))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Loading…") {
    Column(modifier = modifier.fillMaxWidth().padding(NursingDimensions.Spacing.extraLarge), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(NursingDimensions.Spacing.medium))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(NursingDimensions.Spacing.extraLarge), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(NursingDimensions.Icon.large), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(NursingDimensions.Spacing.medium))
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(NursingDimensions.Spacing.xSmall))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        onRetry?.let {
            Spacer(Modifier.height(NursingDimensions.Spacing.medium))
            OutlinedButton(onClick = it) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.size(NursingDimensions.Spacing.xSmall))
                Text("Retry")
            }
        }
    }
}
