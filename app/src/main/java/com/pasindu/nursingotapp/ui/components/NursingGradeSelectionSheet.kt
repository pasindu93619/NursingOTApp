package com.pasindu.nursingotapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.domain.usecase.NursingOtRatePolicy
import java.text.NumberFormat
import java.util.Locale

private val SheetBlue = Color(0xFF0EA5E9)
private val SheetInk = Color(0xFF1E293B)
private val SheetSecondary = Color(0xFF475569)
private val SheetBackground = Color(0xFFF8FAFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NursingGradeSelectionSheet(
    selectedGrade: String,
    onGradeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CURRENT NURSING GRADE",
                color = SheetInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Select your current Nursing Service grade. The official OT rate is assigned automatically.",
                color = SheetSecondary,
                fontSize = 11.sp
            )

            NursingOtRatePolicy.grades.forEach { grade ->
                val selected = grade == selectedGrade
                val rate = NursingOtRatePolicy.rateForGrade(grade) ?: return@forEach

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onGradeSelected(grade)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) Color(0xFFEAF6FF) else SheetBackground,
                    border = if (selected) {
                        BorderStroke(1.5.dp, SheetBlue.copy(alpha = 0.65f))
                    } else null
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = if (selected) SheetBlue.copy(alpha = 0.14f) else Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selected) Icons.Default.Check else Icons.Default.WorkOutline,
                                    contentDescription = null,
                                    tint = if (selected) SheetBlue else SheetSecondary
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = grade,
                                color = SheetInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Nursing Service OT rate • Rs. " + NumberFormat.getNumberInstance(Locale.US).format(rate) + " / hour",
                                color = if (selected) SheetBlue else SheetSecondary,
                                fontSize = 10.sp
                            )
                        }

                        if (selected) {
                            Text(
                                text = "SELECTED",
                                color = SheetBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
