package com.pasindu.nursingotapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos

// Data class for our Super App Modules
data class AppModule(
    val title: String,
    val subtitle: String,
    val icon: String,
    val color1: Color,
    val color2: Color,
    val route: String,
    val height: Int,
    val isAvailable: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NursingViewModel,
    onNavigate: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState(initial = null)
    var isVisible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Modern Staggered Modules
    val modules = listOf(
        AppModule("OT & Forms", "A4 Claim PDFs", "📄", Color(0xFF1E88E5), Color(0xFF00BCD4), "claim_period", 220),
        AppModule("Clinical Tools", "Math Engine", "🏥", Color(0xFF8E24AA), Color(0xFFAB47BC), "clinical_tools", 180),
        AppModule("Smart Roster", "Digital Diary", "📅", Color(0xFF43A047), Color(0xFF66BB6A), "roster", 190, isAvailable = false),
        AppModule("Smart Insights", "Burnout & Trends", "📊", Color(0xFFE53935), Color(0xFFFF5252), "analytics", 230),
        AppModule("Pro Hub", "MOH & CNE", "🎓", Color(0xFFF57C00), Color(0xFFFFB74D), "hub", 170, isAvailable = false)
    )

    // Clean, crisp white base background
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {

        // --- 1. DAYTIME WATERCOLOR BACKGROUND ---
        DaylightAuroraBackground(isVisible)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. FROSTED GLASS HEADER ---
            FrostedGlassHeader(isVisible, userProfile?.fullName)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Your Workspace",
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B), // Dark Slate color for light theme
                modifier = Modifier.graphicsLayer { alpha = if (isVisible) 1f else 0f }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- 3. STAGGERED PINTEREST-STYLE GRID ---
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 16.dp,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                itemsIndexed(modules) { index, module ->
                    AnimatedStaggeredCard(
                        module = module,
                        index = index,
                        isVisible = isVisible,
                        onClick = {
                            if (module.isAvailable) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigate(module.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- LIGHT THEME COMPONENTS ---

@Composable
fun DaylightAuroraBackground(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val phase1 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(25000, easing = LinearEasing)), label = "")
    val phase2 by infiniteTransition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "")

    val alphaAnim by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, tween(2000), label = "")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alphaAnim)
            .blur(radius = 120.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) // Massive blur for watercolor effect
    ) {
        val w = size.width
        val h = size.height

        // Soft pastel colors for daytime
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

@Composable
fun FrostedGlassHeader(isVisible: Boolean, fullName: String?) {
    // 3D Tilt Physics
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }

    val animatedRotX by animateFloatAsState(rotationX, spring(dampingRatio = 0.5f, stiffness = 200f), label = "")
    val animatedRotY by animateFloatAsState(rotationY, spring(dampingRatio = 0.5f, stiffness = 200f), label = "")

    val entranceAlpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(800), label = "")
    val entranceOffset by animateDpAsState(if (isVisible) 0.dp else (-50).dp, tween(800, easing = FastOutSlowInEasing), label = "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(entranceAlpha)
            .offset(y = entranceOffset)
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
            .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color.LightGray, spotColor = Color.LightGray) // Soft shadow
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.85f)) // Solid frosted glass
            .border(1.dp, Color.White, RoundedCornerShape(32.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF00BCD4), Color(0xFF1E88E5))), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(fullName?.firstOrNull()?.toString() ?: "N", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Hello, ${fullName?.split(" ")?.firstOrNull() ?: "Nurse"} 👋", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B)) // Dark Slate
                Text("Ready to save lives?", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold) // Light Slate
            }
        }
    }
}

@Composable
fun AnimatedStaggeredCard(module: AppModule, index: Int, isVisible: Boolean, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    // Staggered Spring Entrance
    val entranceScale by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, spring(0.6f, Spring.StiffnessMedium), label = "")
    val entranceOffset by animateDpAsState(targetValue = if (isVisible) 0.dp else 150.dp, tween(700, index * 100, FastOutSlowInEasing), label = "")

    // "Squish" Physics
    val scaleX by animateFloatAsState(targetValue = if (isPressed) 1.05f else 1f, spring(0.4f), label = "")
    val scaleY by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, spring(0.4f), label = "")

    // Vibrant background for active cards, soft gray for inactive ones
    val bgGradient = if (module.isAvailable) {
        Brush.linearGradient(listOf(module.color1, module.color2))
    } else {
        Brush.linearGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))) // Light grays
    }

    val textColor = if (module.isAvailable) Color.White else Color(0xFF64748B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(module.height.dp)
            .offset(y = entranceOffset)
            .graphicsLayer { this.scaleX = entranceScale * scaleX; this.scaleY = entranceScale * scaleY }
            .shadow(
                elevation = if (module.isAvailable) 16.dp else 0.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = module.color1, // Colored drop shadows!
                spotColor = module.color2
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (module.isAvailable) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
        ) {
            // Background abstract decoration inside the card
            Canvas(modifier = Modifier.fillMaxSize()) {
                val circleColor = if (module.isAvailable) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.03f)
                drawCircle(color = circleColor, radius = size.width * 0.8f, center = Offset(size.width, 0f))
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Icon and Badge
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (module.isAvailable) Color.White.copy(alpha = 0.25f) else Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(module.icon, fontSize = 26.sp)
                    }

                    if (!module.isAvailable) {
                        Box(modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("SOON", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Bottom section: Text
                Column {
                    Text(
                        text = module.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        color = textColor, lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = module.subtitle,
                        fontSize = 13.sp, color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}