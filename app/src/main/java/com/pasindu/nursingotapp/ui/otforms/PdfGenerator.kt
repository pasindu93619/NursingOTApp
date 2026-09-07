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
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class LeaveBlock(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: String,
    val totalDays: Int
)

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
            val weekGroups = fullWeekLogs
                .groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }
                .toSortedMap()

            drawForm2(document, weekGroups, profile, rawBack)

            val majorityMonth = logs
                .groupBy { it.date.month }
                .maxByOrNull { it.value.size }
                ?.key
                ?.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                ?: "Unknown"

            val outputDir = File(context.cacheDir, "pdf").apply { mkdirs() }
            val outputFile = File(outputDir, "OT_Form_$majorityMonth.pdf")

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
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
        )
        val canvas = page.canvas

        canvas.drawBitmap(
            background,
            null,
            android.graphics.Rect(0, 0, a4Width, a4Height),
            null
        )

        val originalImgW = 2475f
        val originalImgH = 3500f

        fun sX(x: Float): Float = x * (a4Width / originalImgW)
        fun sY(y: Float): Float = y * (a4Height / originalImgH)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // These baselines are aligned to the visible row centers of the supplied
        // one-page form rather than using the previous coarse spacing.
        val phRowHeight = 68f
        val doRowHeight = 68f
        val leaveRowHeight = 68f

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

        val workingPHs = logs.filter { it.isPH && it.computedNormalHours > 0f }
        var phY = 914f
        workingPHs.take(4).forEach { log ->
            canvas.drawText(log.date.format(dateFormatter), sX(480f), sY(phY), centerBodyPaint)
            canvas.drawText("${log.normalTimeInStr}H", sX(1120f), sY(phY), centerBodyPaint)
            canvas.drawText("${log.normalTimeOutStr}H", sX(1740f), sY(phY), centerBodyPaint)
            canvas.drawText(formatHrs(log.computedNormalHours), sX(2250f), sY(phY), centerBodyPaint)
            phY += phRowHeight
        }

        val workingDOs = logs.filter { it.isDO && it.computedNormalHours > 0f }
        var doY = 1314f
        workingDOs.take(4).forEach { log ->
            canvas.drawText(log.date.format(dateFormatter), sX(480f), sY(doY), centerBodyPaint)
            canvas.drawText("${log.normalTimeInStr}H", sX(1120f), sY(doY), centerBodyPaint)
            canvas.drawText("${log.normalTimeOutStr}H", sX(1740f), sY(doY), centerBodyPaint)
            canvas.drawText(formatHrs(log.computedNormalHours), sX(2250f), sY(doY), centerBodyPaint)
            doY += doRowHeight
        }

        val leaveLogs = logs.filter {
            (it.isLeave && it.leaveType == "CL") ||
                (it.isDO && it.computedNormalHours == 0f)
        }

        val groupedLeaves = groupConsecutiveLeaves(leaveLogs)
        var leaveY = 1714f
        groupedLeaves.take(4).forEach { leave ->
            canvas.drawText(leave.startDate.format(dateFormatter), sX(460f), sY(leaveY), centerBodyPaint)
            canvas.drawText(leave.endDate.format(dateFormatter), sX(980f), sY(leaveY), centerBodyPaint)
            canvas.drawText(leave.type, sX(1600f), sY(leaveY), centerBodyPaint)
            canvas.drawText(leave.totalDays.toString(), sX(2200f), sY(leaveY), centerBodyPaint)
            leaveY += leaveRowHeight
        }

        canvas.drawText(
            formatFloat(summary.totalOTHours),
            sX(990f),
            sY(2076f),
            centerBodyPaint
        )
        canvas.drawText(
            formatDouble(summary.otAmountRs),
            sX(2123f),
            sY(2076f),
            centerBodyPaint
        )
        canvas.drawText(
            summary.totalPHDays.toString(),
            sX(990f),
            sY(2150f),
            centerBodyPaint
        )
        canvas.drawText(
            formatDouble(summary.phAmountRs),
            sX(2123f),
            sY(2150f),
            centerBodyPaint
        )
        canvas.drawText(
            summary.totalDODays.toString(),
            sX(990f),
            sY(2226f),
            centerBodyPaint
        )
        canvas.drawText(
            formatDouble(summary.doAmountRs),
            sX(2123f),
            sY(2226f),
            centerBodyPaint
        )

        document.finishPage(page)
    }

    private fun drawForm2(
        document: PdfDocument,
        weekGroups: Map<LocalDate, List<DailyLog>>,
        profile: UserProfile,
        background: Bitmap
    ) {
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()
        )
        val canvas = page.canvas

        canvas.drawBitmap(
            background,
            null,
            android.graphics.Rect(0, 0, a4Width, a4Height),
            null
        )

        val originalImgW = 2475f
        val originalImgH = 3500f

        fun sX(x: Float): Float = x * (a4Width / originalImgW)
        fun sY(y: Float): Float = y * (a4Height / originalImgH)

        val yearY = sY(630f)
        val yearX = sX(953f)
        val monthX = sX(1900f)

        // Table columns taken from the supplied blank/reference form.
        val colDateX = 593f
        val colLeaveTextX = 893f
        val colNormInX = 1010f
        val colNormOutX = 1151f
        val colNormHrsX = 1302f
        val colOtInX = 1442f
        val colOtOutX = 1585f
        val colOtHrsX = 1722f

        // Exact rightmost result-column anchors from the user's annotated reference:
        // keep the result column horizontally fixed and derive each weekly anchor
        // from the visible five-week blocks.
        val resultX = 2133f
        val resultYAnchors = floatArrayOf(
            1288f, // week 1 anchor
            1756f, // week 2 anchor
            2224f, // week 3 anchor
            2692f, // week 4 anchor
            3160f  // week 5 anchor area
        )
        val equationOffsetY = 45f

        val rowHeight = 61.5f
        val weekBlockHeight = 430.5f
        val firstRowY = 955f

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val payableLabels = listOf(
            "CL", "VL", "DL", "PH", "sL",
            "Special Leave", "Annual", "Sick",
            "CL/2", "Half Casual Leave"
        )

        val firstWeekStart = weekGroups.keys.firstOrNull()
        if (firstWeekStart != null) {
            canvas.drawText(firstWeekStart.year.toString(), yearX, yearY, bodyPaint)
            canvas.drawText(firstWeekStart.month.name, monthX, yearY, bodyPaint)
        }

        val weeklyTrueOtTotals = mutableListOf<Float>()

        var weekIndex = 0
        for ((weekStart, weekLogsRaw) in weekGroups) {
            val weekLogs = weekLogsRaw.sortedBy { it.date }
            val daysOfWeek = (0..6).map { weekStart.plusDays(it.toLong()) }

            val authoritativeWeek = WeeklyOtCalculator.calculate(
                logs = weekLogs,
                claimStart = weekStart,
                claimEnd = weekStart.plusDays(6),
                otRate = 0.0,
                dayRate = 0.0,
                doRate = 0.0
            )

            val weekRecordedDutyHours = weekLogs
                .sumOf { it.computedNormalHours.toDouble().coerceAtLeast(0.0) }
                .toFloat()

            val weekRecordedOtHours = weekLogs
                .sumOf { it.computedOtHours.toDouble().coerceAtLeast(0.0) }
                .toFloat()

            val trueOtHours = (
                weekRecordedDutyHours -
                    WeeklyOtCalculator.WEEKLY_NORMAL_LIMIT_HOURS.toFloat() +
                    weekRecordedOtHours
                ).coerceAtLeast(0f)

            val authoritativeAllocations = authoritativeWeek.allocations.associateBy { it.date }

            val weekBaseY = weekIndex * weekBlockHeight

            for ((dayIndex, day) in daysOfWeek.withIndex()) {
                val log = weekLogs.find { it.date == day }
                val currentYOffset = dayIndex * rowHeight

                canvas.drawText(
                    day.format(dateFormatter),
                    sX(colDateX),
                    sY(firstRowY + weekBaseY + currentYOffset),
                    centerBodyPaint
                )

                if (log != null) {
                    val rawLeave = log.leaveType ?: ""
                    val isFullLeave =
                        (log.isLeave && rawLeave != "SD") ||
                            ((log.isDO || log.isPH) &&
                                log.computedNormalHours == 0f &&
                                log.computedOtHours == 0f) ||
                            (rawLeave == "SD" &&
                                log.computedNormalHours == 0f &&
                                log.computedOtHours == 0f)

                    val isNight =
                        log.normalTimeInStr.startsWith("19") ||
                            log.normalTimeInStr.startsWith("20") ||
                            log.otTimeInStr.startsWith("19") ||
                            log.otTimeInStr.startsWith("20")

                    var insideText = ""
                    var outsideText = ""

                    if (isFullLeave) {
                        insideText = when {
                            log.isDO -> "DO"
                            log.isPH -> "PH"
                            rawLeave == "Special" || rawLeave == "Special Leave" -> "sL"
                            rawLeave == "Absent" || rawLeave == "AB" -> "AB"
                            rawLeave in listOf("CL", "VL", "DL", "SD") -> rawLeave
                            else -> rawLeave.take(4)
                        }
                    } else {
                        val baseOutside = when {
                            log.isDO -> "DO"
                            log.isPH -> "PH"
                            rawLeave == "SD" -> "SD"
                            rawLeave == "Short Leave" -> "SL"
                            rawLeave == "Half Casual Leave" -> "CL/2"
                            else -> ""
                        }

                        outsideText = when {
                            baseOutside.isNotEmpty() && isNight -> "$baseOutside/N"
                            baseOutside.isNotEmpty() -> baseOutside
                            isNight -> "N"
                            else -> ""
                        }
                    }

                    val rowY = sY(firstRowY + weekBaseY + currentYOffset)

                    if (outsideText.isNotEmpty()) {
                        canvas.drawText(outsideText, sX(colLeaveTextX), rowY, centerBodyPaint)
                    }

                    if (isFullLeave) {
                        canvas.drawText(insideText, sX(colNormInX), rowY, centerBodyPaint)
                        canvas.drawText("-", sX(colNormOutX), rowY, centerBodyPaint)
                    } else if (log.computedNormalHours > 0f) {
                        canvas.drawText(log.normalTimeInStr, sX(colNormInX), rowY, centerBodyPaint)
                        canvas.drawText(log.normalTimeOutStr, sX(colNormOutX), rowY, centerBodyPaint)
                    }

                    var dayNormalHoursToPrint = log.computedNormalHours
                    val checkLabel = if (isFullLeave) insideText else outsideText

                    if (
                        dayNormalHoursToPrint == 0f &&
                        (checkLabel in payableLabels || insideText in payableLabels)
                    ) {
                        val isWeekend =
                            day.dayOfWeek == DayOfWeek.SATURDAY ||
                                day.dayOfWeek == DayOfWeek.SUNDAY

                        dayNormalHoursToPrint =
                            if (
                                profile.unit.contains("Clinic", true) ||
                                profile.unit.contains("Unit", true) ||
                                profile.unit.contains("OPD", true)
                            ) {
                                if (isWeekend) 6f else 8f
                            } else {
                                6f
                            }
                    }

                    if (dayNormalHoursToPrint > 0f) {
                        canvas.drawText(
                            formatHrs(dayNormalHoursToPrint),
                            sX(colNormHrsX),
                            rowY,
                            centerBodyPaint
                        )
                    }

                    val recordedOtHours = log.computedOtHours
                    if (recordedOtHours > 0f) {
                        canvas.drawText(log.otTimeInStr, sX(colOtInX), rowY, centerBodyPaint)
                        canvas.drawText(log.otTimeOutStr, sX(colOtOutX), rowY, centerBodyPaint)
                        canvas.drawText(
                            formatHrs(recordedOtHours),
                            sX(colOtHrsX),
                            rowY,
                            centerBodyPaint
                        )
                    }

                    val customReason =
                        if (!log.reason.isNullOrBlank() && log.reason != "Need for service") {
                            log.reason
                        } else {
                            null
                        }

                    if (customReason != null) {
                        canvas.drawText(
                            customReason,
                            sX(1870f),
                            rowY,
                            leftBodyPaint
                        )
                    }
                }
            }

            // Weekly recorded totals stay in their own source columns.
            val weeklyTotalY = sY(firstRowY + weekBaseY + 7f * rowHeight + 2f)
            canvas.drawText(
                formatHrs(weekRecordedDutyHours),
                sX(1349f),
                weeklyTotalY,
                centerBodyPaint
            )
            canvas.drawText(
                formatHrs(weekRecordedOtHours),
                sX(1789f),
                weeklyTotalY,
                centerBodyPaint
            )

            // Rightmost final payable OT result follows annotated reference anchors.
            val resultY = sY(resultYAnchors.getOrElse(weekIndex) { resultYAnchors.last() })

            val finalTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(
                    sinhalaTypeface ?: Typeface.DEFAULT,
                    Typeface.BOLD
                )
                textAlign = Paint.Align.CENTER
            }

            if (trueOtHours > 0f) {
                canvas.drawText(
                    formatHrs(trueOtHours),
                    sX(resultX),
                    resultY,
                    finalTotalPaint
                )
            }

            // Compact weekly equation directly below the result.
            val equationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 8.5f
                typeface = Typeface.create(
                    sinhalaTypeface ?: Typeface.DEFAULT,
                    Typeface.BOLD
                )
                textAlign = Paint.Align.CENTER
            }

            val equation =
                "${formatHoursForEquation(weekRecordedDutyHours)}-36+" +
                    "${formatHoursForEquation(weekRecordedOtHours)}=" +
                    "${formatHoursForEquation(trueOtHours)}"

            canvas.drawText(
                equation,
                sX(resultX),
                resultY + sY(equationOffsetY),
                equationPaint
            )

            weeklyTrueOtTotals.add(trueOtHours)

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

        val nonZeroWeeks = weeklyTrueOtTotals.filter { it > 0f }
        if (nonZeroWeeks.isNotEmpty()) {
            val equationString =
                nonZeroWeeks.joinToString(" + ") { formatHrs(it) } +
                    " = " +
                    formatHrs(nonZeroWeeks.sum())

            canvas.drawText(
                equationString,
                sX(1000f),
                sY(3415f),
                bottomEquationPaint
            )
        }

        document.finishPage(page)
    }

    private fun groupConsecutiveLeaves(logs: List<DailyLog>): List<LeaveBlock> {
        if (logs.isEmpty()) return emptyList()

        val sortedLogs = logs.sortedBy { it.date }
        val blocks = mutableListOf<LeaveBlock>()

        var currentStart = sortedLogs[0].date
        var currentEnd = sortedLogs[0].date
        var currentType = if (sortedLogs[0].isDO) "DO" else "CL"
        var currentCount = 1

        for (i in 1 until sortedLogs.size) {
            val log = sortedLogs[i]
            val type = if (log.isDO) "DO" else "CL"

            if (
                log.date == currentEnd.plusDays(1) &&
                type == currentType
            ) {
                currentEnd = log.date
                currentCount++
            } else {
                blocks.add(
                    LeaveBlock(
                        currentStart,
                        currentEnd,
                        currentType,
                        currentCount
                    )
                )
                currentStart = log.date
                currentEnd = log.date
                currentType = type
                currentCount = 1
            }
        }

        blocks.add(
            LeaveBlock(
                currentStart,
                currentEnd,
                currentType,
                currentCount
            )
        )

        return blocks
    }

    private fun filterFullWeekLogs(
        logs: List<DailyLog>,
        period: Period
    ): List<DailyLog> {
        val firstSunday =
            period.claimStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val lastSaturday =
            period.claimEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

        if (firstSunday.isAfter(lastSaturday)) {
            return emptyList()
        }

        return logs.filter {
            !it.date.isBefore(firstSunday) &&
                !it.date.isAfter(lastSaturday)
        }
    }

    private fun formatDouble(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun formatFloat(value: Float): String =
        if (value % 1 == 0f) {
            String.format(Locale.US, "%02d", value.toInt())
        } else {
            String.format(Locale.US, "%04.1f", value)
        }

    private fun formatHrs(value: Float): String {
        if (value <= 0f) return ""
        return "${formatFloat(value)}h"
    }

    private fun formatHoursForEquation(value: Float): String =
        if (value % 1 == 0f) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
}