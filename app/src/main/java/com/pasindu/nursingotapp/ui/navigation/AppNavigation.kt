package com.pasindu.nursingotapp.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.Period
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.data.model.UserProfile
import com.pasindu.nursingotapp.domain.ot.WeeklyOtCalculator
import com.pasindu.nursingotapp.ui.AdvancedFinanceViewModel
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.components.IvDripCalculatorCard
import com.pasindu.nursingotapp.ui.otforms.FileShareUtils
import com.pasindu.nursingotapp.ui.otforms.PdfGenerator
import com.pasindu.nursingotapp.ui.screens.*
import java.io.File
import java.time.LocalDate

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: NursingViewModel = hiltViewModel()
    val context = LocalContext.current
    val animDuration = 350

    NavHost(navController = navController, startDestination = "home", enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) + androidx.compose.animation.fadeIn(animationSpec = tween(animDuration)) }, exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) + androidx.compose.animation.fadeOut(animationSpec = tween(animDuration)) }, popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) + androidx.compose.animation.fadeIn(animationSpec = tween(animDuration)) }, popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) + androidx.compose.animation.fadeOut(animationSpec = tween(animDuration)) }) {
        composable("home") { HomeScreen(viewModel = viewModel, onNavigate = { route -> navController.navigate(route) }) }
        composable("nurse_command_center") { NurseCommandCenterScreen(onBack = { navController.popBackStack() }, onNavigate = { route -> navController.navigate(route) }) }
        composable("care_pulse") { CarePulseScreen(onNavigate = { route -> navController.navigate(route) }, onBack = { navController.popBackStack() }) }
        composable("profile") { ProfileScreen(viewModel = viewModel, onNavigateToClaimPeriod = { _, _ -> navController.navigate("claim_period") { popUpTo("home") { inclusive = false } } }) }
        composable("claim_period") { ClaimPeriodScreen(onNavigateToDailyEntry = { claimPeriodId, start, end, wardType -> navController.navigate("daily_entry/$claimPeriodId/$start/$end/$wardType") }, onNavigateToProfile = { navController.navigate("profile") }, onNavigateToAnalytics = { navController.navigate("analytics") }) }
        composable("analytics") { AnalyticsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("advanced_finance_hub") { val advancedFinanceViewModel: AdvancedFinanceViewModel = hiltViewModel(); AdvancedFinanceHubScreen(viewModel = advancedFinanceViewModel, onNavigate = { route -> navController.navigate(route) }, onBack = { navController.popBackStack() }) }
        composable("pay_sheet_bank") { PaySheetBankScreen(onBack = { navController.popBackStack() }) }
        composable("clinical_planning") { ClinicalPlanningDashboardScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("knowledge_hub") { KnowledgeHubScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("clinical_calculators") { ClinicalToolsScreen(onNavigateToIvDrip = { navController.navigate("iv_drip") }, onNavigateToDosage = { navController.navigate("dosage_calc") }, onNavigateToWeightInfusion = { navController.navigate("weight_infusion") }, onNavigateToBsa = { navController.navigate("bsa_calc") }, onNavigateToPediatric = { navController.navigate("pediatric_rules") }, onNavigateToConversions = { navController.navigate("unit_conversions") }, onNavigateToSpecialCalcs = { navController.navigate("special_calcs") }, onNavigateToEmergency = { navController.navigate("emergency_calcs") }, onNavigateToIcu = { navController.navigate("icu_calculators") }, onNavigateBack = { navController.popBackStack() }) }
        composable("iv_drip") { Scaffold { padding -> IvDripCalculatorCard(modifier = Modifier.fillMaxSize().padding(padding)) } }
        composable("dosage_calc") { DosageCalculatorScreen() }
        composable("weight_infusion") { WeightInfusionScreen() }
        composable("bsa_calc") { BsaCalculatorScreen() }
        composable("pediatric_rules") { PediatricRulesScreen() }
        composable("unit_conversions") { UnitConversionsScreen() }
        composable("special_calcs") {
            HighAlertClinicalWorkspaceScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenCalculator = { mode -> navController.navigate("special_calcs/${mode.name}") }
            )
        }
        composable(
            route = "special_calcs/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode")
                ?.let { runCatching { SpecialMode.valueOf(it) }.getOrNull() }
                ?: SpecialMode.INSULIN
            HighAlertCalculatorWorkspaceScreen(
                initialMode = mode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("emergency_calcs") { EmergencyCalculatorsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("icu_calculators") { IcuClinicalWorkspaceScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("vasoactive_infusions") { VasoactiveInfusionsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("daily_entry/{claimPeriodId}/{start}/{end}/{wardType}", arguments = listOf(navArgument("claimPeriodId") { type = NavType.LongType }, navArgument("start") { type = NavType.StringType }, navArgument("end") { type = NavType.StringType }, navArgument("wardType") { type = NavType.StringType })) { backStackEntry ->
            val claimPeriodId = backStackEntry.arguments?.getLong("claimPeriodId") ?: 0L
            val start = backStackEntry.arguments?.getString("start") ?: ""
            val end = backStackEntry.arguments?.getString("end") ?: ""
            val wardType = backStackEntry.arguments?.getString("wardType") ?: "Normal"
            LaunchedEffect(claimPeriodId) { viewModel.loadEntriesForClaim(claimPeriodId) }
            DailyEntryScreen(claimPeriodId = claimPeriodId, startDateStr = start, endDateStr = end, wardType = wardType, viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onGeneratePdfRequest = { generateOtPdf(context, viewModel, start, end) }, onSaveAndSharePdf = { file -> val shareUri = FileShareUtils.savePdfToDownloads(context, file); if (shareUri != null) { FileShareUtils.showSavedToast(context); shareSavedPdf(context, shareUri) } })
        }
    }
}

private fun generateOtPdf(context: Context, viewModel: NursingViewModel, start: String, end: String): File? {
    val dbLogs = viewModel.dailyLogs.value
    val dbProfile = viewModel.userProfile.value
    if (dbProfile == null) return null
    val configuredOtRate = viewModel.configuredOtRate.value
    val effectiveOtRate = configuredOtRate.takeIf { it > 0.0 } ?: dbProfile.otRate.coerceAtLeast(0.0)
    val profile = UserProfile(dbProfile.fullName, dbProfile.serviceNo, dbProfile.unit, dbProfile.paySheetNo, dbProfile.grade, dbProfile.basicSalary, effectiveOtRate)
    val logs = dbLogs.map { entity -> DailyLog(id = entity.id, date = entity.date, isPH = entity.isPH, isDO = entity.isDO, isLeave = entity.isLeave, leaveType = entity.leaveType, reason = entity.reason, wardOverride = entity.wardOverride, normalTimeInStr = entity.normalTimeIn, normalTimeOutStr = entity.normalTimeOut, computedNormalHours = entity.normalHours, otTimeInStr = entity.otTimeIn, otTimeOutStr = entity.otTimeOut, computedOtHours = entity.otHours) }
    val period = Period(LocalDate.parse(start), LocalDate.parse(end))
    val matched2027Basic = viewModel.matchedSalary2027.value?.basicSalary2027
    val workingDayRate = matched2027Basic?.takeIf { it > 0.0 }?.div(30.0) ?: profile.basicSalary.coerceAtLeast(0.0) / 30.0
    val calculation = WeeklyOtCalculator.calculate(logs = logs, claimStart = period.claimStart, claimEnd = period.claimEnd, otRate = profile.otRate.coerceAtLeast(0.0), dayRate = workingDayRate, doRate = workingDayRate)
    val summary = PeriodSummary(totalNormalHours = calculation.totalNormalHours.toFloat(), totalOTHours = calculation.totalOtHours.toFloat(), totalPHDays = calculation.phDays, totalDODays = calculation.doDays, otAmountRs = calculation.otAmountRs, phAmountRs = calculation.phAmountRs, doAmountRs = calculation.doAmountRs, totalAmountRs = calculation.totalAmountRs)
    return PdfGenerator(context).generateAndReturnFile(profile, logs, period, summary)
}

private fun shareSavedPdf(context: Context, uri: Uri) {
    try { val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(Intent.createChooser(shareIntent, "Share OT Claim PDF")) } catch (e: Exception) { e.printStackTrace(); Toast.makeText(context, "The PDF was saved, but sharing is unavailable.", Toast.LENGTH_LONG).show() }
}
