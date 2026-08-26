package com.pasindu.nursingotapp.ui.screens
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CareBg = Color(0xFFF6F8FF)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)
private val Indigo = Color(0xFF4F46E5)
private val Violet = Color(0xFF7C3AED)
private val Cyan = Color(0xFF06B6D4)
private val Teal = Color(0xFF0F766E)
private val Green = Color(0xFF10B981)
private val Orange = Color(0xFFF59E0B)
private val Red = Color(0xFFEF4444)
private val Line = Color(0xFFE2E8F0)

@Composable
fun CarePulseScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CareBg)
    ) {

        // =====================================================
        // STABLE CUSTOM HEADER
        // No TopAppBar / No ExperimentalMaterial3Api
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CareBg)
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Indigo
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CarePulse",
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Your nursing shift, at a glance",
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                color = Indigo.copy(alpha = 0.10f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "CARE",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    color = Indigo,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = 18.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            CarePulseHero()

            SectionTitle(
                eyebrow = "LIVE SNAPSHOT",
                title = "Everything important. One glance."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                PulseMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "OT Hours",
                    value = "24.0 h",
                    icon = Icons.Default.Schedule,
                    accent = Violet
                )

                PulseMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Tasks",
                    value = "7",
                    icon = Icons.Default.HealthAndSafety,
                    accent = Teal
                )

                PulseMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Risk",
                    value = "Low",
                    icon = Icons.Default.WarningAmber,
                    accent = Green
                )
            }

            GlassActionCard(
                title = "Clinical Command Center",
                subtitle =
                    "Jump into calculators, monitoring and high-priority nursing tools.",
                icon = Icons.Default.MedicalServices,
                accent = Indigo,
                effect = 0,
                onClick = {
                    onNavigate("clinical_calculators")
                }
            )

            GlassActionCard(
                title = "Shift Intelligence",
                subtitle =
                    "Review workload, claims, trends and your monthly financial picture.",
                icon = Icons.Default.Analytics,
                accent = Cyan,
                effect = 1,
                onClick = {
                    onNavigate("analytics")
                }
            )

            // =================================================
            // ADVANCED FINANCE
            // =================================================

            GlassActionCard(
                title = "Advanced Finance",
                subtitle =
                    "Salary projections, OT, deductions and smart financial planning.",
                icon = Icons.Default.AccountBalance,
                accent = Orange,
                effect = 2,
                onClick = {
                    onNavigate("advanced_finance_hub")
                }
            )

            SectionTitle(
                eyebrow = "NURSE MODE",
                title = "Fast actions for the ward"
            )

            val actions = listOf(
                "ISBAR Handover" to "clinical_planning",
                "Knowledge Hub" to "knowledge_hub",
                "Emergency Calculators" to "emergency_calcs",
                "Vasoactive Infusions" to "vasoactive_infusions"
            )

            actions.forEachIndexed { index, action ->

                val label = action.first
                val route = action.second

                ActionRow(
                    label = label,
                    selected = selectedIndex == index,
                    accent = listOf(
                        Indigo,
                        Violet,
                        Red,
                        Cyan
                    )[index],
                    onClick = {
                        selectedIndex = index
                        onNavigate(action.second)
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(26.dp)
            )
        }
    }
}
@Composable
private fun CarePulseHero() {

    val transition =
        rememberInfiniteTransition(
            label = "carePulseHero"
        )

    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroPulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(30.dp),
                spotColor = Indigo.copy(alpha = 0.18f)
            ),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(30.dp)
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF101B4D),
                            Indigo,
                            Violet
                        )
                    )
                )
                .padding(22.dp)
        ) {

            Canvas(
                modifier = Modifier.matchParentSize()
            ) {

                val y =
                    size.height * 0.72f

                val path =
                    Path().apply {
                        moveTo(0f, y)
                        lineTo(
                            size.width * 0.16f,
                            y
                        )
                        lineTo(
                            size.width * 0.22f,
                            y - 10f
                        )
                        lineTo(
                            size.width * 0.27f,
                            y + 7f
                        )
                        lineTo(
                            size.width * 0.33f,
                            y - 32f
                        )
                        lineTo(
                            size.width * 0.40f,
                            y + 3f
                        )
                        lineTo(
                            size.width * 0.47f,
                            y
                        )
                        lineTo(
                            size.width,
                            y
                        )
                    }

                drawPath(
                    path = path,
                    color = Color.White.copy(
                        alpha = 0.20f
                    ),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round
                    )
                )
            }

            Column {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Surface(
                            color = Color.White.copy(
                                alpha = 0.14f
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "NURSING OFFICER MODE",
                                color = Color.White.copy(
                                    alpha = 0.85f
                                ),
                                fontSize = 10.sp,
                                fontWeight =
                                    FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 6.dp
                                )
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "Stay ahead of the shift.",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight =
                                FontWeight.Black,
                            lineHeight = 30.sp
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "Clinical tools, workload signals and finance — brought into one fast command center.",
                            color = Color.White.copy(
                                alpha = 0.78f
                            ),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(
                                62.dp * pulse
                            )
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = 0.12f
                                )
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.MonitorHeart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Surface(
                    color = Color.White.copy(
                        alpha = 0.10f
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        SmallHeroStat(
                            value = "6",
                            label = "active tools"
                        )

                        SmallHeroStat(
                            value = "24h",
                            label = "shift view"
                        )

                        SmallHeroStat(
                            value = "98%",
                            label = "task readiness"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallHeroStat(
    value: String,
    label: String
) {
    Column {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = label,
            color = Color.White.copy(
                alpha = 0.62f
            ),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun SectionTitle(
    eyebrow: String,
    title: String
) {
    Column {

        Text(
            text = eyebrow,
            color = Indigo,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = title,
            color = Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun PulseMetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Line
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        accent.copy(alpha = 0.10f)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                color = Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = value,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun GlassActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    effect: Int,
    onClick: () -> Unit
) {

    val transition =
        rememberInfiniteTransition(
            label = "action-$title"
        )

    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3600 +
                        (effect * 500),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor =
                    accent.copy(alpha = 0.16f)
            )
            .clickable(
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            Box(
                modifier = Modifier
                    .size(115.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(
                                    alpha =
                                        0.14f +
                                                drift * 0.05f
                                ),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(
                            RoundedCornerShape(17.dp)
                        )
                        .background(
                            accent.copy(
                                alpha = 0.10f
                            )
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = subtitle,
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Icon(
                    imageVector =
                        Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = if (selected) {
                accent.copy(alpha = 0.10f)
            } else {
                Color.White
            },
            tonalElevation = 1.dp
        ) {

            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector =
                        Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}