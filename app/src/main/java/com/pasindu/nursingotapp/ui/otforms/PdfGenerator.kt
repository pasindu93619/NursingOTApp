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
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import com.pasindu.nursingotapp.data.model.UserProfile
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
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9.5f; typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.NORMAL) }
    private val centerBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9.5f; typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.CENTER }
    private val leftBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9.5f; typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.LEFT }
    private val verticalBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 15f; typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
    private val bottomEquationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 14f; typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.LEFT }

    fun generateAndReturnFile(profile: UserProfile, logs: List<DailyLog>, period: Period, summary: PeriodSummary): File? {
        val document = PdfDocument()
        val rawFront = BitmapFactory.decodeResource(context.resources, R.drawable.form_front_bg)
        val rawBack = BitmapFactory.decodeResource(context.resources, R.drawable.form_back_bg)
        try {
            drawForm1(document, profile, period, summary, logs, rawFront)
            val fullWeekLogs = filterFullWeekLogs(logs, period)
            val weekGroups = fullWeekLogs.groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }.toSortedMap()
            drawForm2(document, weekGroups, profile, rawBack)
            val majorityMonth = logs.groupBy { it.date.month }.maxByOrNull { it.value.size }?.key?.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) ?: "Unknown"
            val outputDir = File(context.cacheDir, "pdf").apply { mkdirs() }
            val outputFile = File(outputDir, "OT_Form_$majorityMonth.pdf")
            FileOutputStream(outputFile).use { stream -> document.writeTo(stream) }
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace(); return null
        } finally {
            document.close(); rawFront?.recycle(); rawBack?.recycle()
        }
    }

    private fun drawForm1(document: PdfDocument, profile: UserProfile, period: Period, summary: PeriodSummary, logs: List<DailyLog>, background: Bitmap) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create())
        val canvas = page.canvas
        canvas.drawBitmap(background, null, android.graphics.Rect(0, 0, a4Width, a4Height), null)
        val originalImgW = 2475f; val originalImgH = 3500f
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

        // Existing coordinates are preserved. These exact cell-center coordinates are
        // supplied in the A4 (595 x 842) coordinate system of form_front_bg.
        val tableColumnCenters = floatArrayOf(422f, 982f, 1539f, 2096f)
        val phRowCenters = floatArrayOf(912f, 971f, 1029f, 1085f)
        val doRowCenters = floatArrayOf(1343f, 1401f, 1457f, 1513f)
        val leaveRowCenters = floatArrayOf(1773f, 1831f, 1889f, 1943f)

        // Do not pass these coordinates through sX()/sY(). They are already PDF/A4
        // coordinates, unlike the legacy coordinates elsewhere in this method.
        val exactTableX = tableColumnCenters
        val exactPhY = phRowCenters
        val exactDoY = doRowCenters
        val exactLeaveY = leaveRowCenters

        val workingPHs = logs.filter { it.isPH && it.computedNormalHours > 0f }.take(4)
        workingPHs.forEachIndexed { index, log ->
            val y = exactPhY[index]
            canvas.drawText(log.date.format(dateFormatter), exactTableX[0], y, centerBodyPaint)
            canvas.drawText("${log.normalTimeInStr}H", exactTableX[1], y, centerBodyPaint)
            canvas.drawText("${log.normalTimeOutStr}H", exactTableX[2], y, centerBodyPaint)
            canvas.drawText(formatHrs(log.computedNormalHours), exactTableX[3], y, centerBodyPaint)
        }

        val workingDOs = logs.filter { it.isDO && it.computedNormalHours > 0f }.take(4)
        workingDOs.forEachIndexed { index, log ->
            val y = exactDoY[index]
            canvas.drawText(log.date.format(dateFormatter), exactTableX[0], y, centerBodyPaint)
            canvas.drawText("${log.normalTimeInStr}H", exactTableX[1], y, centerBodyPaint)
            canvas.drawText("${log.normalTimeOutStr}H", exactTableX[2], y, centerBodyPaint)
            canvas.drawText(formatHrs(log.computedNormalHours), exactTableX[3], y, centerBodyPaint)
        }

        val leaveLogs = logs.filter { (it.isLeave && it.leaveType == "CL") || (it.isDO && it.computedNormalHours == 0f) }
        val groupedLeaves = groupConsecutiveLeaves(leaveLogs).take(4)
        groupedLeaves.forEachIndexed { index, leave ->
            val y = exactLeaveY[index]
            canvas.drawText(leave.startDate.format(dateFormatter), exactTableX[0], y, centerBodyPaint)
            canvas.drawText(leave.endDate.format(dateFormatter), exactTableX[1], y, centerBodyPaint)
            canvas.drawText(leave.type, exactTableX[2], y, centerBodyPaint)
            canvas.drawText(leave.totalDays.toString(), exactTableX[3], y, centerBodyPaint)
        }

        canvas.drawText(formatFloat(summary.totalOTHours), sX(990f), sY(2076f), centerBodyPaint); canvas.drawText(formatDouble(summary.otAmountRs), sX(2123f), sY(2076f), centerBodyPaint)
        canvas.drawText(summary.totalPHDays.toString(), sX(990f), sY(2150f), centerBodyPaint); canvas.drawText(formatDouble(summary.phAmountRs), sX(2123f), sY(2150f), centerBodyPaint)
        canvas.drawText(summary.totalDODays.toString(), sX(990f), sY(2226f), centerBodyPaint); canvas.drawText(formatDouble(summary.doAmountRs), sX(2123f), sY(2226f), centerBodyPaint)
        document.finishPage(page)
    }

    private fun drawForm2(document: PdfDocument, weekGroups: Map<LocalDate, List<DailyLog>>, profile: UserProfile, background: Bitmap) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create())
        val canvas = page.canvas
        canvas.drawBitmap(background, null, android.graphics.Rect(0, 0, a4Width, a4Height), null)
        val originalImgW = 2475f; val originalImgH = 3500f
        fun sX(x: Float): Float = x * (a4Width / originalImgW)
        fun sY(y: Float): Float = y * (a4Height / originalImgH)
        val yearY = sY(630f); val yearX = sX(953f); val monthX = sX(1900f)
        val colDateX = 593f; val colLeaveTextX = 893f; val colNormInX = 1010f; val colNormOutX = 1151f; val colNormHrsX = 1302f; val colOtInX = 1442f; val colOtOutX = 1585f; val colOtHrsX = 1722f
        val weekYOffset = 492.5f; val rowHeight = 71.25f
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val payableLabels = listOf("CL", "VL", "DL", "PH", "sL", "Special Leave", "Annual", "Sick", "CL/2", "Half Casual Leave")
        val firstWeekStart = weekGroups.keys.firstOrNull()
        if (firstWeekStart != null) { canvas.drawText(firstWeekStart.year.toString(), yearX, yearY, bodyPaint); canvas.drawText(firstWeekStart.month.name, monthX, yearY, bodyPaint) }

        val weeklyTrueOtTotals = mutableListOf<Float>()
        var weekIndex = 0
        for ((weekStart, weekLogsRaw) in weekGroups) {
            val weekLogs = weekLogsRaw.sortedBy { it.date }
            val daysOfWeek = (0..6).map { weekStart.plusDays(it.toLong()) }
            val authoritativeWeek = WeeklyOtCalculator.calculate(logs = weekLogs, claimStart = weekStart, claimEnd = weekStart.plusDays(6), otRate = 0.0, dayRate = 0.0, doRate = 0.0)
            val weekRecordedDutyHours = weekLogs.sumOf { it.computedNormalHours.toDouble().coerceAtLeast(0.0) }.toFloat()
            val weekRecordedOtHours = weekLogs.sumOf { it.computedOtHours.toDouble().coerceAtLeast(0.0) }.toFloat()
            val trueOtHours = (weekRecordedDutyHours - WeeklyOtCalculator.WEEKLY_NORMAL_LIMIT_HOURS.toFloat() + weekRecordedOtHours).coerceAtLeast(0f)
            val authoritativeAllocations = authoritativeWeek.allocations.associateBy { it.date }
            val weekBaseY = weekIndex * weekYOffset

            for ((dayIndex, day) in daysOfWeek.withIndex()) {
                val log = weekLogs.find { it.date == day }
                val currentYOffset = weekBaseY + (dayIndex * rowHeight)
                canvas.drawText(day.format(dateFormatter), sX(colDateX), sY(955f + currentYOffset), centerBodyPaint)
                if (log != null) {
                    val rawLeave = log.leaveType ?: ""
                    val isFullLeave = (log.isLeave && rawLeave != "SD") || ((log.isDO || log.isPH) && log.computedNormalHours == 0f && log.computedOtHours == 0f) || (rawLeave == "SD" && log.computedNormalHours == 0f && log.computedOtHours == 0f)
                    val isNight = log.normalTimeInStr.startsWith("19") || log.normalTimeInStr.startsWith("20") || log.otTimeInStr.startsWith("19") || log.otTimeInStr.startsWith("20")
                    var insideText = ""; var outsideText = ""
                    if (isFullLeave) {
                        if (log.isDO) insideText = "DO" else if (log.isPH) insideText = "PH" else insideText = when (rawLeave) { "Special", "Special Leave" -> "sL"; "Absent", "AB" -> "AB"; "CL", "VL", "DL", "SD" -> rawLeave; else -> rawLeave.take(4) }
                    } else {
                        val baseOutside = when { log.isDO -> "DO"; log.isPH -> "PH"; rawLeave == "SD" -> "SD"; rawLeave == "Short Leave" -> "SL"; rawLeave == "Half Casual Leave" -> "CL/2"; else -> "" }
                        outsideText = if (baseOutside.isNotEmpty()) { if (isNight) "$baseOutside/N" else baseOutside } else if (isNight) "N" else ""
                    }
                    if (outsideText.isNotEmpty()) canvas.drawText(outsideText, sX(colLeaveTextX), sY(958f + currentYOffset), centerBodyPaint)
                    if (isFullLeave) { canvas.drawText(insideText, sX(colNormInX), sY(958f + currentYOffset), centerBodyPaint); canvas.drawText("-", sX(colNormOutX), sY(958f + currentYOffset), centerBodyPaint) }
                    else if (log.computedNormalHours > 0f) { canvas.drawText(log.normalTimeInStr, sX(colNormInX), sY(958f + currentYOffset), centerBodyPaint); canvas.drawText(log.normalTimeOutStr, sX(colNormOutX), sY(958f + currentYOffset), centerBodyPaint) }
                    var dayNormalHoursToPrint = log.computedNormalHours
                    val checkLabel = if (isFullLeave) insideText else outsideText
                    if (dayNormalHoursToPrint == 0f && (checkLabel in payableLabels || insideText in payableLabels)) {
                        val isWknd = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
                        dayNormalHoursToPrint = if (profile.unit.contains("Clinic", true) || profile.unit.contains("Unit", true) || profile.unit.contains("OPD", true)) { if (isWknd) 6f else 8f } else 6f
                    }
                    if (dayNormalHoursToPrint > 0f) {
                        if (dayIndex < 6) canvas.drawText(formatHrs(dayNormalHoursToPrint), sX(colNormHrsX), sY(958f + currentYOffset), centerBodyPaint)
                        else canvas.drawText(formatHrs(dayNormalHoursToPrint), sX(1282f), sY(1357f + weekBaseY), centerBodyPaint)
                    }
                    val recordedOtHours = log.computedOtHours
                    if (recordedOtHours > 0f) {
                        canvas.drawText(log.otTimeInStr, sX(colOtInX), sY(958f + currentYOffset), centerBodyPaint)
                        canvas.drawText(log.otTimeOutStr, sX(colOtOutX), sY(958f + currentYOffset), centerBodyPaint)
                        if (dayIndex < 6) canvas.drawText(formatHrs(recordedOtHours), sX(colOtHrsX), sY(958f + currentYOffset), centerBodyPaint)
                        else canvas.drawText(formatHrs(recordedOtHours), sX(1735f), sY(1360f + weekBaseY), centerBodyPaint)
                    }
                    val customReason = when { !log.reason.isNullOrBlank() && log.reason != "Need for service" -> log.reason; else -> null }
                    if (customReason != null) canvas.drawText(customReason, sX(1870f), sY(958f + currentYOffset), leftBodyPaint)
                }
            }

            canvas.drawText(formatHrs(weekRecordedDutyHours), sX(1349f), sY(1384f + weekBaseY), centerBodyPaint)
            canvas.drawText(formatHrs(weekRecordedOtHours), sX(1789f), sY(1380f + weekBaseY), centerBodyPaint)

            val finalTotalX = 2133f
            val finalTotalY = 1364f + weekBaseY
            val finalTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            if (trueOtHours > 0f) canvas.drawText(formatHrs(trueOtHours), sX(finalTotalX), sY(finalTotalY), finalTotalPaint)

            // Visual calculation line, matching the filled-form example.
            // (Duty - 36) + OT = True OT.
            val equationY = 1411f + weekBaseY
            val equationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 8.5f; typeface = Typeface.create(sinhalaTypeface ?: Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            val equation = "${formatHoursForEquation(weekRecordedDutyHours)}-36+${formatHoursForEquation(weekRecordedOtHours)}=${formatHoursForEquation(trueOtHours)}"
            canvas.drawText(equation, sX(2133f), sY(equationY), equationPaint)

            weeklyTrueOtTotals.add(trueOtHours)
            weekIndex++
            if (weekIndex >= 5) break
        }

        canvas.save(); canvas.translate(sX(880f), sY(2156f)); canvas.rotate(-90f); canvas.drawText(profile.unit, 0f, 0f, verticalBoldPaint); canvas.restore()
        canvas.save(); canvas.translate(sX(1986f), sY(2153f)); canvas.rotate(-90f); canvas.drawText("Need for service", 0f, 0f, verticalBoldPaint); canvas.restore()

        val nonZeroWeeks = weeklyTrueOtTotals.filter { it > 0f }
        if (nonZeroWeeks.isNotEmpty()) {
            val equationString = nonZeroWeeks.joinToString(" + ") { formatHrs(it) } + " = " + formatHrs(nonZeroWeeks.sum())
            canvas.drawText(equationString, sX(1000f), sY(3415f), bottomEquationPaint)
        }
        document.finishPage(page)
    }

    private fun groupConsecutiveLeaves(logs: List<DailyLog>): List<LeaveBlock> {
        if (logs.isEmpty()) return emptyList(); val sortedLogs = logs.sortedBy { it.date }; val blocks = mutableListOf<LeaveBlock>(); var currentStart = sortedLogs[0].date; var currentEnd = sortedLogs[0].date; var currentType = if (sortedLogs[0].isDO) "DO" else "CL"; var currentCount = 1
        for (i in 1 until sortedLogs.size) { val log = sortedLogs[i]; val type = if (log.isDO) "DO" else "CL"; if (log.date == currentEnd.plusDays(1) && type == currentType) { currentEnd = log.date; currentCount++ } else { blocks.add(LeaveBlock(currentStart, currentEnd, currentType, currentCount)); currentStart = log.date; currentEnd = log.date; currentType = type; currentCount = 1 } }
        blocks.add(LeaveBlock(currentStart, currentEnd, currentType, currentCount)); return blocks
    }
    private fun filterFullWeekLogs(logs: List<DailyLog>, period: Period): List<DailyLog> { val firstSunday = period.claimStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)); val lastSaturday = period.claimEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)); if (firstSunday.isAfter(lastSaturday)) return emptyList(); return logs.filter { !it.date.isBefore(firstSunday) && !it.date.isAfter(lastSaturday) }
    }
    private fun formatDouble(value: Double): String = String.format(Locale.US, "%.2f", value)
    private fun formatFloat(value: Float): String = if (value % 1 == 0f) String.format(Locale.US, "%02d", value.toInt()) else String.format(Locale.US, "%04.1f", value)
    private fun formatHrs(value: Float): String { if (value <= 0f) return ""; return "${formatFloat(value)}h" }
    private fun formatHoursForEquation(value: Float): String = if (value % 1 == 0f) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}