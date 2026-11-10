package com.pasindu.nursingotapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import kotlinx.coroutines.flow.Flow

internal const val MANAGED_DEDUCTION_NAME_APIT = "APIT"
internal const val MANAGED_DEDUCTION_NAME_WOP = "WOP"
internal const val MANAGED_DEDUCTION_NAME_LOAN = "Loan / Advance"
internal const val MANAGED_DEDUCTION_NAME_OTHER = "Other deduction"

internal fun isManagedCommitmentDeductionName(name: String): Boolean {
    val normalized = name.trim()
    return normalized.equals(MANAGED_DEDUCTION_NAME_APIT, ignoreCase = true) ||
        normalized.equals(MANAGED_DEDUCTION_NAME_WOP, ignoreCase = true) ||
        normalized.equals(MANAGED_DEDUCTION_NAME_LOAN, ignoreCase = true) ||
        normalized.equals(MANAGED_DEDUCTION_NAME_OTHER, ignoreCase = true)
}

internal fun mergeManagedCommitmentDeductionRows(
    claimPeriodId: Long,
    existingRows: List<ProfileDeductionEntity>,
    apit: Double,
    wop: Double,
    loanDeduction: Double,
    otherDeduction: Double,
    nowMillis: Long = System.currentTimeMillis()
): List<ProfileDeductionEntity> {
    val customRows = existingRows.filterNot { isManagedCommitmentDeductionName(it.name) }
    val managedEntries = listOf(
        MANAGED_DEDUCTION_NAME_APIT to apit,
        MANAGED_DEDUCTION_NAME_WOP to wop,
        MANAGED_DEDUCTION_NAME_LOAN to loanDeduction,
        MANAGED_DEDUCTION_NAME_OTHER to otherDeduction
    ).mapNotNull { (canonicalName, amount) ->
        val safeAmount = amount.takeIf { it.isFinite() && it > 0.0 } ?: return@mapNotNull null
        val existingMatch = existingRows.firstOrNull {
            it.name.trim().equals(canonicalName, ignoreCase = true)
        }
        ProfileDeductionEntity(
            id = existingMatch?.id ?: 0L,
            claimPeriodId = claimPeriodId,
            name = canonicalName,
            amount = safeAmount,
            updatedAt = nowMillis
        )
    }
    return customRows + managedEntries
}

@Dao
interface ProfileDeductionDao {
    @Query("SELECT * FROM profile_deductions WHERE claimPeriodId = :claimPeriodId ORDER BY id")
    fun observeForClaimPeriod(claimPeriodId: Long): Flow<List<ProfileDeductionEntity>>

    @Query("SELECT * FROM profile_deductions WHERE claimPeriodId = :claimPeriodId ORDER BY id")
    suspend fun getForClaimPeriodOnce(claimPeriodId: Long): List<ProfileDeductionEntity>

    @Query("DELETE FROM profile_deductions WHERE claimPeriodId = :claimPeriodId")
    suspend fun deleteForClaimPeriod(claimPeriodId: Long)

    @Upsert
    suspend fun upsertAll(items: List<ProfileDeductionEntity>)

    @Transaction
    suspend fun replaceManagedCommitmentsForClaimPeriod(
        claimPeriodId: Long,
        apit: Double,
        wop: Double,
        loanDeduction: Double,
        otherDeduction: Double,
        updatedAt: Long
    ): List<ProfileDeductionEntity> {
        val existingRows = getForClaimPeriodOnce(claimPeriodId)
        val mergedRows = mergeManagedCommitmentDeductionRows(
            claimPeriodId = claimPeriodId,
            existingRows = existingRows,
            apit = apit,
            wop = wop,
            loanDeduction = loanDeduction,
            otherDeduction = otherDeduction,
            nowMillis = updatedAt
        )
        deleteForClaimPeriod(claimPeriodId)
        if (mergedRows.isNotEmpty()) {
            upsertAll(mergedRows)
        }
        return getForClaimPeriodOnce(claimPeriodId)
    }
}
