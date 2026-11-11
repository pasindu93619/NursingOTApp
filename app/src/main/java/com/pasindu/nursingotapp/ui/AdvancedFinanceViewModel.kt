package com.pasindu.nursingotapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.data.local.dao.MANAGED_DEDUCTION_NAME_APIT
import com.pasindu.nursingotapp.data.local.dao.MANAGED_DEDUCTION_NAME_LOAN
import com.pasindu.nursingotapp.data.local.dao.MANAGED_DEDUCTION_NAME_OTHER
import com.pasindu.nursingotapp.data.local.dao.MANAGED_DEDUCTION_NAME_WOP
import com.pasindu.nursingotapp.data.local.dao.ProfileDeductionDao
import com.pasindu.nursingotapp.data.local.dao.isManagedCommitmentDeductionName
import com.pasindu.nursingotapp.data.local.dao.mergeManagedCommitmentDeductionRows
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.PayRateSettingsEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileCompensationEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileDeductionEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.domain.usecase.ApplyFinancePolicyRatesUseCase
import com.pasindu.nursingotapp.domain.usecase.CalculateFinanceSummaryUseCase
import com.pasindu.nursingotapp.domain.usecase.EnsureManualPayRateRecordUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimDailyEntriesUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveClaimPeriodsUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveOtRateUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.ObserveProfileUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceCompensationUseCase
import com.pasindu.nursingotapp.domain.usecase.SaveFinanceRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal const val DEDUCTION_NAME_APIT = MANAGED_DEDUCTION_NAME_APIT
internal const val DEDUCTION_NAME_WOP = MANAGED_DEDUCTION_NAME_WOP
internal const val DEDUCTION_NAME_LOAN = MANAGED_DEDUCTION_NAME_LOAN
internal const val DEDUCTION_NAME_OTHER = MANAGED_DEDUCTION_NAME_OTHER

internal data class CommitmentAmounts(
    val apit: Double = 0.0,
    val wop: Double = 0.0,
    val loanDeduction: Double = 0.0,
    val otherDeduction: Double = 0.0
)

internal fun formatCommitmentInput(value: Double): String {
    if (!value.isFinite() || value <= 0.0) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

internal fun parseStrictMoneyOrNull(value: String): Double? {
    val cleaned = value.trim().replace(",", "")
    if (cleaned.isEmpty()) return null
    val parsed = cleaned.toDoubleOrNull() ?: return null
    if (!parsed.isFinite() || parsed < 0.0) return null
    return parsed
}

internal fun isManagedCommitmentName(name: String): Boolean =
    isManagedCommitmentDeductionName(name)

internal fun findManagedDeductionAmount(
    deductions: List<ProfileDeductionEntity>,
    canonicalName: String
): Double =
    deductions
        .firstOrNull { it.name.trim().equals(canonicalName, ignoreCase = true) }
        ?.amount
        ?.coerceAtLeast(0.0)
        ?: 0.0

internal fun mergeManagedCommitmentDeductions(
    claimPeriodId: Long,
    existingRows: List<ProfileDeductionEntity>,
    commitments: CommitmentAmounts,
    nowMillis: Long = System.currentTimeMillis()
): List<ProfileDeductionEntity> =
    mergeManagedCommitmentDeductionRows(
        claimPeriodId = claimPeriodId,
        existingRows = existingRows,
        apit = commitments.apit,
        wop = commitments.wop,
        loanDeduction = commitments.loanDeduction,
        otherDeduction = commitments.otherDeduction,
        nowMillis = nowMillis
    )

internal fun resolveCommitmentInputText(
    currentInput: String,
    newAmount: Double
): String {
    if (currentInput.isEmpty()) {
        return formatCommitmentInput(newAmount)
    }
    val parsedCurrent = parseStrictMoneyOrNull(currentInput)
    if (parsedCurrent != null && abs(parsedCurrent - newAmount) < 0.0001) {
        return currentInput
    }
    return formatCommitmentInput(newAmount)
}

data class AdvancedFinanceUiState(
    val isLoading: Boolean = true,
    val profile: ProfileEntity? = null,
    val claimPeriod: ClaimPeriodEntity? = null,
    val periodSummary: PeriodSummary? = null,
    val payRateSettings: PayRateSettingsEntity? = null,
    val compensation: ProfileCompensationEntity? = null,
    val deductions: List<ProfileDeductionEntity> = emptyList(),
    val claimStart: java.time.LocalDate? = null,
    val claimEnd: java.time.LocalDate? = null,
    val fullWeeks: Int = 0,
    val apit: Double = 0.0,
    val wop: Double = 0.0,
    val loanDeduction: Double = 0.0,
    val otherDeduction: Double = 0.0,
    val apitInput: String = formatCommitmentInput(apit),
    val wopInput: String = formatCommitmentInput(wop),
    val loanDeductionInput: String = formatCommitmentInput(loanDeduction),
    val otherDeductionInput: String = formatCommitmentInput(otherDeduction),
    val loadedDeductionsClaimPeriodId: Long? = null,
    val errorMessage: String? = null
) {
    val currentBasicSalary: Double
        get() {
            val is2027OrLater = (claimStart?.year ?: 0) >= 2027
            return if (is2027OrLater && basisSalary2027 != null && basisSalary2027!! > 0.0) {
                basisSalary2027!!
            } else {
                profile?.basicSalary ?: 0.0
            }
        }
    val riskAllowance: Double get() = compensation?.riskAllowance ?: 0.0
    val claAllowance: Double get() = compensation?.claAllowance ?: 0.0
    val additionalAllowancesTotal: Double get() = compensation?.additionalAllowancesTotal ?: 0.0
    /**
     * Total Paysheet Deductions is the authoritative payroll aggregate entered
     * on the nurse profile. Claim-period named deduction rows are kept separate
     * and must never replace or double-count this paysheet total.
     */
    val paysheetDeductions: Double
        get() = compensation?.totalDeductions?.coerceAtLeast(0.0) ?: 0.0
    val otRate: Double get() = payRateSettings?.otRate?.coerceAtLeast(0.0) ?: 0.0
    val phRate: Double get() = payRateSettings?.phRate?.coerceAtLeast(0.0) ?: 0.0
    val doRate: Double get() = payRateSettings?.doRate?.coerceAtLeast(0.0) ?: 0.0
    val basisSalary2027: Double? get() = payRateSettings?.basisSalary2027
    val ratesMissing: Boolean
        get() = periodSummary?.ratesMissing == true || phRate <= 0.0 || doRate <= 0.0
    val totalNormalHours: Double get() = periodSummary?.totalNormalHours?.toDouble() ?: 0.0
    val totalOTHours: Double get() = periodSummary?.totalOTHours?.toDouble() ?: 0.0
    val totalPHDays: Int get() = periodSummary?.totalPHDays ?: 0
    val totalDODays: Int get() = periodSummary?.totalDODays ?: 0
    val otAmountRs: Double get() = periodSummary?.otAmountRs ?: (totalOTHours * otRate)
    val phAmountRs: Double get() = periodSummary?.phAmountRs ?: (totalPHDays * phRate)
    val doAmountRs: Double get() = periodSummary?.doAmountRs ?: (totalDODays * doRate)
    val grossEarnings: Double get() = currentBasicSalary + riskAllowance + claAllowance + additionalAllowancesTotal + otAmountRs + phAmountRs + doAmountRs
    val estimatedNetSalary: Double get() = grossEarnings - paysheetDeductions
    val hasEnteredDeductions: Boolean
        get() = (compensation?.totalDeductions ?: 0.0) > 0.0
    val workloadTargetHours: Double get() = fullWeeks * 36.0
    val dutyProgress36Hours: Float get() = if (workloadTargetHours <= 0.0) 0f else (totalNormalHours / workloadTargetHours).coerceIn(0.0, 1.0).toFloat()
}

@HiltViewModel
class AdvancedFinanceViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
    observeClaimPeriod: ObserveClaimPeriodsUseCase,
    observePayRates: ObserveOtRateUseCase,
    observeCompensation: ObserveProfileCompensationUseCase,
    private val profileDeductionDao: ProfileDeductionDao,
    private val ensureManualPayRateRecordUseCase: EnsureManualPayRateRecordUseCase,
    private val applyFinancePolicyRatesUseCase: ApplyFinancePolicyRatesUseCase,
    private val observeClaimDailyEntriesUseCase: ObserveClaimDailyEntriesUseCase,
    private val calculateFinanceSummaryUseCase: CalculateFinanceSummaryUseCase,
    private val saveFinanceCompensationUseCase: SaveFinanceCompensationUseCase,
    private val saveFinanceRatesUseCase: SaveFinanceRatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedFinanceUiState())
    val uiState: StateFlow<AdvancedFinanceUiState> = _uiState.asStateFlow()

    private var deductionObservationJob: Job? = null
    private var dailyEntriesObservationJob: Job? = null
    private var recalculateJob: Job? = null
    private var deductionWriteJob: Job? = null
    private val deductionWriteMutex = Mutex()
    private var activeClaimPeriodId: Long? = null
    private var entriesLoadedClaimPeriodId: Long? = null
    private var latestObservedEntries: List<DailyEntryEntity> = emptyList()
    private val inFlightWriteCountByPeriod = mutableMapOf<Long, Int>()
    private val writeVersionByPeriod = mutableMapOf<Long, Long>()
    private val latestCommitmentsByPeriod = mutableMapOf<Long, CommitmentAmounts>()

    init {
        observeProfile().collectInViewModel { profile ->
            _uiState.value = _uiState.value.copy(profile = profile)
            viewModelScope.launch {
                runCatching {
                    ensureManualPayRateRecordUseCase()
                    if (profile != null) applyFinancePolicyRatesUseCase(profile)
                    recalculate()
                }.onFailure { error ->
                    setError(error, "Unable to initialize financial information.")
                }
            }
        }
        observeClaimPeriod().collectInViewModel { periods ->
            val current = periods.firstOrNull()
            val newPeriodId = current?.id
            val periodChanged = newPeriodId != activeClaimPeriodId
            activeClaimPeriodId = newPeriodId

            if (periodChanged) {
                deductionObservationJob?.cancel()
                deductionObservationJob = null
                dailyEntriesObservationJob?.cancel()
                dailyEntriesObservationJob = null
                entriesLoadedClaimPeriodId = null
                latestObservedEntries = emptyList()
                _uiState.value = _uiState.value.copy(
                    claimPeriod = current,
                    deductions = emptyList(),
                    apit = 0.0,
                    wop = 0.0,
                    loanDeduction = 0.0,
                    otherDeduction = 0.0,
                    apitInput = "",
                    wopInput = "",
                    loanDeductionInput = "",
                    otherDeductionInput = "",
                    loadedDeductionsClaimPeriodId = null
                )
                if (newPeriodId != null) {
                    observeDeductionsForClaimPeriod(newPeriodId)
                    observeDailyEntriesForClaimPeriod(newPeriodId)
                }
            } else {
                _uiState.value = _uiState.value.copy(claimPeriod = current)
            }
            recalculate()
        }
        observePayRates().collectInViewModel { settings ->
            _uiState.value = _uiState.value.copy(payRateSettings = settings)
            recalculate()
        }
        observeCompensation().collectInViewModel { compensation ->
            _uiState.value = _uiState.value.copy(compensation = compensation)
            recalculate()
        }
    }

    private fun observeDailyEntriesForClaimPeriod(claimPeriodId: Long) {
        dailyEntriesObservationJob?.cancel()
        dailyEntriesObservationJob = viewModelScope.launch {
            observeClaimDailyEntriesUseCase(claimPeriodId).collect { entries ->
                if (activeClaimPeriodId != claimPeriodId) return@collect
                entriesLoadedClaimPeriodId = claimPeriodId
                latestObservedEntries = entries
                recalculate(entriesOverride = entries, entriesClaimPeriodId = claimPeriodId)
            }
        }
    }

    private fun observeDeductionsForClaimPeriod(claimPeriodId: Long) {
        deductionObservationJob?.cancel()
        deductionObservationJob = viewModelScope.launch {
            profileDeductionDao.observeForClaimPeriod(claimPeriodId).collect { deductions ->
                if ((inFlightWriteCountByPeriod[claimPeriodId] ?: 0) > 0 ||
                    activeClaimPeriodId != claimPeriodId
                ) {
                    return@collect
                }
                applyObservedDeductions(claimPeriodId, deductions)
            }
        }
    }

    private fun applyObservedDeductions(
        claimPeriodId: Long,
        deductions: List<ProfileDeductionEntity>
    ) {
        val currentState = _uiState.value
        if (currentState.claimPeriod?.id != claimPeriodId || activeClaimPeriodId != claimPeriodId) return

        val apitAmount = findManagedDeductionAmount(deductions, DEDUCTION_NAME_APIT)
        val wopAmount = findManagedDeductionAmount(deductions, DEDUCTION_NAME_WOP)
        val loanAmount = findManagedDeductionAmount(deductions, DEDUCTION_NAME_LOAN)
        val otherAmount = findManagedDeductionAmount(deductions, DEDUCTION_NAME_OTHER)

        latestCommitmentsByPeriod[claimPeriodId] = CommitmentAmounts(
            apit = apitAmount,
            wop = wopAmount,
            loanDeduction = loanAmount,
            otherDeduction = otherAmount
        )

        _uiState.value = currentState.copy(
            deductions = deductions,
            apit = apitAmount,
            wop = wopAmount,
            loanDeduction = loanAmount,
            otherDeduction = otherAmount,
            apitInput = resolveCommitmentInputText(currentState.apitInput, apitAmount),
            wopInput = resolveCommitmentInputText(currentState.wopInput, wopAmount),
            loanDeductionInput = resolveCommitmentInputText(currentState.loanDeductionInput, loanAmount),
            otherDeductionInput = resolveCommitmentInputText(currentState.otherDeductionInput, otherAmount),
            loadedDeductionsClaimPeriodId = claimPeriodId
        )
        recalculate()
    }

    private fun recalculate(
        entriesOverride: List<DailyEntryEntity>? = null,
        entriesClaimPeriodId: Long? = null,
        forceReloadEntries: Boolean = false
    ) {
        val state = _uiState.value
        val profile = state.profile ?: run {
            recalculateJob?.cancel()
            _uiState.value = state.copy(isLoading = false, periodSummary = null, claimStart = null, claimEnd = null, errorMessage = null)
            return
        }
        val claimPeriod = state.claimPeriod ?: run {
            recalculateJob?.cancel()
            _uiState.value = state.copy(isLoading = false, periodSummary = null, claimStart = null, claimEnd = null, fullWeeks = 0, errorMessage = null)
            return
        }

        recalculateJob?.cancel()
        recalculateJob = viewModelScope.launch {
            runCatching {
                val entries = when {
                    entriesOverride != null && entriesClaimPeriodId == claimPeriod.id -> entriesOverride
                    !forceReloadEntries && entriesLoadedClaimPeriodId == claimPeriod.id -> latestObservedEntries
                    else -> observeClaimDailyEntriesUseCase(claimPeriod.id).first().also { fetched ->
                        if (activeClaimPeriodId == claimPeriod.id) {
                            entriesLoadedClaimPeriodId = claimPeriod.id
                            latestObservedEntries = fetched
                        }
                    }
                }
                if (activeClaimPeriodId != claimPeriod.id) return@launch
                val latestState = _uiState.value
                val latestProfile = latestState.profile ?: return@launch
                val latestPeriod = latestState.claimPeriod ?: return@launch
                if (latestPeriod.id != claimPeriod.id) return@launch

                val summary = calculateFinanceSummaryUseCase(
                    profile = latestProfile,
                    entries = entries,
                    claimStart = latestPeriod.startDate,
                    claimEnd = latestPeriod.endDate,
                    payRates = latestState.payRateSettings
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    periodSummary = summary,
                    claimStart = latestPeriod.startDate,
                    claimEnd = latestPeriod.endDate,
                    fullWeeks = countFullSundaySaturdayWeeks(latestPeriod.startDate, latestPeriod.endDate),
                    errorMessage = null
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                setError(error, "Unable to calculate financial summary.")
            }
        }
    }

    fun refresh() = recalculate(forceReloadEntries = true)

    fun updateApit(value: String) = updateManagedCommitment(value) { state, parsedAmount ->
        state.copy(
            apit = parsedAmount ?: state.apit,
            apitInput = value
        )
    }

    fun updateWop(value: String) = updateManagedCommitment(value) { state, parsedAmount ->
        state.copy(
            wop = parsedAmount ?: state.wop,
            wopInput = value
        )
    }

    fun updateLoanDeduction(value: String) = updateManagedCommitment(value) { state, parsedAmount ->
        state.copy(
            loanDeduction = parsedAmount ?: state.loanDeduction,
            loanDeductionInput = value
        )
    }

    fun updateOtherDeduction(value: String) = updateManagedCommitment(value) { state, parsedAmount ->
        state.copy(
            otherDeduction = parsedAmount ?: state.otherDeduction,
            otherDeductionInput = value
        )
    }

    private fun updateManagedCommitment(
        rawInput: String,
        reducer: (AdvancedFinanceUiState, Double?) -> AdvancedFinanceUiState
    ) {
        val parsedOrCleared: Double? = when {
            rawInput.isBlank() -> 0.0
            else -> parseStrictMoneyOrNull(rawInput)
        }
        val updatedState = reducer(_uiState.value, parsedOrCleared)
        _uiState.value = updatedState

        if (parsedOrCleared == null) return
        val targetClaimPeriodId = updatedState.claimPeriod?.id ?: return
        if (activeClaimPeriodId != targetClaimPeriodId) return

        val nextVersion = (writeVersionByPeriod[targetClaimPeriodId] ?: 0L) + 1L
        writeVersionByPeriod[targetClaimPeriodId] = nextVersion
        latestCommitmentsByPeriod[targetClaimPeriodId] = CommitmentAmounts(
            apit = updatedState.apit,
            wop = updatedState.wop,
            loanDeduction = updatedState.loanDeduction,
            otherDeduction = updatedState.otherDeduction
        )
        persistCommitmentDeductions(targetClaimPeriodId)
    }

    private fun persistCommitmentDeductions(targetClaimPeriodId: Long) {
        inFlightWriteCountByPeriod[targetClaimPeriodId] =
            (inFlightWriteCountByPeriod[targetClaimPeriodId] ?: 0) + 1
        deductionWriteJob = viewModelScope.launch {
            try {
                deductionWriteMutex.withLock {
                    val commitments = latestCommitmentsByPeriod[targetClaimPeriodId] ?: return@withLock
                    val writeVersion = writeVersionByPeriod[targetClaimPeriodId] ?: 0L
                    runCatching {
                        val persistedRows = withContext(NonCancellable) {
                            profileDeductionDao.replaceManagedCommitmentsForClaimPeriod(
                                claimPeriodId = targetClaimPeriodId,
                                apit = commitments.apit,
                                wop = commitments.wop,
                                loanDeduction = commitments.loanDeduction,
                                otherDeduction = commitments.otherDeduction,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        val isLatestWriteForPeriod =
                            (writeVersionByPeriod[targetClaimPeriodId] ?: 0L) == writeVersion
                        if (isLatestWriteForPeriod &&
                            activeClaimPeriodId == targetClaimPeriodId &&
                            _uiState.value.claimPeriod?.id == targetClaimPeriodId
                        ) {
                            applyObservedDeductions(targetClaimPeriodId, persistedRows)
                        }
                    }.onFailure { error ->
                        val rolledBackRows = runCatching {
                            profileDeductionDao.getForClaimPeriodOnce(targetClaimPeriodId)
                        }.getOrNull()
                        if (rolledBackRows != null) {
                            val apitAmount = findManagedDeductionAmount(rolledBackRows, DEDUCTION_NAME_APIT)
                            val wopAmount = findManagedDeductionAmount(rolledBackRows, DEDUCTION_NAME_WOP)
                            val loanAmount = findManagedDeductionAmount(rolledBackRows, DEDUCTION_NAME_LOAN)
                            val otherAmount = findManagedDeductionAmount(rolledBackRows, DEDUCTION_NAME_OTHER)
                            latestCommitmentsByPeriod[targetClaimPeriodId] = CommitmentAmounts(
                                apit = apitAmount,
                                wop = wopAmount,
                                loanDeduction = loanAmount,
                                otherDeduction = otherAmount
                            )
                            if (activeClaimPeriodId == targetClaimPeriodId &&
                                _uiState.value.claimPeriod?.id == targetClaimPeriodId
                            ) {
                                _uiState.value = _uiState.value.copy(
                                    deductions = rolledBackRows,
                                    apit = apitAmount,
                                    wop = wopAmount,
                                    loanDeduction = loanAmount,
                                    otherDeduction = otherAmount,
                                    apitInput = formatCommitmentInput(apitAmount),
                                    wopInput = formatCommitmentInput(wopAmount),
                                    loanDeductionInput = formatCommitmentInput(loanAmount),
                                    otherDeductionInput = formatCommitmentInput(otherAmount),
                                    loadedDeductionsClaimPeriodId = targetClaimPeriodId
                                )
                            }
                        }
                        if (activeClaimPeriodId == targetClaimPeriodId) {
                            setError(error, "Unable to save financial commitments.")
                        }
                    }
                }
            } finally {
                val remaining = (inFlightWriteCountByPeriod[targetClaimPeriodId] ?: 1) - 1
                if (remaining <= 0) {
                    inFlightWriteCountByPeriod.remove(targetClaimPeriodId)
                } else {
                    inFlightWriteCountByPeriod[targetClaimPeriodId] = remaining
                }
            }
        }
    }

    fun updateOtRate(value: String) = saveRates(parseMoney(value), _uiState.value.phRate, _uiState.value.doRate, _uiState.value.basisSalary2027, "2027_BASIC_SALARY_DIV_30")
    fun updatePhRate(value: String) = saveRates(_uiState.value.otRate, parseMoney(value), _uiState.value.doRate, _uiState.value.basisSalary2027, "MANUAL")
    fun updateDoRate(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, parseMoney(value), _uiState.value.basisSalary2027, "MANUAL")
    fun updateBasisSalary2027(value: String) = saveRates(_uiState.value.otRate, _uiState.value.phRate, _uiState.value.doRate, value.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 }, _uiState.value.payRateSettings?.rateSource ?: "MANUAL")

    fun apply2027DayRateFromSalary(basisSalary2027: Double) {
        if (basisSalary2027 <= 0.0) return
    }

    fun saveCompensation(riskAllowance: Double, claAllowance: Double, additionalAllowancesTotal: Double, totalDeductions: Double) {
        launchWithError("Unable to save compensation information.") {
            saveFinanceCompensationUseCase(riskAllowance, claAllowance, additionalAllowancesTotal, totalDeductions)
        }
    }

    private fun saveRates(otRate: Double, phRate: Double, doRate: Double, basisSalary2027: Double?, source: String) {
        launchWithError("Unable to save pay rate settings.") {
            saveFinanceRatesUseCase(otRate, phRate, doRate, basisSalary2027, source)
        }
    }

    private fun launchWithError(message: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { error -> setError(error, message) }
        }
    }

    private fun setError(error: Throwable, fallbackMessage: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = error.message?.takeIf { it.isNotBlank() } ?: fallbackMessage
        )
    }

    private fun parseMoney(value: String): Double = value.trim().replace(",", "").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    private fun <T> kotlinx.coroutines.flow.Flow<T>.collectInViewModel(block: (T) -> Unit) {
        viewModelScope.launch { collect { block(it) } }
    }
}


private fun countFullSundaySaturdayWeeks(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Int {
    if (endDate.isBefore(startDate)) return 0
    val firstSunday = if (startDate.dayOfWeek == DayOfWeek.SUNDAY) startDate
    else startDate.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SUNDAY))
    val lastSaturday = if (endDate.dayOfWeek == DayOfWeek.SATURDAY) endDate
    else endDate.with(java.time.temporal.TemporalAdjusters.previous(DayOfWeek.SATURDAY))
    if (firstSunday.isAfter(lastSaturday)) return 0
    return java.time.temporal.ChronoUnit.WEEKS
        .between(firstSunday, lastSaturday.plusDays(1))
        .toInt()
}
