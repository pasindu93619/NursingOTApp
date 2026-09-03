package com.pasindu.nursingotapp.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.pasindu.nursingotapp.data.local.entity.PaySheetDocumentEntity
import com.pasindu.nursingotapp.domain.usecase.ObservePaySheetDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PaySheetBankViewModel @Inject constructor(
    observePaySheetDocumentsUseCase: ObservePaySheetDocumentsUseCase
) : ViewModel() {
    val documents: StateFlow<List<PaySheetDocumentEntity>> =
        observePaySheetDocumentsUseCase()
            .stateIn(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate),
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
