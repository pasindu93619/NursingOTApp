package com.pasindu.nursingotapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Root application class for dependency injection.
 *
 * This is intentionally the only Hilt bootstrap change in this step.
 * Existing screens and ViewModels continue using their current wiring until
 * each module is migrated incrementally and verified.
 */
@HiltAndroidApp
class NursingOTAppApplication : Application()
