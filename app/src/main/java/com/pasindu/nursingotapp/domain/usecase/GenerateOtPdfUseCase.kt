package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.Period
import com.pasindu.nursingotapp.data.model.PeriodSummary
import java.time.LocalDate

/**
 * Prepares the domain models required by the existing PDF renderer.
 * PDF rendering itself stays in the presentation/infrastructure layer.
 */
class GenerateOtPdfUseCase {
    operator fun invoke(
        profileEntity: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ): PdfGenerationData {
        val profile = com.pasindu.nursingotapp.data.model.UserProfile(
            name = profileEntity.fullName,
            serviceNo = profileEntity.serviceNo,
            unit = profileEntity.unit,
            paySheetNo = profileEntity.paySheetNo,
            grade = profileEntity.grade,
            basicSalary = profileEntity.basicSalary,
            otRate = profileEntity.otRate
        )

        val logs = entries.map { entity ->
            DailyLog(
                id = entity.id,
                date = entity.date,
                isPH = entity.isPH,
                isDO = entity.isDO,
                isLeave = entity.isLeave,
                leaveType = entity.leaveType,
                reason = entity.reason,
                wardOverride = entity.wardOverride,
                normalTimeInStr = entity.normalTimeIn,
                normalTimeOutStr = entity.normalTimeOut,
                computedNormalHours = entity.normalHours,
                otTimeInStr = entity.otTimeIn,
                otTimeOutStr = entity.otTimeOut,
                computedOtHours = entity.otHours
            )
        }

        val totalNormalHours = logs.sumOf { it.computedNormalHours.toDouble().coerceAtLeast(0.0) }.toFloat()
        val totalOtHours = logs.sumOf { it.computedOtHours.toDouble().coerceAtLeast(0.0) }.toFloat()
        val phDays = logs.count { it.isPH }
        val doDays = logs.count { it.isDO }
        val dayRate = profile.basicSalary / 30.0
        val otAmount = totalOtHours * profile.otRate
        val phAmount = phDays * dayRate
        val doAmount = doDays * dayRate
        val totalAmount = otAmount + phAmount + doAmount

        val summary = PeriodSummary(
            totalNormalHours,
            totalOtHours,
            phDays,
            doDays,
            otAmount,
            phAmount,
            doAmount,
            totalAmount
        )

        return PdfGenerationData(
            profile = profile,
            logs = logs,
            period = Period(claimStart, claimEnd),
            summary = summary
        )
    }
}

data class PdfGenerationData(
    val profile: com.pasindu.nursingotapp.data.model.UserProfile,
    val logs: List<DailyLog>,
    val period: Period,
    val summary: PeriodSummary
)
