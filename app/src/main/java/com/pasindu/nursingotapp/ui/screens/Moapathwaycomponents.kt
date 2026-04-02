package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun MoAPathwayOverlay(drug: EmergencyDrug, onClose: () -> Unit) {
    var visibleStepCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (i in 0..drug.moaSteps.size) {
            delay(500)
            visibleStepCount = i
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1120).copy(alpha = 0.98f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            AnimatedReceptorBinding(drug)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 220.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                drug.moaSteps.forEachIndexed { index, step ->
                    AnimatedVisibility(
                        visible = visibleStepCount > index,
                        enter = slideInHorizontally(
                            initialOffsetX = { 200 },
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
                        ) + fadeIn(tween(300))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(top = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = drug.gradientStart.copy(alpha = 0.5f),
                                        start = Offset(size.width / 2, size.height),
                                        end = Offset(size.width / 2, size.height + 200f),
                                        strokeWidth = 4f
                                    )
                                    drawCircle(color = drug.gradientStart.copy(alpha = 0.3f), radius = size.width / 2)
                                    drawCircle(color = drug.gradientEnd, radius = size.width / 4)
                                    drawCircle(color = Color.White, radius = size.width / 8)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = drug.gradientEnd.copy(alpha = 0.4f))
                                    .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    .border(1.dp, drug.gradientEnd.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "PHASE ${index + 1}",
                                    color = drug.gradientEnd,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = step.title,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = step.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PHARMACOLOGICAL PATHWAY",
                        color = drug.gradientEnd,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = drug.name,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun AnimatedReceptorBinding(drug: EmergencyDrug) {
    val infiniteTransition = rememberInfiniteTransition(label = "receptor_binding")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "binding_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = 80.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val cx = w / 2f
            val cy = h * 0.6f

            // Cell membrane
            drawLine(Color(0xFF334155), Offset(0f, cy + 20f), Offset(w, cy + 20f), strokeWidth = 8f)
            drawLine(drug.gradientEnd.copy(alpha = 0.2f), Offset(0f, cy + 20f), Offset(w, cy + 20f), strokeWidth = 24f)

            // Receptor (U-shaped lock)
            val receptorPath = Path().apply {
                moveTo(cx - 35f, cy - 25f)
                lineTo(cx - 35f, cy + 10f)
                quadraticBezierTo(cx, cy + 40f, cx + 35f, cy + 10f)
                lineTo(cx + 35f, cy - 25f)
            }
            drawPath(receptorPath, color = Color(0xFF475569), style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Drug molecule Y position
            val drugStartY = cy - 120f
            val drugEndY = cy - 5f
            val currentDrugY = when {
                phase < 0.3f -> drugStartY + (drugEndY - drugStartY) * (phase / 0.3f)
                phase < 0.8f -> drugEndY
                else -> drugStartY
            }
            val drugAlpha = when {
                phase < 0.1f -> phase / 0.1f
                phase < 0.8f -> 1f
                else -> 1f - ((phase - 0.8f) / 0.2f)
            }

            // Spinning hexagon drug molecule (key)
            if (phase < 0.8f) {
                withTransform({
                    rotate(
                        degrees = if (phase < 0.3f) phase * 360f else 0f,
                        pivot = Offset(cx, currentDrugY)
                    )
                }) {
                    val hexRadius = 20f
                    val hexPath = Path().apply {
                        for (i in 0..5) {
                            val angle = Math.toRadians(i * 60.0)
                            val px = cx + (hexRadius * Math.cos(angle)).toFloat()
                            val py = currentDrugY + (hexRadius * Math.sin(angle)).toFloat()
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    drawPath(hexPath, color = drug.gradientStart.copy(alpha = drugAlpha))
                    drawPath(hexPath, color = drug.gradientEnd.copy(alpha = drugAlpha), style = Stroke(width = 4f, join = StrokeJoin.Round))
                }
            }

            // Binding confirmation — receptor lights up + ripple rings
            if (phase in 0.3f..0.8f) {
                drawPath(receptorPath, color = drug.gradientEnd.copy(alpha = 0.8f), style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(receptorPath, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                val rippleProgress = (phase - 0.3f) / 0.5f
                val rippleRadius = 20f + (150f * rippleProgress)
                val rippleAlpha = 1f - rippleProgress

                drawCircle(color = drug.gradientEnd.copy(alpha = rippleAlpha * 0.6f), radius = rippleRadius, center = Offset(cx, cy + 10f), style = Stroke(width = 8f))
                drawCircle(color = drug.gradientStart.copy(alpha = rippleAlpha * 0.3f), radius = rippleRadius * 0.7f, center = Offset(cx, cy + 10f), style = Stroke(width = 4f))
            }

            // Intracellular signal cascade particles streaming downward
            if (phase > 0.35f && phase < 0.9f) {
                val particlePhase = ((phase - 0.35f) * 2f) % 1f
                val pY = cy + 20f + (h - cy) * particlePhase
                drawCircle(color = drug.gradientStart.copy(alpha = 1f - particlePhase), radius = 6f, center = Offset(cx - 30f, pY))
                drawCircle(color = drug.gradientEnd.copy(alpha = 1f - particlePhase), radius = 8f, center = Offset(cx + 30f, pY + 15f))
                drawCircle(color = Color.White.copy(alpha = 1f - particlePhase), radius = 4f, center = Offset(cx, pY + 30f))
            }
        }
    }
}