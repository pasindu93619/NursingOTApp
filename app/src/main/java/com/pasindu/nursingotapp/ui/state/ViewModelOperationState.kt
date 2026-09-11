package com.pasindu.nursingotapp.ui.state

/**
 * Shared state for one-shot ViewModel operations.
 *
 * The state is presentation-only: it does not contain business or clinical
 * rules. Existing Room/domain use cases remain the source of truth.
 */
sealed interface ViewModelOperationState {
    data object Idle : ViewModelOperationState
    data object Loading : ViewModelOperationState
    data object Success : ViewModelOperationState
    data class Error(val message: String) : ViewModelOperationState
}
