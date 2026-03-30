// com/pasindu/nursingotapp/ui/screens/ProfileScreen.kt
package com.pasindu.nursingotapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// --- APP MATCHED VIBRANT COLORS (Pulled directly from your HomeScreen) ---
val BaseWhiteBg = Color(0xFFF8FAFC)
val DarkSlate = Color(0xFF1E293B)
val LightSlate = Color(0xFF64748B)

val BlueGradStart = Color(0xFF00BCD4)
val BlueGradEnd = Color(0xFF1E88E5)
val PurpleGradStart = Color(0xFFAB47BC)
val PurpleGradEnd = Color(0xFF8E24AA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: NursingViewModel,
    onNavigateToClaimPeriod: (String, String) -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current

    // State Variables
    var name by remember { mutableStateOf("") }
    var serviceNo by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var paySheetNo by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var basicSalary by remember { mutableStateOf("") }
    var otRate by remember { mutableStateOf("") }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let {
            name = it.fullName
            serviceNo = it.serviceNo
            unit = it.unit
            paySheetNo = it.paySheetNo
            grade = it.grade
            basicSalary = if (it.basicSalary == 0.0) "" else it.basicSalary.toString()
            otRate = if (it.otRate == 0.0) "" else it.otRate.toString()
        }
        delay(100)
        isVisible = true
    }

    // --- PULSATING BUTTON ENGINE ---
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_fx")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    Scaffold(
        containerColor = Color.Transparent, // Transparent to let Aurora shine through
        bottomBar = {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(600, delayMillis = 400)) + fadeIn()
            ) {
                Box(modifier = Modifier.navigationBarsPadding().fillMaxWidth().padding(24.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .scale(buttonScale)
                            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = BlueGradEnd)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(BlueGradStart, BlueGradEnd)))
                            .clickable {
                                val basic = basicSalary.toDoubleOrNull() ?: 0.0
                                val ot = otRate.toDoubleOrNull() ?: 0.0

                                if (name.isBlank() || serviceNo.isBlank() || basic == 0.0 || ot == 0.0) {
                                    Toast.makeText(context, "Please fill required fields", Toast.LENGTH_LONG).show()
                                    return@clickable
                                }

                                viewModel.saveProfile(
                                    ProfileEntity(
                                        id = profile?.id ?: 1, fullName = name, serviceNo = serviceNo, unit = unit,
                                        paySheetNo = paySheetNo, grade = grade, basicSalary = basic, otRate = ot, updatedAt = System.currentTimeMillis()
                                    )
                                )
                                onNavigateToClaimPeriod(name, serviceNo)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (profile == null) "SAVE PROFILE" else "SAVE & CONTINUE",
                                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // 1. BASE BACKGROUND & AURORA
        Box(modifier = Modifier.fillMaxSize().background(BaseWhiteBg)) {

            ProfileAuroraBackground(isVisible) // Ported directly from your Home Screen

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // 2. 3D TILT FROSTED HEADER
                InteractiveFrostedHeader(isVisible, name.ifBlank { "Nurse" })

                Spacer(modifier = Modifier.height(24.dp))

                // 3. PROFESSIONAL DETAILS GLASS CARD
                AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600, delayMillis = 150)) + fadeIn()) {
                    FrostedGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Vibrant Icon Box matching Home Screen modules
                                Box(
                                    modifier = Modifier.size(46.dp).background(Brush.linearGradient(listOf(BlueGradStart, BlueGradEnd)), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Identity & Placement", fontWeight = FontWeight.ExtraBold, color = DarkSlate, fontSize = 18.sp)
                                    Text("Professional credentials", color = LightSlate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)

                            FrostedTextField(value = name, onValueChange = { name = it }, label = "Full Name")

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FrostedTextField(value = serviceNo, onValueChange = { serviceNo = it }, label = "Service No", modifier = Modifier.weight(1f))
                                FrostedTextField(value = grade, onValueChange = { grade = it }, label = "Grade", modifier = Modifier.weight(1f))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FrostedTextField(value = unit, onValueChange = { unit = it }, label = "Unit/Ward", modifier = Modifier.weight(1f))
                                FrostedTextField(value = paySheetNo, onValueChange = { paySheetNo = it }, label = "Pay Sheet No", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 4. FINANCIAL DETAILS GLASS CARD
                AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600, delayMillis = 300)) + fadeIn()) {
                    FrostedGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Vibrant Icon Box matching Home Screen modules
                                Box(
                                    modifier = Modifier.size(46.dp).background(Brush.linearGradient(listOf(PurpleGradStart, PurpleGradEnd)), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Compensation Metrics", fontWeight = FontWeight.ExtraBold, color = DarkSlate, fontSize = 18.sp)
                                    Text("Used for accurate claim generation", color = LightSlate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)

                            FrostedTextField(
                                value = basicSalary, onValueChange = { basicSalary = it }, label = "Basic Salary",
                                keyboardType = KeyboardType.Number, prefix = "Rs."
                            )

                            FrostedTextField(
                                value = otRate, onValueChange = { otRate = it }, label = "Hourly OT Rate",
                                keyboardType = KeyboardType.Number, prefix = "Rs."
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// --- 3D TILT HEADER (Imported from your Home Screen) ---
@Composable
fun InteractiveFrostedHeader(isVisible: Boolean, displayName: String) {
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }

    val animatedRotX by animateFloatAsState(rotationX, spring(dampingRatio = 0.5f, stiffness = 200f), label = "")
    val animatedRotY by animateFloatAsState(rotationY, spring(dampingRatio = 0.5f, stiffness = 200f), label = "")
    val entranceAlpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(800), label = "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .alpha(entranceAlpha)
            .graphicsLayer {
                this.rotationX = animatedRotX
                this.rotationY = animatedRotY
                cameraDistance = 16f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { rotationX = 0f; rotationY = 0f },
                    onDragCancel = { rotationX = 0f; rotationY = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    rotationY += dragAmount.x * 0.15f
                    rotationX -= dragAmount.y * 0.15f
                    rotationY = rotationY.coerceIn(-20f, 20f)
                    rotationX = rotationX.coerceIn(-20f, 20f)
                }
            }
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.LightGray)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.85f)) // Frosted glass
            .border(1.dp, Color.White, RoundedCornerShape(32.dp))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.linearGradient(listOf(BlueGradStart, BlueGradEnd)), CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(displayName.firstOrNull()?.toString()?.uppercase() ?: "N", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Master Profile", fontSize = 14.sp, color = LightSlate, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = DarkSlate)
            }
        }
    }
}

// --- REUSABLE GLASS CARD ---
@Composable
fun FrostedGlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color.LightGray)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.85f)) // Frosted glass lets Aurora shine through
            .border(1.dp, Color.White, RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        content()
    }
}

// --- HOME SCREEN WATERCOLOR AURORA (Exact Math) ---
@Composable
fun ProfileAuroraBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val phase1 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "")
    val phase2 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "")

    val alphaAnim by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alphaAnim)
            .blur(radius = 120.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    ) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF64B5F6).copy(alpha = 0.3f), Color.Transparent), radius = w * 0.9f),
            center = Offset(w * 0.2f + (sin(phase1) * w * 0.3f).toFloat(), h * 0.2f + (cos(phase1.toDouble()) * h * 0.2f).toFloat())
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFBA68C8).copy(alpha = 0.25f), Color.Transparent), radius = w * 1.0f),
            center = Offset(w * 0.8f + (cos(phase2.toDouble()) * w * 0.2f).toFloat(), h * 0.5f + (sin(phase2) * h * 0.3f).toFloat())
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF4DD0E1).copy(alpha = 0.3f), Color.Transparent), radius = w * 0.8f),
            center = Offset(w * 0.5f + (sin(phase1 + phase2) * w * 0.4f).toFloat(), h * 0.9f)
        )
    }
}

// --- SOFT SQUISHY TEXT FIELDS ---
@Composable
fun FrostedTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text, prefix: String = ""
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = LightSlate, fontWeight = FontWeight.SemiBold) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = if (prefix.isNotEmpty()) { { Text(prefix, color = BlueGradEnd, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 8.dp)) } } else null,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = DarkSlate,
            unfocusedTextColor = DarkSlate,
            focusedContainerColor = Color.Black.copy(alpha = 0.03f), // Very subtle soft grey inside the glass
            unfocusedContainerColor = Color.Black.copy(alpha = 0.03f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
    )
}