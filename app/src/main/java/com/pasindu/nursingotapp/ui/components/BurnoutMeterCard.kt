// com/pasindu/nursingotapp/ui/components/BurnoutMeterCard.kt
package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BurnoutMeterCard(
    startDate: LocalDate,
    endDate: LocalDate,
    avgWeeklyHours: Float,
    consecutiveNightShifts: Int,
    suggestionText: String,
    modifier: Modifier = Modifier
) {
    // Existing thresholds are intentionally unchanged. This step is UI-only.
    val safeColor = Color(0xFF10B981)    // 0-40 hours
    val cautionColor = Color(0xFFF59E0B) // 41-48 hours
    val dangerColor = Color(0xFFEF4444)  // 49+ hours
    val trackColor = Color(0xFFE2E8F0)
    val inkColor = Color(0xFF1E293B)
    val mutedColor = Color(0xFF64748B)

    val gaugeColor = when {
        avgWeeklyHours <= 40f -> safeColor
        avgWeeklyHours <= 48f -> cautionColor
        else -> dangerColor
    }

    val statusText = when {
        avgWeeklyHours <= 40f -> "Optimal / Safe"
        avgWeeklyHours <= 48f -> "Caution: High OT"
        else -> "Danger: Burnout Risk"
    }

    val statusLabel = when {
        avgWeeklyHours <= 40f -> "WITHIN TARGET"
        avgWeeklyHours <= 48f -> "WATCH RECOVERY"
        else -> "HIGH LOAD"
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (avgWeeklyHours / 60f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1200),
        label = "BurnoutGaugeAnimation"
    )

    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "Burnout Risk Standards",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "These metrics use the existing NursingOTApp thresholds:\n\n" +
                            "• Safe Zone (0-40h): Optimal / Safe\n" +
                            "• Caution (41-48h): Caution: High OT\n" +
                            "• Danger (49h+): Danger: Burnout Risk\n\n" +
                            "Night-shift tracking is shown separately so the workload signal remains easy to read."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(gaugeColor)
                        )
                        Text(
                            text = "RECOVERY & BURNOUT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp,
                            color = mutedColor
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Weekly workload",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = inkColor
                    )

                    Text(
                        text = "${startDate.format(DateTimeFormatter.ofPattern("MMM dd"))} – ${endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = mutedColor
                    )
                }

                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = gaugeColor.copy(alpha = 0.10f)
                ) {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Burnout risk information",
                            tint = gaugeColor
                        )
                    }
                }
            }

            // Main gauge panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                gaugeColor.copy(alpha = 0.10f),
                                Color.White
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(220.dp, 122.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 22.dp.toPx()
                            val arcSize = Size(size.width - strokeWidth, (size.height * 2f) - strokeWidth)
                            val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                            drawArc(
                                color = trackColor,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = arcOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            drawArc(
                                color = gaugeColor,
                                startAngle = 180f,
                                sweepAngle = 180f * animatedProgress,
                                useCenter = false,
                                topLeft = arcOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 3.dp)
                        ) {
                            Text(
                                text = "${avgWeeklyHours.toInt()}h",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = inkColor
                            )
                            Text(
                                text = "of weekly workload",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = mutedColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = gaugeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = gaugeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = statusText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor
                    )
                }
            }

            // Night-shift signal
            val safeNightShiftCount = consecutiveNightShifts.coerceAtLeast(0)
            val isNightShiftCritical = safeNightShiftCount > 3

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = if (isNightShiftCritical) dangerColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC)
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Night-shift streak",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkColor
                            )
                            Text(
                                text = "19:00 – 07:00",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = mutedColor
                            )
                        }

                        Text(
                            text = "$safeNightShiftCount / 4",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isNightShiftCritical) dangerColor else inkColor
                        )
                    }

                    Spacer(modifier = Modifier.height(11.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        for (i in 1..4) {
                            val isActive = i <= safeNightShiftCount
                            val segmentColor = when {
                                !isActive -> trackColor
                                isNightShiftCritical -> dangerColor
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(segmentColor)
                            )
                        }
                    }
                }
            }

            // Existing suggestion text — presentation only
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = gaugeColor.copy(alpha = 0.08f)
            ) {
                Text(
                    text = suggestionText,
                    modifier = Modifier.padding(15.dp),
                    fontSize = 13.sp,
                    color = inkColor,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
