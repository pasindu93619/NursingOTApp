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
                val cycleW = 120f
                while (x < width) {
                    path.lineTo(x + 10f, midY) // Isoelectric
                    path.quadraticBezierTo(x + 18f, midY - height * 0.15f, x + 25f, midY) // P wave
                    path.lineTo(x + 35f, midY) // PR segment
                    path.lineTo(x + 40f, midY + height * 0.1f) // Q wave
                    path.lineTo(x + 45f, midY - height * 0.5f) // R wave
                    path.lineTo(x + 50f, midY + height * 0.15f) // S wave
                    path.lineTo(x + 55f, midY) // J point
                    path.lineTo(x + 75f, midY) // ST segment
                    path.quadraticBezierTo(x + 85f, midY - height * 0.2f, x + 100f, midY) // T wave
                    x += cycleW
                }
            }

            EcgRhythm.SVT -> {
                val cycleW = 50f
                while (x < width) {
                    path.lineTo(x + 10f, midY)
                    path.lineTo(x + 15f, midY - height * 0.45f) // Narrow R
                    path.lineTo(x + 20f, midY + height * 0.15f) // Narrow S
                    path.lineTo(x + 25f, midY)
                    path.quadraticBezierTo(x + 35f, midY - height * 0.15f, x + 45f, midY) // T wave
                    x += cycleW
                }
            }

            EcgRhythm.VTACH -> {
                val cycleW = 80f
                while (x < width) {
                    path.quadraticBezierTo(x + cycleW * 0.25f, midY - height * 0.45f, x + cycleW * 0.5f, midY)
                    path.quadraticBezierTo(x + cycleW * 0.75f, midY + height * 0.45f, x + cycleW, midY)
                    x += cycleW
                }
            }

            EcgRhythm.VFIB -> {
                while (x < width) {
                    val step = Random.nextInt(15, 35).toFloat()
                    val nextX = x + step
                    val amp = Random.nextFloat() * (height * 0.4f) + (height * 0.1f)
                    val sign = if (Random.nextBoolean()) 1 else -1
                    path.quadraticBezierTo(x + step / 2f, midY + (amp * sign), nextX, midY + (amp * -sign * 0.5f))
                    x = nextX
                }
            }

            EcgRhythm.AFIB -> {
                while (x < width) {
                    val cycleW = Random.nextInt(70, 150).toFloat()
                    val qrsStart = x + (cycleW * 0.6f)

                    var fibX = x
                    while (fibX < qrsStart) {
                        val fStep = 10f
                        val fAmp = Random.nextFloat() * (height * 0.08f)
                        val fSign = if (Random.nextBoolean()) 1 else -1
                        path.quadraticBezierTo(fibX + fStep/2, midY + (fAmp * fSign), fibX + fStep, midY)
                        fibX += fStep
                    }

                    path.lineTo(qrsStart + 5f, midY - height * 0.45f)
                    path.lineTo(qrsStart + 10f, midY + height * 0.15f)
                    path.lineTo(qrsStart + 15f, midY)
                    path.quadraticBezierTo(qrsStart + 25f, midY - height * 0.1f, qrsStart + 35f, midY)
                    x += cycleW
                }
            }

            EcgRhythm.TORSADES -> {
                val fastFreq = 0.1f
                val slowFreq = 0.015f
                val step = 5f
                while (x < width) {
                    val envelope = sin(x * slowFreq)
                    val wave = sin(x * fastFreq) * envelope
                    val y = midY - (wave * height * 0.45f)
                    path.lineTo(x, y)
                    x += step
                }
            }

            EcgRhythm.HEART_BLOCK -> {
                val pInterval = 70f
                val qrsInterval = 220f
                var pX = 10f
                var qrsX = 50f
                val step = 2f
                while (x < width) {
                    var yOffset = 0f
                    if (x > pX && x < pX + 20f) {
                        val pProgress = (x - pX) / 20f
                        yOffset -= sin(pProgress * PI.toFloat()) * (height * 0.15f)
                    } else if (x >= pX + 20f) {
                        pX += pInterval
                    }

                    if (x > qrsX && x < qrsX + 40f) {
                        val qrsProgress = (x - qrsX) / 40f
                        yOffset -= sin(qrsProgress * PI.toFloat() * 2f) * (height * 0.4f)
                    } else if (x >= qrsX + 40f) {
                        if (x > qrsX + 50f && x < qrsX + 90f) {
                            val tProgress = (x - (qrsX + 50f)) / 40f
                            yOffset += sin(tProgress * PI.toFloat()) * (height * 0.2f)
                        } else if (x >= qrsX + 90f) {
                            qrsX += qrsInterval
                        }
                    }
                    path.lineTo(x, midY + yOffset)
                    x += step
                }
            }

            EcgRhythm.ASYSTOLE -> {
                while (x < width) {
                    val step = 20f
                    val drift = (Random.nextFloat() - 0.5f) * (height * 0.03f)
                    path.lineTo(x + step, midY + drift)
                    x += step
                }
            }
        }
        return path
    }
}