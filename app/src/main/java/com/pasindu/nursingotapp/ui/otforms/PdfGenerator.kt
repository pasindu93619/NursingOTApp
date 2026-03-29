// com/pasindu/nursingotapp/ui/otforms/PdfGenerator.kt
package com.pasindu.nursingotapp.ui.otforms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.pasindu.nursingotapp.R
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.Period
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.data.model.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class PdfGenerator(private val context: Context) {

    private val a4Width = 595
    private val a4Height = 842

    private val sinhalaTypeface: Typeface? = ResourcesCompat.getFont(context, R.font.notosassinhala)

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val centerBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val leftBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    private val verticalBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 15f
        typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val bottomEquationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    fun generateAndReturnFile(
        profile: UserProfile,
        logs: List<DailyLog>,
        period: Period,
        summary: PeriodSummary
    ): File? {
        val document = PdfDocument()

        val rawFront = BitmapFactory.decodeResource(context.resources, R.drawable.form_front_bg)
        val rawBack = BitmapFactory.decodeResource(context.resources, R.drawable.form_back_bg)

        try {
            drawForm1(document, profile, period, summary, logs, rawFront)

            val fullWeekLogs = filterFullWeekLogs(logs, period)
            val weekGroups = fullWeekLogs.groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }
                .toSortedMap()

            drawForm2(document, weekGroups, profile, rawBack)

            val outputDir = File(context.cacheDir, "pdf").apply { mkdirs() }
            val outputFile = File(outputDir, "nursing_ot_claim_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outputFile).use { stream ->
                document.writeTo(stream)
            }
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
            rawFront?.recycle()
            rawBack?.recycle()
        }
    }

    private fun drawForm1(
        document: PdfDocument,
        profile: UserProfile,
        period: Period,
        summary: PeriodSummary,
        logs: List<DailyLog>,
        background: Bitmap
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val destRect = android.graphics.Rect(0, 0, a4Width, a4Height)
        canvas.drawBitmap(background, null, destRect, null)

        val originalImgW = 2475f
        val originalImgH = 3500f

        fun sX(x: Float): Float = x * (a4Width / originalImgW)
        fun sY(y: Float): Float = y * (a4Height / originalImgH)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        canvas.drawText(profile.serviceNo, sX(2113f), sY(80f), bodyPaint)
        canvas.drawText(profile.unit, sX(2050f), sY(156f), bodyPaint)
        canvas.drawText(profile.paySheetNo, sX(2103f), sY(346f), bodyPaint)
        canvas.drawText(profile.name, sX(1046f), sY(403f), bodyPaint)
        canvas.drawText(profile.grade, sX(506f), sY(466f), bodyPaint)
        canvas.drawText(profile.unit, sX(590f), sY(536f), bodyPaint)
        canvas.drawText(period.claimStart.format(dateFormatter), sX(470f), sY(600f), bodyPaint)
        canvas.drawText(period.claimEnd.format(dateFormatter), sX(1046f), sY(603f), bodyPaint)
        canvas.drawText(formatDouble(profile.basicSalary), sX(673f), sY(670f), bodyPaint)
        canvas.drawText(formatDouble(profile.otRate), sX(1473f), sY(673f), bodyPaint)
        canvas.drawText(formatFloat(summary.totalOTHours), sX(520f), sY(730f), bodyPaint)

        val rowGap = 60f
        val tCol1X = 375f
        val tCol2X = 950f
        val tCol3X = 1510f
        val tCol4X = 2100f

        var phY = 983f
        logs.filter { it.isPH }.take(4).forEach { log ->
            val inTime = log.normalTimeInStr.ifBlank { log.otTimeInStr }
            val outTime = log.normalTimeOutStr.ifBlank { log.otTimeOutStr }
            canvas.drawText(log.date.format(dateFormatter), sX(tCol1X), sY(phY), centerBodyPaint)
            canvas.drawText(inTime, sX(tCol2X), sY(phY), centerBodyPaint)
            canvas.drawText(outTime, sX(tCol3X), sY(phY), centerBodyPaint)
            canvas.drawText(formatHrs(log.computedNormalHours + log.computedOtHours), sX(tCol4X), sY(phY), centerBodyPaint)
            phY += rowGap
        }

        var doY = 1410f
        logs.filter { it.isDO }.take(4).forEach { log ->
            val inTime = log.normalTimeInStr.ifBlank { log.otTimeInStr }
            val outTime = log.normalTimeOutStr.ifBlank { log.otTimeOutStr }
            canvas.drawText(log.date.format(dateFormatter), sX(tCol1X), sY(doY), centerBodyPaint)
            canvas.drawText(inTime, sX(tCol2X), sY(doY), centerBodyPaint)
            canvas.drawText(outTime, sX(tCol3X), sY(doY), centerBodyPaint)
            canvas.drawText(formatHrs(log.computedNormalHours + log.computedOtHours), sX(tCol4X), sY(doY), centerBodyPaint)
            doY += rowGap
        }

        var leaveY = 1836f
        logs.filter { it.isLeave }.take(4).forEach { log ->
            canvas.drawText(log.date.format(dateFormatter), sX(tCol1X), sY(leaveY), centerBodyPaint)
            canvas.drawText(log.date.format(dateFormatter), sX(tCol2X), sY(leaveY), centerBodyPaint)
            val leaveText = log.leaveType ?: "Leave"
            canvas.drawText(leaveText, sX(tCol3X), sY(leaveY), centerBodyPaint)
            canvas.drawText("1", sX(tCol4X), sY(leaveY), centerBodyPaint)
            leaveY += rowGap
        }

        canvas.drawText(formatFloat(summary.totalOTHours), sX(990f), sY(2076f), centerBodyPaint)
        canvas.drawText(formatDouble(summary.otAmountRs), sX(2123f), sY(2076f), centerBodyPaint)
        canvas.drawText(summary.totalPHDays.toString(), sX(990f), sY(2150f), centerBodyPaint)
        canvas.drawText(formatDouble(summary.phAmountRs), sX(2123f), sY(2150f), centerBodyPaint)
        canvas.drawText(summary.totalDODays.toString(), sX(990f), sY(2226f), centerBodyPaint)
        canvas.drawText(formatDouble(summary.doAmountRs), sX(2123f), sY(2226f), centerBodyPaint)

        val todayStr = LocalDate.now().format(dateFormatter)
        canvas.drawText(todayStr, sX(540f), sY(2530f), bodyPaint)
        canvas.drawText(todayStr, sX(460f), sY(2606f), bodyPaint)
        canvas.drawText(period.claimStart.format(dateFormatter), sX(1003f), sY(2606f), bodyPaint)

        document.finishPage(page)
    }

    private fun drawForm2(
        document: PdfDocument,
        weekGroups: Map<LocalDate, List<DailyLog>>,
        profile: UserProfile,
        background: Bitmap
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val destRect = android.graphics.Rect(0, 0, a4Width, a4Height)
        canvas.drawBitmap(background, null, destRect, null)

        val originalImgW = 2475f
        val originalImgH = 3500f

        fun sX(x: Float): Float = x * (a4Width / originalImgW)
        fun sY(y: Float): Float = y * (a4Height / originalImgH)

        val yearY = sY(630f)
        val yearX = sX(953f)
        val monthX = sX(1900f)

        val colDateX = 593f
        val colLeaveTextX = 893f
        val colNormInX = 1010f
        val colNormOutX = 1151f
        val colNormHrsX = 1302f
        val colOtInX = 1442f
        val colOtOutX = 1585f
        val colOtHrsX = 1722f

        val weekYOffset = 492.5f
        val rowHeight = 71.25f

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val payableLabels = listOf("CL", "VL", "DL", "PH", "sL", "Special Leave", "Annual", "Sick", "CL/2", "Half Casual Leave")

        val firstWeekStart = weekGroups.keys.firstOrNull()
        if (firstWeekStart != null) {
            canvas.drawText(firstWeekStart.year.toString(), yearX, yearY, bodyPaint)
            val monthName = firstWeekStart.month.name
            canvas.drawText(monthName, monthX, yearY, bodyPaint)
        }

        val weeklyOtTotalsList = mutableListOf<Float>()

        var weekIndex = 0
        for ((weekStart, weekLogs) in weekGroups) {
            val daysOfWeek = (0..6).map { weekStart.plusDays(it.toLong()) }

            var totalNormalHours = 0f
            var totalOtHours = 0f
            val weekBaseY = weekIndex * weekYOffset

            for ((dayIndex, day) in daysOfWeek.withIndex()) {
                val log = weekLogs.find { it.date == day }
                val currentYOffset = weekBaseY + (dayIndex * rowHeight)

                canvas.drawText(day.format(dateFormatter), sX(colDateX), sY(961f + currentYOffset), centerBodyPaint)

                if (log != null) {
                    val rawLeave = log.leaveType ?: ""
                    val isFullLeave = log.isLeave || ((log.isDO || log.isPH) && log.computedNormalHours == 0f && log.computedOtHours == 0f)
                    val isNight = log.normalTimeInStr.startsWith("19") || log.normalTimeInStr.startsWith("20") || log.otTimeInStr.startsWith("19") || log.otTimeInStr.startsWith("20")

                    var insideText = ""
                    var outsideText = ""

                    if (isFullLeave) {
                        if (log.isDO) insideText = "DO"
                        else if (log.isPH) insideText = "PH"
                        else {
                            insideText = when (rawLeave) {
                                "Special", "Special Leave" -> "sL"
                                "Absent", "AB" -> "AB"
                                "CL", "VL", "DL" -> rawLeave
                                else -> rawLeave.take(4)
                            }
                        }
                    } else {
                        val baseOutside = when {
                            log.isDO -> "DO"
                            log.isPH -> "PH"
                            rawLeave == "Short Leave" -> "SL"
                            rawLeave == "Half Casual Leave" -> "CL/2"
                            else -> ""
                        }

                        outsideText = if (baseOutside.isNotEmpty()) {
                            if (isNight) "$baseOutside/N" else baseOutside
                        } else {
                            if (isNight) "N" else ""
                        }
                    }

                    if (outsideText.isNotEmpty()) {
                        canvas.drawText(outsideText, sX(colLeaveTextX), sY(964f + currentYOffset), centerBodyPaint)
                    }

                    if (isFullLeave) {
                        canvas.drawText(insideText, sX(colNormInX), sY(964f + currentYOffset), centerBodyPaint)
                        canvas.drawText("-", sX(colNormOutX), sY(964f + currentYOffset), centerBodyPaint)
                    } else {
                        if (log.computedNormalHours > 0f) {
                            canvas.drawText(log.normalTimeInStr, sX(colNormInX), sY(964f + currentYOffset), centerBodyPaint)
                            canvas.drawText(log.normalTimeOutStr, sX(colNormOutX), sY(964f + currentYOffset), centerBodyPaint)
                        }
                    }

                    var dayNormalHoursToPrint = log.computedNormalHours

                    val checkLabel = if (isFullLeave) insideText else outsideText
                    if (dayNormalHoursToPrint == 0f && (checkLabel in payableLabels || insideText in payableLabels)) {
                        val isWknd = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
                        dayNormalHoursToPrint = if (profile.unit.contains("Clinic", true) || profile.unit.contains("Unit", true) || profile.unit.contains("OPD", true)) {
                            if (isWknd) 6f else 8f
                        } else {
                            6f
                        }
                    }

                    val dayNormalHoursToAdd = dayNormalHoursToPrint

                    if (dayNormalHoursToPrint > 0f) {
                        if (dayIndex < 6) {
                            canvas.drawText(formatHrs(dayNormalHoursToPrint), sX(colNormHrsX), sY(964f + currentYOffset), centerBodyPaint)
                        } else {
                            canvas.drawText(formatHrs(dayNormalHoursToPrint), sX(1282f), sY(1363f + weekBaseY), centerBodyPaint)
                        }
                    }
                    totalNormalHours += dayNormalHoursToAdd

                    if (log.computedOtHours > 0f) {
                        canvas.drawText(log.otTimeInStr, sX(colOtInX), sY(964f + currentYOffset), centerBodyPaint)
                        canvas.drawText(log.otTimeOutStr, sX(colOtOutX), sY(964f + currentYOffset), centerBodyPaint)

                        if (dayIndex < 6) {
                            canvas.drawText(formatHrs(log.computedOtHours), sX(colOtHrsX), sY(964f + currentYOffset), centerBodyPaint)
                        } else {
                            canvas.drawText(formatHrs(log.computedOtHours), sX(1735f), sY(1366f + weekBaseY), centerBodyPaint)
                        }
                        totalOtHours += log.computedOtHours
                    }

                    val customReason = when {
                        !log.reason.isNullOrBlank() && log.reason != "Need for service" -> log.reason
                        else -> null
                    }

                    if (customReason != null) {
                        canvas.drawText(customReason, sX(1870f), sY(964f + currentYOffset), leftBodyPaint)
                    }
                }
            }

            canvas.drawText(formatHrs(totalNormalHours), sX(1349f), sY(1390f + weekBaseY), centerBodyPaint)
            canvas.drawText(formatHrs(totalOtHours), sX(1789f), sY(1386f + weekBaseY), centerBodyPaint)

            var weeklyOtSum = 0f

            val finalTotalX = 2133f
            val finalTotalY = 1370f + weekBaseY

            val finalTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            if (totalNormalHours > 36f) {
                val extraFromNorm = totalNormalHours - 36f
                val grandTotal = extraFromNorm + totalOtHours
                weeklyOtSum = grandTotal

                val mathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 10f
                    typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.RIGHT
                }

                val numX = 2150f
                val opX = 2175f
                var currentY = 1010f + weekBaseY
                val stepY = 48f

                canvas.drawText(formatFloat(totalNormalHours), sX(numX), sY(currentY), mathPaint)
                canvas.drawText("-", sX(opX), sY(currentY), mathPaint)
                currentY += stepY; canvas.drawText("36", sX(numX), sY(currentY), mathPaint)
                currentY += 15f; canvas.drawText("-------", sX(opX), sY(currentY), mathPaint)
                currentY += 35f; canvas.drawText(formatHrs(extraFromNorm), sX(numX), sY(currentY), mathPaint)
                currentY += stepY + 5f; canvas.drawText(formatFloat(extraFromNorm), sX(numX), sY(currentY), mathPaint)
                canvas.drawText("+", sX(opX), sY(currentY), mathPaint)
                currentY += stepY; canvas.drawText(formatFloat(totalOtHours), sX(numX), sY(currentY), mathPaint)
                currentY += 15f; canvas.drawText("-------", sX(opX), sY(currentY), mathPaint)

                canvas.drawText(formatHrs(grandTotal), sX(finalTotalX), sY(finalTotalY), finalTotalPaint)

            } else {
                if (totalOtHours > 0f) {
                    weeklyOtSum = totalOtHours
                    canvas.drawText(formatHrs(totalOtHours), sX(finalTotalX), sY(finalTotalY), finalTotalPaint)
                }
            }

            weeklyOtTotalsList.add(weeklyOtSum)
            weekIndex++
            if (weekIndex >= 5) break
        }

        canvas.save()
        canvas.translate(sX(880f), sY(2156f))
        canvas.rotate(-90f)
        canvas.drawText(profile.unit, 0f, 0f, verticalBoldPaint)
        canvas.restore()

        canvas.save()
        canvas.translate(sX(1986f), sY(2153f))
        canvas.rotate(-90f)
        canvas.drawText("Need for service", 0f, 0f, verticalBoldPaint)
        canvas.restore()

        val nonZeroWeeks = weeklyOtTotalsList.filter { it > 0f }
        if (nonZeroWeeks.isNotEmpty()) {
            val equationString = nonZeroWeeks.joinToString(" + ") { formatHrs(it) } + " = " + formatHrs(nonZeroWeeks.sum())
            // ADJUSTMENT: Moved Y slightly up from 3450f to 3425f to perfectly center it in the bottom box.
            canvas.drawText(equationString, sX(1000f), sY(3425f), bottomEquationPaint)
        }

        document.finishPage(page)
    }

    private fun filterFullWeekLogs(logs: List<DailyLog>, period: Period): List<DailyLog> {
        val firstSunday = period.claimStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastSaturday = period.claimEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        if (firstSunday.isAfter(lastSaturday)) return emptyList()
        return logs.filter { !it.date.isBefore(firstSunday) && !it.date.isAfter(lastSaturday) }
    }

    private fun formatDouble(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun formatFloat(value: Float): String {
        return if (value % 1 == 0f) {
            String.format(Locale.US, "%02d", value.toInt())
        } else {
            String.format(Locale.US, "%04.1f", value)
        }
    }

    private fun formatHrs(value: Float): String {
        if (value <= 0f) return ""
        return "${formatFloat(value)}h"
    }
}