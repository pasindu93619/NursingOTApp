package com.pasindu.nursingotapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SedationAnalgesiaCalculatorCard(modifier: Modifier = Modifier) {
    // State variables for inputs
    var weightInput by remember { mutableStateOf("") }
    var doseInput by remember { mutableStateOf("") }
    var drugMassInput by remember { mutableStateOf("") }
    var volumeInput by remember { mutableStateOf("") }

    // Theme Colors (Cool Tech Blue & Crisp Slate-White)
    val techBlue = Color(0xFF1976D2)
    val skyBlue = Color(0xFFE3F2FD)
    val slateWhite = Color(0xFFF8F9FA)
    val alertAmber = Color(0xFFFFA000)

    // Calculation Logic
    val weight = weightInput.toFloatOrNull() ?: 0f
    val dose = doseInput.toFloatOrNull() ?: 0f
    val mass = drugMassInput.toFloatOrNull() ?: 0f
    val volume = volumeInput.toFloatOrNull() ?: 0f

    val concentration = if (volume > 0f) mass / volume else 0f
    val rate = if (concentration > 0f) (dose * weight) / concentration else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(slateWhite)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Sedation & Analgesia Infusion",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = techBlue
            )

            Text(
                text = "Calculates mL/hr for continuous infusions (Propofol, Midazolam, Fentanyl).",
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            // Inputs
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = techBlue
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = doseInput,
                    onValueChange = { doseInput = it },
                    label = { Text("Dose (mg/kg/hr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = techBlue
                    ),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = drugMassInput,
                    onValueChange = { drugMassInput = it },
                    label = { Text("Drug Mass (mg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = techBlue
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = volumeInput,
                    onValueChange = { volumeInput = it },
                    label = { Text("Volume (mL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = techBlue
                    ),
                    singleLine = true
                )
            }

            Divider(color = skyBlue, thickness = 2.dp)

            // Result Display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = skyBlue,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Infusion Rate",
                        fontSize = 14.sp,
                        color = techBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (rate > 0f) String.format(Locale.US, "%.1f mL/hr", rate) else "0.0 mL/hr",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = techBlue
                    )
                    if (concentration > 0f) {
                        Text(
                            text = String.format(Locale.US, "Conc: %.2f mg/mL", concentration),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Clinical Safety Warning
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(alertAmber.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = alertAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Always confirm concentration. Use a syringe pump; avoid manual drips for sedation.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}