package com.pasindu.nursingotapp.ui.components

import androidx.compose.ui.graphics.Path
import com.pasindu.nursingotapp.ui.screens.EcgRhythm
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object EcgWaveformGenerator {

    /**
     * Generates a medically accurate ECG Path for Jetpack Compose Canvas.
     * @param rhythm The specific cardiac rhythm to draw.
     * @param width The total width to generate.
     * @param height The height of the canvas.
     */
    fun generatePath(rhythm: EcgRhythm, width: Float, height: Float): Path {
        val path = Path()
        if (width <= 0f || height <= 0f) return path

        val midY = height / 2f
        path.moveTo(0f, midY)

        var x = 0f

        when (rhythm) {
            EcgRhythm.NSR -> {
                val cycleW = 200f // Widened significantly for slower reading
                while (x < width) {
                    path.lineTo(x + 20f, midY) // Isoelectric
                    path.quadraticBezierTo(x + 30f, midY - height * 0.15f, x + 40f, midY) // P wave
                    path.lineTo(x + 55f, midY) // PR segment
                    path.lineTo(x + 62f, midY + height * 0.1f) // Q wave
                    path.lineTo(x + 72f, midY - height * 0.5f) // R wave
                    path.lineTo(x + 82f, midY + height * 0.15f) // S wave
                    path.lineTo(x + 90f, midY) // J point
                    path.lineTo(x + 115f, midY) // ST segment
                    path.quadraticBezierTo(x + 135f, midY - height * 0.2f, x + 160f, midY) // T wave
                    x += cycleW
                }
            }

            EcgRhythm.SVT -> {
                val cycleW = 110f // Widened for clarity so it doesn't look like a solid block
                while (x < width) {
                    path.lineTo(x + 20f, midY)
                    path.lineTo(x + 30f, midY - height * 0.45f) // Narrow R
                    path.lineTo(x + 40f, midY + height * 0.15f) // Narrow S
                    path.lineTo(x + 50f, midY)
                    path.quadraticBezierTo(x + 70f, midY - height * 0.15f, x + 90f, midY) // T wave
                    x += cycleW
                }
            }

            EcgRhythm.VTACH -> {
                val cycleW = 160f // Widened to show distinct, wide complexes
                while (x < width) {
                    path.quadraticBezierTo(x + cycleW * 0.25f, midY - height * 0.45f, x + cycleW * 0.5f, midY)
                    path.quadraticBezierTo(x + cycleW * 0.75f, midY + height * 0.45f, x + cycleW, midY)
                    x += cycleW
                }
            }

            EcgRhythm.VFIB -> {
                // FIXED: Wider steps and cubic bezier curves for a slower, rolling chaotic baseline
                while (x < width) {
                    val step = Random.nextInt(50, 100).toFloat() // Much wider steps
                    val nextX = x + step
                    val amp = Random.nextFloat() * (height * 0.35f) + (height * 0.1f)
                    val sign = if (Random.nextBoolean()) 1 else -1

                    path.cubicTo(
                        x + step * 0.3f, midY + (amp * sign * 1.5f),
                        x + step * 0.7f, midY + (amp * -sign * 0.5f),
                        nextX, midY + (amp * -sign)
                    )
                    x = nextX
                }
            }

            EcgRhythm.AFIB -> {
                // FIXED: Slower fibrillatory waves and wider R-R intervals
                while (x < width) {
                    val cycleW = Random.nextInt(150, 300).toFloat() // Irregular but slower R-R
                    val qrsStart = x + (cycleW * 0.7f)

                    var fibX = x
                    while (fibX < qrsStart) {
                        val fStep = Random.nextInt(25, 45).toFloat() // Wider/slower fibrillatory waves
                        val fAmp = Random.nextFloat() * (height * 0.08f)
                        val fSign = if (Random.nextBoolean()) 1 else -1
                        path.quadraticBezierTo(fibX + fStep/2, midY + (fAmp * fSign), fibX + fStep, midY)
                        fibX += fStep
                    }

                    path.lineTo(qrsStart + 10f, midY - height * 0.45f)
                    path.lineTo(qrsStart + 20f, midY + height * 0.15f)
                    path.lineTo(qrsStart + 28f, midY)
                    path.quadraticBezierTo(qrsStart + 50f, midY - height * 0.1f, qrsStart + 70f, midY)
                    x += cycleW
                }
            }

            EcgRhythm.TORSADES -> {
                val fastFreq = 0.06f // Slower spindle oscillation
                val slowFreq = 0.008f // Slower envelope
                val step = 6f
                while (x < width) {
                    val envelope = sin(x * slowFreq)
                    val wave = sin(x * fastFreq) * envelope
                    val y = midY - (wave * height * 0.45f)
                    path.lineTo(x, y)
                    x += step
                }
            }

            EcgRhythm.HEART_BLOCK -> {
                val pInterval = 120f // Widened
                val qrsInterval = 350f // Widened
                var pX = 20f
                var qrsX = 80f
                val step = 3f
                while (x < width) {
                    var yOffset = 0f
                    if (x > pX && x < pX + 30f) {
                        val pProgress = (x - pX) / 30f
                        yOffset -= sin(pProgress * PI.toFloat()) * (height * 0.15f)
                    } else if (x >= pX + 30f) {
                        pX += pInterval
                    }

                    if (x > qrsX && x < qrsX + 60f) {
                        val qrsProgress = (x - qrsX) / 60f
                        yOffset -= sin(qrsProgress * PI.toFloat() * 2f) * (height * 0.4f)
                    } else if (x >= qrsX + 60f) {
                        if (x > qrsX + 70f && x < qrsX + 130f) {
                            val tProgress = (x - (qrsX + 70f)) / 60f
                            yOffset += sin(tProgress * PI.toFloat()) * (height * 0.2f)
                        } else if (x >= qrsX + 130f) {
                            qrsX += qrsInterval
                        }
                    }
                    path.lineTo(x, midY + yOffset)
                    x += step
                }
            }

            EcgRhythm.ASYSTOLE -> {
                while (x < width) {
                    val step = 35f
                    val drift = (Random.nextFloat() - 0.5f) * (height * 0.03f)
                    path.lineTo(x + step, midY + drift)
                    x += step
                }
            }
        }
        return path
    }
}