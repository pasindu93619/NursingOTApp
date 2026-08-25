package com.pasindu.nursingotapp.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.NursingViewModel
import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// NEUTRAL / STATIC UI COLORS (unaffected by palette rotation)
// ============================================================

private val AppBackground = Color(0xFFF7F5FF)
private val PrimaryText = Color(0xFF0F172A)
private val SecondaryText = Color(0xFF475569)

// ============================================================
// ROTATING SOLID COLOR PALETTE SYSTEM
// ============================================================
// A new palette is picked at random every time the app is opened
// (cold start), persisted for that session via SharedPreferences.
// All colors are flat/solid — no gradients anywhere.

data class DashboardPalette(
    val avatar: Color,
    val claimForms: Color,
    val clinicalPlanning: Color,
    val knowledgeHub: Color,
    val calculators: Color,
    val finance: Color
)

private val DashboardPalettes = listOf(
    // 1. Coral / Purple — energetic
    DashboardPalette(
        avatar = Color(0xFFFF3D6E),
        claimForms = Color(0xFF2563EB),
        clinicalPlanning = Color(0xFF0D9488),
        knowledgeHub = Color(0xFFD6336C),
        calculators = Color(0xFF06B6D4),
        finance = Color(0xFF4F46E5)
    ),
    // 2. Emerald / Amber — fresh & warm
    DashboardPalette(
        avatar = Color(0xFF059669),
        claimForms = Color(0xFFDB2777),
        clinicalPlanning = Color(0xFF0891B2),
        knowledgeHub = Color(0xFFF59E0B),
        calculators = Color(0xFF7C3AED),
        finance = Color(0xFF16A34A)
    ),
    // 3. Sunset — bold red/orange/violet
    DashboardPalette(
        avatar = Color(0xFFEA580C),
        claimForms = Color(0xFFDC2626),
        clinicalPlanning = Color(0xFF9333EA),
        knowledgeHub = Color(0xFFEC4899),
        calculators = Color(0xFFF97316),
        finance = Color(0xFF7E22CE)
    ),
    // 4. Ocean — deep blues & teals
    DashboardPalette(
        avatar = Color(0xFF0284C7),
        claimForms = Color(0xFF0D9488),
        clinicalPlanning = Color(0xFF1D4ED8),
        knowledgeHub = Color(0xFF0EA5E9),
        calculators = Color(0xFF0891B2),
        finance = Color(0xFF3730A3)
    ),
    // 5. Berry — magenta/plum/rose
    DashboardPalette(
        avatar = Color(0xFFBE185D),
        claimForms = Color(0xFF9D174D),
        clinicalPlanning = Color(0xFF7C2D92),
        knowledgeHub = Color(0xFFDB2777),
        calculators = Color(0xFFC026D3),
        finance = Color(0xFF6D28D9)
    ),
    // 6. Citrus — lime/teal/gold, high contrast
    DashboardPalette(
        avatar = Color(0xFF65A30D),
        claimForms = Color(0xFF0369A1),
        clinicalPlanning = Color(0xFF15803D),
        knowledgeHub = Color(0xFFCA8A04),
        calculators = Color(0xFF0D9488),
        finance = Color(0xFF166534)
    )
)

private const val PALETTE_PREFS = "dashboard_palette_prefs"
private const val KEY_LAST_PALETTE_INDEX = "last_palette_index"

/**
 * Picks a random palette, avoiding immediate repeats of the last one used.
 * The chosen index is persisted so it's remembered as "this session's palette"
 * until the app is fully closed and reopened, at which point a new one is picked.
 */
private fun pickPaletteForThisLaunch(context: Context): DashboardPalette {
    val prefs = context.getSharedPreferences(PALETTE_PREFS, Context.MODE_PRIVATE)
    val lastIndex = prefs.getInt(KEY_LAST_PALETTE_INDEX, -1)

    val newIndex = if (DashboardPalettes.size > 1) {
        var candidate: Int
        do {
            candidate = DashboardPalettes.indices.random()
        } while (candidate == lastIndex)
        candidate
    } else {
        0
    }

    prefs.edit().putInt(KEY_LAST_PALETTE_INDEX, newIndex).apply()
    return DashboardPalettes[newIndex]
}

// Eye-catching clinical card animation effects
enum class CardEffect {
    NONE,
    WAVE,
    PARTICLES,
    ECG,
    BUBBLES,
    PULSE_RINGS
}

@Composable
fun HomeScreen(
    viewModel: NursingViewModel,
    onNavigate: (String) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    val displayFullName =
        userProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: "Nursing Officer"

    val shortName =
        displayFullName.split(" ").lastOrNull()
            ?: displayFullName

    val initial =
        displayFullName.firstOrNull()
            ?.toString()
            ?.uppercase()
            ?: "P"

    val scrollState = rememberScrollState()

    // Picked ONCE per cold app launch (persisted in SharedPreferences),
    // so it stays stable while the app is open but changes next time
    // the user closes and reopens the app.
    val context = LocalContext.current
    val palette = remember { pickPaletteForThisLaunch(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        // ====================================================
        // PROFILE / GREETING CARD
        // ====================================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = palette.avatar.copy(alpha = 0.30f)
                )
                .clickable {
                    onNavigate("profile")
                },
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Solid color avatar (no gradient)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            palette.avatar,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Hello, $shortName 👋",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (userProfile == null) {
                            "Tap here to setup profile & SLNC"
                        } else {
                            "Ward 17 In-Charge Dashboard"
                        },
                        fontSize = 13.sp,
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Small profile arrow
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            palette.avatar.copy(alpha = 0.10f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open profile",
                        tint = palette.avatar,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ====================================================
        // CORE LEGACY MODULE
        // ====================================================

        Text(
            text = "Core Legacy Module",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = PrimaryText
        )

        // OT & Claim Forms
        AnimatedDashboardCard(
            title = "OT & Claim Forms",
            subtitle = "A4 Multi-page Claims & 36h Rule Engine",
            icon = Icons.Default.Description,
            color = palette.claimForms,
            height = 140.dp,
            effect = CardEffect.WAVE,
            onClick = {
                onNavigate("profile")
            }
        )

        // ====================================================
        // SUPER APP ENHANCEMENTS
        // ====================================================

        Text(
            text = "Super App Enhancements",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SecondaryText
        )

        // ====================================================
        // STAGGERED DASHBOARD
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==================================================
            // LEFT COLUMN
            // ==================================================

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ------------------------------------------------
                // CLINICAL PLANNING
                // ------------------------------------------------

                AnimatedDashboardCard(
                    title = "Clinical Planning",
                    subtitle = "ISBAR Handover & Task Alarms",
                    icon = Icons.Default.Assignment,
                    color = palette.clinicalPlanning,
                    height = 200.dp,
                    effect = CardEffect.BUBBLES,
                    onClick = {
                        onNavigate("clinical_planning")
                    }
                )

                // ------------------------------------------------
                // KNOWLEDGE HUB
                // ------------------------------------------------

                AnimatedDashboardCard(
                    title = "Knowledge Hub",
                    subtitle = "CPD Ledger & MoH Circulars",
                    icon = Icons.Default.MenuBook,
                    color = palette.knowledgeHub,
                    height = 180.dp,
                    effect = CardEffect.PARTICLES,
                    onClick = {
                        onNavigate("knowledge_hub")
                    }
                )
            }

            // ==================================================
            // RIGHT COLUMN
            // ==================================================

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ------------------------------------------------
                // CLINICAL CALCULATORS
                // ------------------------------------------------

                AnimatedDashboardCard(
                    title = "Clinical Calculators",
                    subtitle = "IV Drip Metronome & GCS",
                    icon = Icons.Default.MedicalServices,
                    color = palette.calculators,
                    height = 180.dp,
                    effect = CardEffect.ECG,
                    onClick = {
                        onNavigate("clinical_calculators")
                    }
                )

                // ------------------------------------------------
                // ADVANCED FINANCE
                // ------------------------------------------------

                AnimatedDashboardCard(
                    title = "Advanced Finance",
                    subtitle = "Vico Charts, APIT & Loans",
                    icon = Icons.Default.AccountBalance,
                    color = palette.finance,
                    height = 200.dp,
                    effect = CardEffect.PULSE_RINGS,
                    onClick = {
                        onNavigate("financial_dashboard")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}


// ============================================================
// ANIMATED DASHBOARD CARD — solid color background
// ============================================================

@Composable
fun AnimatedDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    height: Dp,
    textColor: Color = Color.White,
    isSoon: Boolean = false,
    effect: CardEffect = CardEffect.NONE,
    onClick: () -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "CardEffectsAnimation"
        )

    // Subtle breathing animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Animation phase
    val timePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "TimePhase"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(
                if (effect != CardEffect.NONE) scale
                else 1f
            )
            .shadow(
                elevation = if (isSoon) 3.dp else 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = color.copy(alpha = 0.35f)
            )
            .clickable(
                enabled = !isSoon,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color)
        ) {

            // ==================================================
            // CANVAS ANIMATIONS
            // ==================================================

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(24.dp)
                    )
            ) {

                val w = size.width
                val h = size.height

                when (effect) {

                    // ==========================================
                    // WAVE
                    // ==========================================

                    CardEffect.WAVE -> {

                        val path1 = Path()
                        val path2 = Path()
                        val path3 = Path()

                        path1.moveTo(
                            0f,
                            h * 0.50f
                        )

                        path2.moveTo(
                            0f,
                            h * 0.60f
                        )

                        path3.moveTo(
                            0f,
                            h * 0.70f
                        )

                        for (x in 0..w.toInt() step 5) {

                            val y1: Float =
                                (h * 0.50f +
                                        sin(
                                            (x * 0.02) +
                                                    timePhase
                                        ) *
                                        (h * 0.15f)).toFloat()

                            val y2: Float =
                                (h * 0.60f +
                                        sin(
                                            (x * 0.015) -
                                                    timePhase
                                        ) *
                                        (h * 0.10f)).toFloat()

                            val y3: Float =
                                (h * 0.70f +
                                        sin(
                                            (x * 0.01) +
                                                    timePhase * 1.5
                                        ) *
                                        (h * 0.08f)).toFloat()

                            path1.lineTo(
                                x.toFloat(),
                                y1
                            )

                            path2.lineTo(
                                x.toFloat(),
                                y2
                            )

                            path3.lineTo(
                                x.toFloat(),
                                y3
                            )
                        }

                        path1.lineTo(w, h)
                        path1.lineTo(0f, h)
                        path1.close()

                        path2.lineTo(w, h)
                        path2.lineTo(0f, h)
                        path2.close()

                        path3.lineTo(w, h)
                        path3.lineTo(0f, h)
                        path3.close()

                        drawPath(
                            path1,
                            color = Color.White.copy(
                                alpha = 0.08f
                            )
                        )

                        drawPath(
                            path2,
                            color = Color.White.copy(
                                alpha = 0.12f
                            )
                        )

                        drawPath(
                            path3,
                            color = Color.White.copy(
                                alpha = 0.16f
                            )
                        )
                    }

                    // ==========================================
                    // ECG
                    // ==========================================

                    CardEffect.ECG -> {

                        val path = Path()

                        val centerY =
                            h * 0.65f

                        val progress =
                            (timePhase / (2 * Math.PI))
                                .toFloat()

                        val currentX =
                            w * progress

                        path.moveTo(
                            0f,
                            centerY
                        )

                        path.lineTo(
                            w * 0.20f,
                            centerY
                        )

                        path.lineTo(
                            w * 0.25f,
                            centerY - 20f
                        )

                        path.lineTo(
                            w * 0.30f,
                            centerY
                        )

                        path.lineTo(
                            w * 0.40f,
                            centerY
                        )

                        path.lineTo(
                            w * 0.45f,
                            centerY + 15f
                        )

                        path.lineTo(
                            w * 0.50f,
                            centerY - h * 0.40f
                        )

                        path.lineTo(
                            w * 0.55f,
                            centerY + 25f
                        )

                        path.lineTo(
                            w * 0.60f,
                            centerY
                        )

                        path.lineTo(
                            w * 0.70f,
                            centerY
                        )

                        path.lineTo(
                            w * 0.80f,
                            centerY - 30f
                        )

                        path.lineTo(
                            w * 0.90f,
                            centerY
                        )

                        path.lineTo(
                            w,
                            centerY
                        )

                        drawPath(
                            path = path,
                            color = Color.White.copy(
                                alpha = 0.30f
                            ),
                            style = Stroke(
                                width = 4f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(
                                currentX,
                                centerY
                            )
                        )
                    }

                    // ==========================================
                    // BUBBLES
                    // ==========================================

                    CardEffect.BUBBLES -> {

                        for (i in 0..6) {

                            val phaseOffset =
                                i * (Math.PI / 3)

                            val bubbleY =
                                h -
                                        (
                                                (
                                                        timePhase +
                                                                phaseOffset
                                                        ) %
                                                        (2 * Math.PI)
                                                )
                                            .toFloat() /
                                        (2 * Math.PI)
                                            .toFloat() *
                                        (h + 50f)

                            val bubbleX =
                                w *
                                        (
                                                0.2f +
                                                        0.1f * i
                                                ) +
                                        sin(
                                            timePhase * 2 +
                                                    i
                                        ) *
                                        20f

                            drawCircle(
                                color = Color.White.copy(
                                    alpha = 0.12f
                                ),
                                radius = 8f + i * 2,
                                center = Offset(
                                    bubbleX,
                                    bubbleY
                                )
                            )
                        }
                    }

                    // ==========================================
                    // PARTICLES
                    // ==========================================

                    CardEffect.PARTICLES -> {

                        val center =
                            Offset(
                                w * 0.85f,
                                h * 0.15f
                            )

                        val radius =
                            w * 0.45f

                        val orbitDegrees =
                            Math.toDegrees(
                                timePhase.toDouble()
                            ).toFloat()

                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color.White.copy(
                                        alpha = 0.30f
                                    ),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius
                            ),
                            center = center,
                            radius = radius
                        )

                        for (i in 0..4) {

                            val angle =
                                Math.toRadians(
                                    (
                                            orbitDegrees +
                                                    i * 72f
                                            ).toDouble()
                                )

                            val px =
                                center.x +
                                        (
                                                radius *
                                                        0.6f *
                                                        cos(angle)
                                                ).toFloat()

                            val py =
                                center.y +
                                        (
                                                radius *
                                                        0.6f *
                                                        sin(angle)
                                                ).toFloat()

                            drawCircle(
                                color = Color.White.copy(
                                    alpha = 0.85f
                                ),
                                radius = 4.5f,
                                center = Offset(
                                    px,
                                    py
                                )
                            )
                        }
                    }

                    // ==========================================
                    // PULSE RINGS
                    // ==========================================

                    CardEffect.PULSE_RINGS -> {

                        val center =
                            Offset(
                                w * 0.80f,
                                h * 0.80f
                            )

                        val maxRadius =
                            w * 0.60f

                        val progress1 =
                            (
                                    timePhase /
                                            (2 * Math.PI)
                                    ).toFloat()

                        val progress2 =
                            (
                                    (
                                            timePhase +
                                                    Math.PI
                                            ) %
                                            (2 * Math.PI) /
                                            (2 * Math.PI)
                                    ).toFloat()

                        drawCircle(
                            color = Color.White.copy(
                                alpha =
                                    0.25f *
                                            (1f - progress1)
                            ),
                            radius =
                                maxRadius *
                                        progress1,
                            center = center,
                            style = Stroke(
                                width = 4f
                            )
                        )

                        drawCircle(
                            color = Color.White.copy(
                                alpha =
                                    0.25f *
                                            (1f - progress2)
                            ),
                            radius =
                                maxRadius *
                                        progress2,
                            center = center,
                            style = Stroke(
                                width = 4f
                            )
                        )
                    }

                    CardEffect.NONE -> Unit
                }
            }

            // ==================================================
            // FOREGROUND CONTENT
            // ==================================================

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement =
                    Arrangement.SpaceBetween
            ) {

                // ==============================================
                // ICON
                // ==============================================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Color.White.copy(
                                    alpha = 0.18f
                                ),
                                CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(
                                    alpha = 0.35f
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    if (isSoon) {

                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                        ) {

                            Text(
                                text = "SOON",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryText
                            )
                        }
                    }
                }

                // ==============================================
                // CARD TEXT
                // ==============================================

                Column {

                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        lineHeight = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(
                            alpha = 0.86f
                        ),
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}