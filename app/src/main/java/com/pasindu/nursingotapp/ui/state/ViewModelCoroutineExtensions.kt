package com.pasindu.nursingotapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Standard launch policy for one-shot ViewModel operations.
 * Cancellation is always rethrown; user-facing failures are converted to the
 * shared operation state instead of escaping as unhandled coroutine errors.
 */
fun ViewModel.launchOperation(
    setState: (ViewModelOperationState) -> Unit,
    operation: suspend () -> Unit
): Job = viewModelScope.launch {
    setState(ViewModelOperationState.Loading)
    try {
        operation()
        setState(ViewModelOperationState.Success)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        setState(
            ViewModelOperationState.Error(
                error.message?.takeIf { it.isNotBlank() } ?: "Operation failed"
            )
        )
    }
}
