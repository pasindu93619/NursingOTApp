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
    val safeColor = Color(0xFF4CAF50)    // 0-40 hours
    val cautionColor = Color(0xFFFFC107) // 41-48 hours
    val dangerColor = Color(0xFFF44336)  // 49+ hours
    val trackColor = Color(0xFFE0E0E0)

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

    val animatedProgress by animateFloatAsState(
        targetValue = (avgWeeklyHours / 60f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1500),
        label = "GaugeAnimation"
    )

    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Burnout Risk Standards", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "These metrics are based on the American Nurses Association (ANA) and NIOSH guidelines:\n\n" +
                            "• Safe Zone (0-40h): Optimal for patient safety and nurse recovery.\n" +
                            "• Caution (41-48h): Fatigue begins to impair decision making.\n" +
                            "• Danger (49h+): Drastically increased risk of medical errors and severe burnout.\n\n" +
                            "Night Shifts: Working more than 3 consecutive 12-hour night shifts requires a mandatory 2-day rest period to reset circadian rhythms."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Understood") }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Info Button and Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Avg Weekly Burnout Risk",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} - ${endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { showInfoDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Semi-Circle Gauge Chart
            Box(
                modifier = Modifier.size(200.dp, 100.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val size = Size(size.width, size.height * 2)

                    drawArc(
                        color = trackColor, startAngle = 180f, sweepAngle = 180f,
                        useCenter = false, topLeft = Offset(0f, 0f), size = size,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = gaugeColor, startAngle = 180f, sweepAngle = 180f * animatedProgress,
                        useCenter = false, topLeft = Offset(0f, 0f), size = size,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${avgWeeklyHours.toInt()}h",
                        fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = gaugeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Night Shift Tracker
            val isNightShiftCritical = consecutiveNightShifts > 3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Consecutive Night Shifts (19:00 - 07:00)",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in 1..4) {
                        val isActive = i <= consecutiveNightShifts
                        val circleColor = if (isActive) {
                            if (isNightShiftCritical) dangerColor else MaterialTheme.colorScheme.primary
                        } else trackColor
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(circleColor))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Suggestion Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (avgWeeklyHours > 40f) dangerColor.copy(alpha = 0.1f) else safeColor.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = suggestionText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}