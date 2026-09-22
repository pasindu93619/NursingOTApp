package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.Period
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.data.model.UserProfile
import com.pasindu.nursingotapp.domain.finance.FinancialSnapshot
import java.time.LocalDate

data class PdfGenerationData(
    val profile: UserProfile,
    val logs: List<DailyLog>,
    val period: Period,
    val summary: PeriodSummary,
    val financialSnapshot: FinancialSnapshot? = null
)

/**
 * PDF preparation now keeps the existing renderer contract while exposing the
 * same authoritative financial snapshot when policy rates/compensation are supplied.
 *
 * The legacy overload is retained for compatibility.
 */
class GenerateOtPdfUseCase {

    operator fun invoke(
        profileEntity: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ): PdfGenerationData {
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
        val dayRate = profileEntity.basicSalary.coerceAtLeast(0.0) / 30.0
        val otAmount = totalOtHours * profileEntity.otRate.coerceAtLeast(0.0)
        val phAmount = phDays * dayRate
        val doAmount = doDays * dayRate

        val summary = PeriodSummary(
            totalNormalHours = totalNormalHours,
            totalOTHours = totalOtHours,
            totalPHDays = phDays,
            totalDODays = doDays,
            otAmountRs = otAmount,
            phAmountRs = phAmount,
            doAmountRs = doAmount,
            totalAmountRs = otAmount + phAmount + doAmount
        )

        val profile = UserProfile(
            name = profileEntity.fullName,
            serviceNo = profileEntity.serviceNo,
            unit = profileEntity.unit,
            paySheetNo = profileEntity.paySheetNo,
            grade = profileEntity.grade,
            basicSalary = profileEntity.basicSalary,
            otRate = profileEntity.otRate
        )

        return PdfGenerationData(
            profile = profile,
            logs = logs,
            period = Period(claimStart, claimEnd),
            summary = summary
        )
    }
}
