package com.pasindu.nursingotapp.ui.screens.emergency

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
//  COLOUR PALETTE  (Premium Glassmorphism — matches your existing theme)
// ─────────────────────────────────────────────────────────────────────────────
private object MoAColors {
    // Organs – base fills
    val heartRed        = Color(0xFFE53935)
    val heartRedDark    = Color(0xFFB71C1C)
    val lungPink        = Color(0xFFEF9A9A)
    val lungPinkDark    = Color(0xFFE57373)
    val brainPurple     = Color(0xFFCE93D8)
    val brainPurpleDark = Color(0xFF9C27B0)
    val kidneyBrown     = Color(0xFFBCAAA4)
    val kidneyBrownDark = Color(0xFF795548)
    val liverBrown      = Color(0xFFD7946A)
    val liverBrownDark  = Color(0xFFA0522D)
    val vesselRed       = Color(0xFFEF5350)
    val vesselBlue      = Color(0xFF42A5F5)

    // Drug highlights
    val epiYellow       = Color(0xFFFFD600)
    val epiOrange       = Color(0xFFFF6D00)
    val amioBlue        = Color(0xFF1565C0)
    val amioLightBlue   = Color(0xFF42A5F5)
    val naloxoneGreen   = Color(0xFF00C853)
    val alteplasePurple = Color(0xFF7B1FA2)
    val alteplasePink   = Color(0xFFE040FB)
    val adenosineGold   = Color(0xFFFFAB00)
    val atropineOrange  = Color(0xFFFF6F00)
    val dopamineBlue    = Color(0xFF0288D1)
    val magBlue         = Color(0xFF00BCD4)
    val norepBlue       = Color(0xFF1A237E)

    // Particles / glow
    val glowWhite       = Color(0x55FFFFFF)
    val particleWhite   = Color(0xCCFFFFFF)
}

// ─────────────────────────────────────────────────────────────────────────────
//  DRUG ENUM  (must match your existing EmergencyDrug / MoAPathway enum)
// ─────────────────────────────────────────────────────────────────────────────
enum class MoADrug {
    EPINEPHRINE, AMIODARONE, NALOXONE, ALTEPLASE,
    ADENOSINE, ATROPINE, DOPAMINE, MAGNESIUM_SULFATE,
    NOREPINEPHRINE, VASOPRESSIN
}

// ─────────────────────────────────────────────────────────────────────────────
//  TOP-LEVEL COMPOSABLE
//  Drop this wherever you currently place your MoA animation panel.
//
//  Usage:
//    MoAOrganAnimation(drug = MoADrug.EPINEPHRINE, modifier = Modifier.size(280.dp))
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MoAOrganAnimation(
    drug: MoADrug,
    modifier: Modifier = Modifier.size(280.dp, 200.dp)
) {
    when (drug) {
        MoADrug.EPINEPHRINE       -> EpinephrineAnimation(modifier)
        MoADrug.AMIODARONE        -> AmiodaroneAnimation(modifier)
        MoADrug.NALOXONE          -> NaloxoneAnimation(modifier)
        MoADrug.ALTEPLASE         -> AlteplaseAnimation(modifier)
        MoADrug.ADENOSINE         -> AdenosineAnimation(modifier)
        MoADrug.ATROPINE          -> AtropineAnimation(modifier)
        MoADrug.DOPAMINE          -> DopamineAnimation(modifier)
        MoADrug.MAGNESIUM_SULFATE -> MagnesiumAnimation(modifier)
        MoADrug.NOREPINEPHRINE    -> NorepinephrineAnimation(modifier)
        MoADrug.VASOPRESSIN       -> VasopressinAnimation(modifier)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  1.  EPINEPHRINE  — heart + lungs  (β1 → ↑HR/contractility, β2 → bronchodilation)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun EpinephrineAnimation(modifier: Modifier = Modifier) {

    // Heart beat – fast (140 bpm feel)
    val heartBeat = rememberInfiniteTransition(label = "epi_beat")
    val beatScale by heartBeat.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 430
                1f       at 0    using FastOutSlowInEasing
                1.18f    at 130  using FastOutLinearInEasing
                1f       at 260  using LinearEasing
                1f       at 430
            }, repeatMode = RepeatMode.Restart
        ), label = "beat_scale"
    )

    // Glow pulse
    val glowAlpha by heartBeat.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    // Lung expansion
    val lungScale by heartBeat.animateFloat(
        initialValue = 0.92f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "lung_scale"
    )

    // Particle travel along vessels
    val particlePos by heartBeat.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "particle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // ── Background vessels (aorta + pulmonary) ──────────────────────────
        drawVesselArtery(cx, cy, w, glowAlpha)

        // ── Lungs (left + right) ────────────────────────────────────────────
        withTransform({ scale(lungScale, lungScale, pivot = Offset(cx * 0.45f, cy)) }) {
            drawLung(isLeft = true, cx = cx, cy = cy, color = MoAColors.lungPink,
                highlight = MoAColors.epiOrange, highlightAlpha = glowAlpha * 0.6f)
        }
        withTransform({ scale(lungScale, lungScale, pivot = Offset(cx * 1.55f, cy)) }) {
            drawLung(isLeft = false, cx = cx, cy = cy, color = MoAColors.lungPink,
                highlight = MoAColors.epiOrange, highlightAlpha = glowAlpha * 0.6f)
        }

        // ── Heart (centre) ───────────────────────────────────────────────────
        withTransform({ scale(beatScale, beatScale, pivot = Offset(cx, cy)) }) {
            drawHeart(cx = cx, cy = cy,
                baseColor  = MoAColors.heartRed,
                darkColor  = MoAColors.heartRedDark,
                glowColor  = MoAColors.epiYellow,
                glowAlpha  = glowAlpha)
        }

        // ── Travelling particle (blood bolus) ────────────────────────────────
        val px = cx + cos(particlePos * 2 * PI.toFloat()) * (w * 0.28f)
        val py = cy + sin(particlePos * 2 * PI.toFloat()) * (h * 0.22f)
        drawCircle(color = MoAColors.epiYellow, radius = 5.dp.toPx(),
            center = Offset(px, py), alpha = 0.85f)
        drawCircle(color = MoAColors.particleWhite, radius = 2.5f.dp.toPx(),
            center = Offset(px, py))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  2.  AMIODARONE  — heart only (K⁺ channel block → ↓HR, stabilises rhythm)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AmiodaroneAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "amio")

    // Slow, steady beat (60 bpm feel)
    val beatScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f    at 0   using FastOutSlowInEasing
                1.10f at 300 using FastOutLinearInEasing
                1f    at 600
                1f    at 1000
            }, repeatMode = RepeatMode.Restart
        ), label = "amio_beat"
    )

    // Ion channel wave sweeping across heart
    val waveProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave"
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        // ECG-style rhythm line behind heart
        drawEcgLine(cx, cy, w, h, waveProgress,
            color = MoAColors.amioLightBlue, alpha = 0.7f)

        withTransform({ scale(beatScale, beatScale, pivot = Offset(cx, cy)) }) {
            drawHeart(cx = cx, cy = cy,
                baseColor = MoAColors.heartRed,
                darkColor = MoAColors.heartRedDark,
                glowColor = MoAColors.amioBlue,
                glowAlpha = glowAlpha)
        }

        // K⁺ ion particles orbiting heart
        repeat(6) { i ->
            val angle = (waveProgress * 2 * PI + i * PI / 3).toFloat()
            val rx = cx + cos(angle) * (w * 0.20f)
            val ry = cy + sin(angle) * (h * 0.15f)
            drawCircle(color = MoAColors.amioLightBlue, radius = 4.dp.toPx(),
                center = Offset(rx, ry), alpha = 0.8f)
            // "K⁺" label approximated as small cross marker
            drawLine(MoAColors.particleWhite, Offset(rx - 3.dp.toPx(), ry),
                Offset(rx + 3.dp.toPx(), ry), strokeWidth = 1.5f.dp.toPx())
            drawLine(MoAColors.particleWhite, Offset(rx, ry - 3.dp.toPx()),
                Offset(rx, ry + 3.dp.toPx()), strokeWidth = 1.5f.dp.toPx())
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  3.  NALOXONE  — brain + lungs  (μ-opioid receptor antagonist → ↑RR)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun NaloxoneAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "nalox")

    // Lungs starting collapsed, expanding as naloxone works
    val lungExpand by transition.animateFloat(
        initialValue = 0.70f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "lung_expand"
    )

    // Brain "wake-up" pulse
    val brainGlow by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "brain_glow"
    )

    // Receptor block particles
    val blockPos by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "block"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        // Brain at top
        drawBrain(cx = cx, cy = cy * 0.55f,
            color = MoAColors.brainPurple,
            darkColor = MoAColors.brainPurpleDark,
            glowColor = MoAColors.naloxoneGreen,
            glowAlpha = brainGlow * 0.8f)

        // Receptor block indicator on brain
        val bx = cx + 20.dp.toPx()
        val by2 = cy * 0.55f - 15.dp.toPx()
        drawCircle(color = MoAColors.naloxoneGreen, radius = 7.dp.toPx(),
            center = Offset(bx, by2), alpha = 0.9f)
        // "X" cross on receptor
        val xs = 4.dp.toPx()
        drawLine(Color.White, Offset(bx - xs, by2 - xs), Offset(bx + xs, by2 + xs),
            strokeWidth = 2.dp.toPx())
        drawLine(Color.White, Offset(bx + xs, by2 - xs), Offset(bx - xs, by2 + xs),
            strokeWidth = 2.dp.toPx())

        // Lungs at bottom — expanding
        withTransform({ scale(lungExpand, lungExpand, pivot = Offset(cx * 0.55f, cy * 1.6f)) }) {
            drawLung(isLeft = true, cx = cx, cy = cy * 1.6f,
                color = MoAColors.lungPink, highlight = MoAColors.naloxoneGreen,
                highlightAlpha = (1f - lungExpand + 0.3f).coerceIn(0f, 1f))
        }
        withTransform({ scale(lungExpand, lungExpand, pivot = Offset(cx * 1.45f, cy * 1.6f)) }) {
            drawLung(isLeft = false, cx = cx, cy = cy * 1.6f,
                color = MoAColors.lungPink, highlight = MoAColors.naloxoneGreen,
                highlightAlpha = (1f - lungExpand + 0.3f).coerceIn(0f, 1f))
        }

        // Travelling antagonist molecule (brain → lung path)
        val t = blockPos
        val molX = cx + (t - 0.5f) * w * 0.3f
        val molY = cy * 0.55f + t * cy * 1.1f
        drawCircle(color = MoAColors.naloxoneGreen, radius = 6.dp.toPx(),
            center = Offset(molX, molY), alpha = 0.9f)
        drawCircle(color = Color.White, radius = 3.dp.toPx(),
            center = Offset(molX, molY))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  4.  ALTEPLASE  — vessels (tPA → fibrinolysis → clot dissolving)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AlteplaseAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "alteplase")

    val dissolveProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "dissolve"
    )

    val glowPulse by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        // Main vessel (coronary artery or pulmonary trunk)
        val vesselPath = Path().apply {
            moveTo(cx - w * 0.38f, cy)
            cubicTo(cx - w * 0.20f, cy - h * 0.15f,
                cx + w * 0.10f, cy + h * 0.15f,
                cx + w * 0.38f, cy)
        }
        drawPath(vesselPath, color = MoAColors.vesselRed.copy(alpha = 0.5f),
            style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round))

        // Clot (dark red blockage)
        val clotCx = cx - w * 0.08f
        val clotAlpha = (1f - dissolveProgress * 1.3f).coerceIn(0f, 1f)
        if (clotAlpha > 0f) {
            drawOval(color = MoAColors.heartRedDark.copy(alpha = clotAlpha),
                topLeft = Offset(clotCx - 22.dp.toPx(), cy - 14.dp.toPx()),
                size = Size(44.dp.toPx(), 28.dp.toPx()))
            // fibrin strands
            repeat(4) { i ->
                val fy = cy - 10.dp.toPx() + i * 7.dp.toPx()
                drawLine(Color(0xFF8B0000).copy(alpha = clotAlpha),
                    Offset(clotCx - 18.dp.toPx(), fy),
                    Offset(clotCx + 18.dp.toPx(), fy),
                    strokeWidth = 1.5f.dp.toPx())
            }
        }

        // tPA enzyme particles dissolving clot
        if (dissolveProgress > 0.1f) {
            repeat(8) { i ->
                val angle = dissolveProgress * 2 * PI + i * PI / 4
                val r = dissolveProgress * 30.dp.toPx()
                val px = clotCx + cos(angle).toFloat() * r
                val py = cy + sin(angle).toFloat() * r * 0.6f
                drawCircle(color = MoAColors.alteplasePink.copy(alpha = glowPulse * 0.7f),
                    radius = 4.dp.toPx(), center = Offset(px, py))
            }
        }

        // Blood flow restored — travelling pulse after dissolve
        if (dissolveProgress > 0.5f) {
            val flowAlpha = ((dissolveProgress - 0.5f) * 2f).coerceIn(0f, 1f)
            val flowX = cx - w * 0.38f + dissolveProgress * w * 0.76f
            drawCircle(color = MoAColors.alteplasePurple.copy(alpha = flowAlpha),
                radius = 8.dp.toPx(), center = Offset(flowX, cy))
            drawCircle(color = Color.White.copy(alpha = flowAlpha * 0.8f),
                radius = 4.dp.toPx(), center = Offset(flowX, cy))
        }

        // Glow halo around vessel when flow restored
        if (dissolveProgress > 0.7f) {
            val haloAlpha = ((dissolveProgress - 0.7f) * 3.3f).coerceIn(0f, 0.4f)
            drawPath(vesselPath,
                color = MoAColors.alteplasePurple.copy(alpha = haloAlpha * glowPulse),
                style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  5.  ADENOSINE  — heart (A1 receptor → transient AV block → ↓HR)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AdenosineAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "adeno")

    // Brief pause in beat → restart
    val beatState by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f    at 0
                1f    at 400    // beat
                0.9f  at 600
                0.9f  at 1800   // pause (AV block)
                1f    at 2100   // beat resumes
                0.9f  at 2400
                0.9f  at 3000
            }, repeatMode = RepeatMode.Restart
        ), label = "adeno_beat"
    )

    val waveOut by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave_out"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        val beatScale = 0.90f + beatState * 0.18f

        withTransform({ scale(beatScale, beatScale, pivot = Offset(cx, cy)) }) {
            drawHeart(cx, cy, MoAColors.heartRed, MoAColors.heartRedDark,
                MoAColors.adenosineGold, glowAlpha = beatState * 0.8f)
        }

        // Radial conduction wave from AV node (centre of heart)
        repeat(3) { i ->
            val waveR = (waveOut * w * 0.4f) - i * w * 0.12f
            if (waveR > 0f) {
                drawCircle(color = MoAColors.adenosineGold.copy(
                    alpha = (1f - waveOut) * 0.5f * (1f - i * 0.3f)),
                    radius = waveR, center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  6.  ATROPINE  — heart (M2 block → ↑HR, treats bradycardia)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AtropineAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "atropine")

    // Start slow, ramp to normal
    val heartRate by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "hr_ramp"
    )

    val beatPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "beat_phase"
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_a"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        val beatScale = 1f + beatPhase * 0.15f * heartRate

        // Vagus nerve (right side, wavy line from top)
        drawVagusNerve(cx, cy, h, alpha = (1f - heartRate * 0.8f).coerceIn(0.1f, 1f))

        withTransform({ scale(beatScale, beatScale, pivot = Offset(cx, cy)) }) {
            drawHeart(cx, cy, MoAColors.heartRed, MoAColors.heartRedDark,
                MoAColors.atropineOrange, glowAlpha = glowAlpha * heartRate)
        }

        // Speed-up indicator arrows
        val arrowAlpha = heartRate * glowAlpha
        drawSpeedArrows(cx, cy, h, color = MoAColors.atropineOrange, alpha = arrowAlpha)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  7.  DOPAMINE  — heart + kidneys  (D1/β1 → ↑CO, ↑renal perfusion)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun DopamineAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "dopamine")

    val beatScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 700
                1f    at 0   using FastOutSlowInEasing
                1.14f at 200 using FastOutLinearInEasing
                1f    at 420
                1f    at 700
            }, repeatMode = RepeatMode.Restart
        ), label = "dop_beat"
    )

    val renalGlow by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "renal_glow"
    )

    val flowPos by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "flow_pos"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        // Kidneys (bottom left + right)
        drawKidney(isLeft = true, cx = cx, cy = cy * 1.55f,
            color = MoAColors.kidneyBrown, glowColor = MoAColors.dopamineBlue,
            glowAlpha = renalGlow)
        drawKidney(isLeft = false, cx = cx, cy = cy * 1.55f,
            color = MoAColors.kidneyBrown, glowColor = MoAColors.dopamineBlue,
            glowAlpha = renalGlow)

        // Aorta (centre vertical)
        drawPath(Path().apply {
            moveTo(cx, cy * 0.4f)
            lineTo(cx, cy * 1.8f)
        }, color = MoAColors.vesselRed.copy(alpha = 0.5f),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))

        // Renal arteries (branching left & right)
        drawPath(Path().apply {
            moveTo(cx, cy * 1.3f)
            lineTo(cx - w * 0.22f, cy * 1.55f)
        }, color = MoAColors.vesselRed.copy(alpha = 0.5f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(Path().apply {
            moveTo(cx, cy * 1.3f)
            lineTo(cx + w * 0.22f, cy * 1.55f)
        }, color = MoAColors.vesselRed.copy(alpha = 0.5f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))

        // Heart (top)
        withTransform({ scale(beatScale, beatScale, pivot = Offset(cx, cy * 0.5f)) }) {
            drawHeart(cx, cy * 0.5f, MoAColors.heartRed, MoAColors.heartRedDark,
                MoAColors.dopamineBlue, glowAlpha = renalGlow)
        }

        // Flow particle down aorta
        val pY = cy * 0.4f + flowPos * cy * 1.4f
        drawCircle(color = MoAColors.dopamineBlue, radius = 5.dp.toPx(),
            center = Offset(cx, pY), alpha = 0.85f)
        drawCircle(Color.White, 2.5f.dp.toPx(), Offset(cx, pY))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  8.  MAGNESIUM SULFATE  — heart (membrane stabiliser → antiarrhythmic)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun MagnesiumAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "mag")

    val beatScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1f    at 0   using FastOutSlowInEasing
                1.08f at 320
                1f    at 640
                1f    at 1100
            }, repeatMode = RepeatMode.Restart
        ), label = "mag_beat"
    )

    val ionOrbit by transition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ion_orbit"
    )

    val shieldAlpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shield"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        // Protective shield ring
        drawCircle(color = MoAColors.magBlue.copy(alpha = shieldAlpha),
            radius = w * 0.30f, center = Offset(cx, cy),
            style = Stroke(width = 8.dp.toPx()))

        withTransform({ scale(beatScale, beatScale, pivot = Offset(cx, cy)) }) {
            drawHeart(cx, cy, MoAColors.heartRed, MoAColors.heartRedDark,
                MoAColors.magBlue, glowAlpha = shieldAlpha * 2f)
        }

        // Mg²⁺ ions orbiting (4 ions at 90° intervals)
        repeat(4) { i ->
            val angle = ionOrbit + i * (PI / 2).toFloat()
            val ix = cx + cos(angle) * w * 0.27f
            val iy = cy + sin(angle) * h * 0.20f
            drawCircle(MoAColors.magBlue, 5.dp.toPx(), Offset(ix, iy), alpha = 0.9f)
            // small "2+" cross
            drawLine(Color.White, Offset(ix - 2.5f.dp.toPx(), iy),
                Offset(ix + 2.5f.dp.toPx(), iy), 1.2f.dp.toPx())
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  9.  NOREPINEPHRINE  — vessels (α1 → vasoconstriction → ↑SVR/MAP)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun NorepinephrineAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "norepi")

    val constrict by transition.animateFloat(
        initialValue = 1f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "constrict"
    )

    val pressurePulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pressure"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        // Three vessel segments (arterioles) that constrict
        val vesselBaseWidth = 20.dp.toPx()
        val vesselWidth = vesselBaseWidth * constrict

        repeat(3) { i ->
            val vx = cx + (i - 1) * w * 0.28f
            val path = Path().apply {
                moveTo(vx, cy - h * 0.35f)
                cubicTo(vx + 10.dp.toPx() * (1f - constrict),
                    cy - h * 0.10f,
                    vx - 10.dp.toPx() * (1f - constrict),
                    cy + h * 0.10f,
                    vx, cy + h * 0.35f)
            }
            // vessel wall shading
            drawPath(path, color = MoAColors.norepBlue.copy(alpha = 0.3f),
                style = Stroke(width = vesselWidth + 4.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, color = MoAColors.vesselRed.copy(alpha = 0.7f),
                style = Stroke(width = vesselWidth, cap = StrokeCap.Round))

            // Pressure wave particle
            val pY = cy - h * 0.35f + pressurePulse * h * 0.70f
            drawCircle(MoAColors.norepBlue, 4.dp.toPx(), Offset(vx, pY), alpha = 0.85f)
        }

        // MAP label (↑ arrow) indicator
        val arrowAlpha = (1f - constrict + 0.2f).coerceIn(0f, 1f)
        drawLine(MoAColors.norepBlue.copy(alpha = arrowAlpha),
            Offset(cx, cy * 1.85f), Offset(cx, cy * 1.45f),
            strokeWidth = 3.dp.toPx())
        // Arrowhead
        drawLine(MoAColors.norepBlue.copy(alpha = arrowAlpha),
            Offset(cx - 6.dp.toPx(), cy * 1.53f), Offset(cx, cy * 1.45f),
            strokeWidth = 3.dp.toPx())
        drawLine(MoAColors.norepBlue.copy(alpha = arrowAlpha),
            Offset(cx + 6.dp.toPx(), cy * 1.53f), Offset(cx, cy * 1.45f),
            strokeWidth = 3.dp.toPx())
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  10. VASOPRESSIN  — vessels + kidneys (V1/V2 → vasoconstriction + water retention)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun VasopressinAnimation(modifier: Modifier = Modifier) {

    val transition = rememberInfiniteTransition(label = "vaso")

    val constrict by transition.animateFloat(
        initialValue = 1f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "vas_constrict"
    )

    val renalPulse by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "renal_p"
    )

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f;   val cy = h / 2f

        // Main vessel constricting
        val vw = 22.dp.toPx() * constrict
        drawLine(MoAColors.vesselRed.copy(alpha = 0.7f),
            Offset(cx - w * 0.38f, cy * 0.8f),
            Offset(cx + w * 0.38f, cy * 0.8f),
            strokeWidth = vw)

        // Kidneys below
        drawKidney(isLeft = true, cx = cx, cy = cy * 1.55f,
            color = MoAColors.kidneyBrown,
            glowColor = Color(0xFF0288D1), glowAlpha = renalPulse)
        drawKidney(isLeft = false, cx = cx, cy = cy * 1.55f,
            color = MoAColors.kidneyBrown,
            glowColor = Color(0xFF0288D1), glowAlpha = renalPulse)

        // Water retention droplets on kidneys
        repeat(3) { i ->
            val dx = cx - w * 0.28f + i * 10.dp.toPx()
            val dy = cy * 1.65f
            drawCircle(Color(0xFF29B6F6).copy(alpha = renalPulse),
                3.5f.dp.toPx(), Offset(dx, dy))
            val dx2 = cx + w * 0.18f + i * 10.dp.toPx()
            drawCircle(Color(0xFF29B6F6).copy(alpha = renalPulse),
                3.5f.dp.toPx(), Offset(dx2, dy))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  SHARED DRAW PRIMITIVES
//  All functions are DrawScope extensions → zero object allocation per frame
// ═════════════════════════════════════════════════════════════════════════════

// ── Heart ───────────────────────────────────────────────────────────────────
private fun DrawScope.drawHeart(
    cx: Float, cy: Float,
    baseColor: Color, darkColor: Color,
    glowColor: Color, glowAlpha: Float
) {
    val s = minOf(size.width, size.height) * 0.22f

    // Glow halo
    drawCircle(glowColor.copy(alpha = glowAlpha * 0.35f),
        radius = s * 1.6f, center = Offset(cx, cy + s * 0.1f))

    // Heart path (two lobes + bottom point)
    val path = Path().apply {
        val top = cy - s * 0.35f
        val mid = cy - s * 0.05f
        val bot = cy + s * 0.9f
        val left = cx - s * 0.85f
        val right = cx + s * 0.85f

        moveTo(cx, bot)
        cubicTo(left - s * 0.1f, mid + s * 0.3f, left, top + s * 0.2f, left + s * 0.35f, top)
        cubicTo(cx - s * 0.05f, top - s * 0.55f, cx + s * 0.05f, top - s * 0.55f, right - s * 0.35f, top)
        cubicTo(right, top + s * 0.2f, right + s * 0.1f, mid + s * 0.3f, cx, bot)
        close()
    }

    // Shadow
    drawPath(path.apply { }, color = darkColor.copy(alpha = 0.5f),
        style = Fill)

    // Main fill with radial gradient feel (simulate via two overlapping fills)
    drawPath(path, color = baseColor, style = Fill)

    // Highlight (top-left lobe)
    val hlPath = Path().apply {
        val top = cy - s * 0.35f
        val left = cx - s * 0.85f
        addOval(Rect(left, top - s * 0.1f, cx - s * 0.1f, cy + s * 0.1f))
    }
    drawPath(hlPath, color = baseColor.copy(alpha = 0.4f),
        style = Fill)

    // Glint (specular)
    drawCircle(Color.White.copy(alpha = 0.25f),
        radius = s * 0.25f,
        center = Offset(cx - s * 0.35f, cy - s * 0.25f))
}

// ── Lung ────────────────────────────────────────────────────────────────────
private fun DrawScope.drawLung(
    isLeft: Boolean,
    cx: Float, cy: Float,
    color: Color, highlight: Color, highlightAlpha: Float
) {
    val s = minOf(size.width, size.height) * 0.18f
    val sign = if (isLeft) -1f else 1f
    val lx = cx + sign * s * 1.35f

    val path = Path().apply {
        // Lung outline — roughly oval with bronchus notch at medial side
        moveTo(lx, cy - s * 0.9f)
        cubicTo(lx + sign * s * 0.8f, cy - s * 0.9f,
            lx + sign * s * 1.0f, cy - s * 0.1f,
            lx + sign * s * 0.8f, cy + s * 0.7f)
        cubicTo(lx + sign * s * 0.3f, cy + s * 1.1f,
            lx - sign * s * 0.3f, cy + s * 1.0f,
            lx - sign * s * 0.1f, cy + s * 0.3f)
        cubicTo(lx - sign * s * 0.6f, cy + s * 0.1f,
            lx - sign * s * 0.4f, cy - s * 0.6f,
            lx, cy - s * 0.9f)
        close()
    }

    // Glow
    drawPath(path, color = highlight.copy(alpha = highlightAlpha * 0.4f), style = Fill)
    drawPath(path, color = color, style = Fill)

    // Bronchial tree (simplified 2-level)
    val branchColor = Color(0xFFC62828).copy(alpha = 0.5f)
    val bw = 2.5f.dp.toPx()
    // Main bronchus
    drawLine(branchColor, Offset(lx, cy - s * 0.1f), Offset(lx, cy + s * 0.5f), bw)
    // Secondary branches
    drawLine(branchColor, Offset(lx, cy + s * 0.1f),
        Offset(lx + sign * s * 0.4f, cy - s * 0.1f), bw)
    drawLine(branchColor, Offset(lx, cy + s * 0.3f),
        Offset(lx + sign * s * 0.45f, cy + s * 0.3f), bw)
    drawLine(branchColor, Offset(lx, cy + s * 0.5f),
        Offset(lx + sign * s * 0.35f, cy + s * 0.65f), bw)

    // Specular glint
    drawCircle(Color.White.copy(alpha = 0.20f), s * 0.2f,
        Offset(lx + sign * s * 0.2f, cy - s * 0.5f))
}

// ── Brain ───────────────────────────────────────────────────────────────────
private fun DrawScope.drawBrain(
    cx: Float, cy: Float,
    color: Color, darkColor: Color,
    glowColor: Color, glowAlpha: Float
) {
    val s = minOf(size.width, size.height) * 0.20f

    // Glow
    drawCircle(glowColor.copy(alpha = glowAlpha * 0.4f),
        s * 1.5f, Offset(cx, cy))

    // Left hemisphere
    val leftPath = Path().apply {
        moveTo(cx - s * 0.05f, cy + s * 0.7f)
        cubicTo(cx - s * 1.2f, cy + s * 0.6f,
            cx - s * 1.4f, cy - s * 0.3f,
            cx - s * 0.9f, cy - s * 0.9f)
        cubicTo(cx - s * 0.5f, cy - s * 1.3f,
            cx - s * 0.0f, cy - s * 1.2f,
            cx - s * 0.05f, cy - s * 0.0f)
        close()
    }
    drawPath(leftPath, color = color, style = Fill)

    // Right hemisphere
    val rightPath = Path().apply {
        moveTo(cx + s * 0.05f, cy + s * 0.7f)
        cubicTo(cx + s * 1.2f, cy + s * 0.6f,
            cx + s * 1.4f, cy - s * 0.3f,
            cx + s * 0.9f, cy - s * 0.9f)
        cubicTo(cx + s * 0.5f, cy - s * 1.3f,
            cx + s * 0.0f, cy - s * 1.2f,
            cx + s * 0.05f, cy - s * 0.0f)
        close()
    }
    drawPath(rightPath, color = color, style = Fill)

    // Corpus callosum divider
    drawLine(darkColor.copy(alpha = 0.6f),
        Offset(cx, cy - s * 1.15f), Offset(cx, cy + s * 0.7f),
        strokeWidth = 2.dp.toPx())

    // Gyri folds (3 curved lines per hemisphere)
    val gyrusColor = darkColor.copy(alpha = 0.35f)
    repeat(3) { i ->
        val gy = cy - s * 0.6f + i * s * 0.4f
        val gx = cx - s * 0.6f
        drawLine(gyrusColor, Offset(gx, gy), Offset(gx - s * 0.4f, gy - s * 0.15f),
            2.dp.toPx())
        drawLine(gyrusColor, Offset(cx + s * 0.6f, gy),
            Offset(cx + s * 1.0f, gy - s * 0.15f), 2.dp.toPx())
    }

    // Specular glint
    drawCircle(Color.White.copy(alpha = 0.18f), s * 0.3f,
        Offset(cx - s * 0.4f, cy - s * 0.7f))
}

// ── Kidney ──────────────────────────────────────────────────────────────────
private fun DrawScope.drawKidney(
    isLeft: Boolean,
    cx: Float, cy: Float,
    color: Color, glowColor: Color, glowAlpha: Float
) {
    val s = minOf(size.width, size.height) * 0.12f
    val sign = if (isLeft) -1f else 1f
    val kx = cx + sign * s * 1.8f

    val path = Path().apply {
        // Kidney bean shape
        moveTo(kx, cy - s)
        cubicTo(kx + sign * s * 1.1f, cy - s,
            kx + sign * s * 1.1f, cy + s,
            kx, cy + s)
        cubicTo(kx - sign * s * 0.2f, cy + s * 0.8f,
            kx - sign * s * 0.6f, cy + s * 0.3f,
            kx - sign * s * 0.4f, cy)
        cubicTo(kx - sign * s * 0.6f, cy - s * 0.3f,
            kx - sign * s * 0.2f, cy - s * 0.8f,
            kx, cy - s)
        close()
    }

    drawCircle(glowColor.copy(alpha = glowAlpha * 0.35f), s * 1.4f, Offset(kx, cy))
    drawPath(path, color = color, style = Fill)
    // Medulla highlight
    drawCircle(color.copy(alpha = 0.5f), s * 0.45f, Offset(kx + sign * s * 0.2f, cy))
    drawCircle(Color.White.copy(alpha = 0.15f), s * 0.2f,
        Offset(kx + sign * s * 0.5f, cy - s * 0.4f))
}

// ── Vessel / Artery layout ───────────────────────────────────────────────────
private fun DrawScope.drawVesselArtery(
    cx: Float, cy: Float, w: Float, alpha: Float
) {
    // Aortic arch
    val path = Path().apply {
        moveTo(cx, cy - size.height * 0.3f)
        cubicTo(cx + w * 0.15f, cy - size.height * 0.35f,
            cx + w * 0.35f, cy - size.height * 0.1f,
            cx + w * 0.3f, cy + size.height * 0.3f)
    }
    drawPath(path, color = MoAColors.vesselRed.copy(alpha = alpha * 0.4f),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
}

// ── ECG line ────────────────────────────────────────────────────────────────
private fun DrawScope.drawEcgLine(
    cx: Float, cy: Float, w: Float, h: Float,
    progress: Float, color: Color, alpha: Float
) {
    val startX = cx - w * 0.45f
    val endX = cx + w * 0.45f
    val lineY = cy + h * 0.38f
    val qrsX = startX + progress * (endX - startX)

    // Baseline
    drawLine(color.copy(alpha = alpha * 0.4f),
        Offset(startX, lineY), Offset(endX, lineY), 1.5f.dp.toPx())

    // QRS spike at progress position
    if (progress > 0.05f && progress < 0.85f) {
        val qPath = Path().apply {
            moveTo(qrsX - 20.dp.toPx(), lineY)
            lineTo(qrsX - 8.dp.toPx(), lineY + 6.dp.toPx())
            lineTo(qrsX, lineY - 30.dp.toPx())
            lineTo(qrsX + 8.dp.toPx(), lineY + 12.dp.toPx())
            lineTo(qrsX + 15.dp.toPx(), lineY)
        }
        drawPath(qPath, color = color.copy(alpha = alpha),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ── Vagus nerve ──────────────────────────────────────────────────────────────
private fun DrawScope.drawVagusNerve(
    cx: Float, cy: Float, h: Float, alpha: Float
) {
    val nx = cx + size.width * 0.38f
    val path = Path().apply {
        moveTo(nx, cy - h * 0.35f)
        cubicTo(nx + 8.dp.toPx(), cy - h * 0.1f,
            nx - 8.dp.toPx(), cy + h * 0.1f,
            nx + 4.dp.toPx(), cy + h * 0.3f)
    }
    drawPath(path, color = Color(0xFF81C784).copy(alpha = alpha),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(8f, 6f), 0f)))
}

// ── Speed arrows (for Atropine ↑HR) ─────────────────────────────────────────
private fun DrawScope.drawSpeedArrows(
    cx: Float, cy: Float, h: Float,
    color: Color, alpha: Float
) {
    val y0 = cy + h * 0.38f
    repeat(3) { i ->
        val ax = cx - 18.dp.toPx() + i * 18.dp.toPx()
        drawLine(color.copy(alpha = alpha), Offset(ax, y0 + 10.dp.toPx()),
            Offset(ax + 10.dp.toPx(), y0), 2.5f.dp.toPx())
        drawLine(color.copy(alpha = alpha), Offset(ax + 10.dp.toPx(), y0),
            Offset(ax, y0 - 10.dp.toPx()), 2.5f.dp.toPx())
    }
}