package com.pasindu.nursingotapp.domain.usecase

import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.domain.finance.FinanceCalculationEngine
import com.pasindu.nursingotapp.domain.finance.FinancialSnapshot
import java.time.LocalDate

/**
 * Prepares the existing PDF renderer from the same authoritative financial snapshot
 * used by the Finance UI. The legacy summary fields are retained for compatibility.
 */
class GenerateOtPdfUseCase {

    operator fun invoke(
        profileEntity: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate
    ): PdfGenerationData {
        val snapshot = FinanceCalculationEngine.calculate(
            profile = profileEntity,
            entries = entries,
            claimStart = claimStart,
            claimEnd = claimEnd,
            payRates = null,
            compensation = null
        )
        return PdfGenerationData.fromSnapshot(profileEntity, entries, snapshot)
    }
}

data class PdfGenerationData(
    val profile: com.pasindu.nursingotapp.data.model.UserProfile,
    val logs: List<com.pasindu.nursingotapp.data.model.DailyLog>,
    val period: com.pasindu.nursingotapp.data.model.Period,
    val summary: com.pasindu.nursingotapp.data.model.PeriodSummary,
    val financialSnapshot: FinancialSnapshot? = null
) {
    companion object {
        fun fromSnapshot(
            profileEntity: ProfileEntity,
            entries: List<DailyEntryEntity>,
            snapshot: FinancialSnapshot
        ): PdfGenerationData {
            val logs = entries.map { entity ->
                com.pasindu.nursingotapp.data.model.DailyLog(
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
            val summary = com.pasindu.nursingotapp.data.model.PeriodSummary(
                totalNormalHours = snapshot.normalDutyHours.toFloat(),
                totalOTHours = snapshot.totalOtHours.toFloat(),
                totalPHDays = snapshot.publicHolidayDays,
                totalDODays = snapshot.workingDayOffDays,
                otAmountRs = snapshot.otEarnings,
                phAmountRs = snapshot.phEarnings,
                doAmountRs = snapshot.doEarnings,
                totalAmountRs = snapshot.otEarnings + snapshot.phEarnings + snapshot.doEarnings
            )
            return PdfGenerationData(
                profile = com.pasindu.nursingotapp.data.model.UserProfile(
                    name = profileEntity.fullName,
                    serviceNo = profileEntity.serviceNo,
                    unit = profileEntity.unit,
                    paySheetNo = profileEntity.paySheetNo,
                    grade = profileEntity.grade,
                    basicSalary = profileEntity.basicSalary,
                    otRate = snapshot.otRate
                ),
                logs = logs,
                period = com.pasindu.nursingotapp.data.model.Period(snapshot.claimStart, snapshot.claimEnd),
                summary = summary,
                financialSnapshot = snapshot
            )
        }
    }
}
