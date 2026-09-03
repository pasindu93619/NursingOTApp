package com.pasindu.nursingotapp.ui.otforms

import androidx.lifecycle.ViewModel
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity
import com.pasindu.nursingotapp.data.model.Period
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.data.model.UserProfile
import com.pasindu.nursingotapp.domain.usecase.GenerateOtPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OtPdfGenerationViewModel @Inject constructor(
    private val generateOtPdfUseCase: GenerateOtPdfUseCase
) : ViewModel() {
    fun generate(
        profile: ProfileEntity,
        entries: List<DailyEntryEntity>,
        claimStart: LocalDate,
        claimEnd: LocalDate,
        renderer: (UserProfile, List<com.pasindu.nursingotapp.data.model.DailyLog>, Period, PeriodSummary) -> File?
    ): File? {
        val data = generateOtPdfUseCase(profile, entries, claimStart, claimEnd)
        return renderer(data.profile, data.logs, data.period, data.summary)
    }
}
