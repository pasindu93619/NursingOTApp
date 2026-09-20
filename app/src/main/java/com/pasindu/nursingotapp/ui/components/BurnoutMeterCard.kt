package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pasindu.nursingotapp.ui.theme.Amber
import com.pasindu.nursingotapp.ui.theme.AppBackground
import com.pasindu.nursingotapp.ui.theme.ClinicalPrimaryColor
import com.pasindu.nursingotapp.ui.theme.CriticalRed
import com.pasindu.nursingotapp.ui.theme.Emerald
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BurnoutMeterCard(
    startDate: LocalDate,
    endDate: LocalDate,
    avgWeeklyHours: Float,
    consecutiveNightShifts: Int,
    suggestionText: String,
    totalHours: Float = 0f,
    periodDays: Int = 7,
    weeksEquivalent: Float = 1f,
    isWeeklyView: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Keep the existing deterministic thresholds and gauge math unchanged.
    val gaugeColor = when {
        avgWeeklyHours <= 40f -> Emerald
        avgWeeklyHours <= 48f -> Amber
        else -> CriticalRed
    }
    val statusText = when {
        avgWeeklyHours <= 40f -> "Optimal / Safe"
        avgWeeklyHours <= 48f -> "Caution: High OT"
        else -> "Danger: Burnout Risk"
    }
    val animatedProgress by animateFloatAsState(
        targetValue = (avgWeeklyHours / 60f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "burnout_gauge"
    )
    val isNightShiftCritical = consecutiveNightShifts > 3
    var showInfoDialog by remember { mutableStateOf(false) }

    val scopeLabel = if (isWeeklyView) "Selected week" else "Full claim period"
    val calculationText = if (weeksEquivalent > 0f && periodDays > 0) {
        (if (isWeeklyView) "Weekly workload" else "Average weekly workload") +
            " = (${totalHours.toInt()}h ÷ ${periodDays} days) × 7 = ${avgWeeklyHours.toInt()}h/week"
    } else {
        "No logged workload in this period."
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    "How the Burnout Meter is calculated",
                    color = Slate,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(scopeLabel, color = ClinicalPrimaryColor, fontWeight = FontWeight.Bold)
                    Text(calculationText, color = Slate, fontWeight = FontWeight.Bold)
                    Text(
                        "What the value describes: the logged workload normalized to a 7-day week. " +
                            "It describes workload pressure in the selected claim or week; it is not a medical diagnosis.",
                        color = TextSecondary
                    )
                    Text(
                        "Dashboard thresholds: 0–40 h/week = Optimal / Safe, 41–48 h/week = Caution: High OT, " +
                            "49+ h/week = Danger: Burnout Risk. More than 3 consecutive night shifts is also flagged.",
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Burnout & workload meter", color = Slate, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} – ${endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        color = ClinicalPrimaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        scopeLabel,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Burnout meter information", tint = ClinicalPrimaryColor)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFEAF6FF), Color(0xFFF3EEFF))))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(210.dp, 115.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeWidth = 22.dp.toPx()
                        val arcSize = Size(size.width, size.height * 2)
                        drawArc(
                            color = Color(0xFFDCE6F0),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset.Zero,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = gaugeColor,
                            startAngle = 180f,
                            sweepAngle = 180f * animatedProgress,
                            useCenter = false,
                            topLeft = Offset.Zero,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${avgWeeklyHours.toInt()}h", color = Slate, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text(statusText, color = gaugeColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppBackground)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "How this value is calculated",
                        color = Slate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        calculationText,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Text(
                        "Describes workload pressure for the selected scope, not a medical diagnosis.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Consecutive night shifts", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("$consecutiveNightShifts", color = if (isNightShiftCritical) CriticalRed else ClinicalPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val active = i <= consecutiveNightShifts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (active) { if (isNightShiftCritical) CriticalRed else ClinicalPrimaryColor } else Color(0xFFE2E8F0))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (avgWeeklyHours > 40f) Amber.copy(alpha = 0.12f) else Emerald.copy(alpha = 0.10f))
                    .padding(13.dp)
            ) {
                Text(
                    text = suggestionText,
                    color = Slate,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}