package com.pasindu.nursingotapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.domain.model.DecisionPriority
import com.pasindu.nursingotapp.domain.model.NursingOsDecision

@Composable
fun NursingOsDecisionCard(
    decision: NursingOsDecision,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (decision.priority) {
        DecisionPriority.URGENT -> Color(0xFFDC2626)
        DecisionPriority.TODAY -> Color(0xFFD97706)
        DecisionPriority.LATER -> Color(0xFF475569)
        DecisionPriority.CLEAR -> Color(0xFF059669)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAction),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "NURSINGOS • NEXT ACTION",
                        color = Color(0xFF27187E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "WHAT SHOULD I DO NOW?",
                        modifier = Modifier.padding(top = 3.dp),
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(
                    Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = accent
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = accent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        decision.priority.name,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        decision.title,
                        modifier = Modifier.padding(top = 3.dp),
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        decision.reason,
                        modifier = Modifier.padding(top = 4.dp),
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open action",
                    tint = accent
                )
            }

            Text(
                decision.actionLabel,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
