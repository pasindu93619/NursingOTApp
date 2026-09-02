package com.pasindu.nursingotapp.ui.navigation

import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pasindu.nursingotapp.data.model.DailyLog
import com.pasindu.nursingotapp.data.model.Period
import com.pasindu.nursingotapp.data.model.PeriodSummary
import com.pasindu.nursingotapp.data.model.UserProfile
import com.pasindu.nursingotapp.ui.AdvancedFinanceViewModel
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.components.IvDripCalculatorCard
import com.pasindu.nursingotapp.ui.otforms.FileShareUtils
import com.pasindu.nursingotapp.ui.otforms.PdfGenerator
import com.pasindu.nursingotapp.ui.screens.*
import java.time.LocalDate

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: NursingViewModel = viewModel()
    val context = LocalContext.current

    val animDuration = 350

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) +
                androidx.compose.animation.fadeIn(animationSpec = tween(animDuration))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) +
                androidx.compose.animation.fadeOut(animationSpec = tween(animDuration))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) +
                androidx.compose.animation.fadeIn(animationSpec = tween(animDuration))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) +
                androidx.compose.animation.fadeOut(animationSpec = tween(animDuration))
        }
    ) {
        composable("home") {
            HomeScreen(viewModel = viewModel, onNavigate = { route -> navController.navigate(route) })
        }

        composable("nurse_command_center") {
            NurseCommandCenterScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("care_pulse") {
            CarePulseScreen(onNavigate = { route -> navController.navigate(route) }, onBack = { navController.popBackStack() })
        }

        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToClaimPeriod = { _, _ ->
                    navController.navigate("claim_period") { popUpTo("home") { inclusive = false } }
                }
            )
        }

        composable("claim_period") {
            ClaimPeriodScreen(
                onNavigateToDailyEntry = { claimPeriodId, start, end, wardType ->
                    navController.navigate("daily_entry/$claimPeriodId/$start/$end/$wardType")
                },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToAnalytics = { navController.navigate("analytics") }
            )
        }

        composable("analytics") {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("advanced_finance_hub") {
            val advancedFinanceViewModel: AdvancedFinanceViewModel = hiltViewModel()
            AdvancedFinanceHubScreen(
                viewModel = advancedFinanceViewModel,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable("pay_sheet_bank") {
            PaySheetBankScreen(onBack = { navController.popBackStack() })
        }

        composable("clinical_planning") {
            ClinicalPlanningDashboardScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("knowledge_hub") {
            KnowledgeHubScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("clinical_calculators") {
            ClinicalToolsScreen(
                onNavigateToIvDrip = { navController.navigate("iv_drip") },
                onNavigateToDosage = { navController.navigate("dosage_calc") },
                onNavigateToWeightInfusion = { navController.navigate("weight_infusion") },
                onNavigateToBsa = { navController.navigate("bsa_calc") },
                onNavigateToPediatric = { navController.navigate("pediatric_rules") },
                onNavigateToConversions = { navController.navigate("unit_conversions") },
                onNavigateToSpecialCalcs = { navController.navigate("special_calcs") },
                onNavigateToEmergency = { navController.navigate("emergency_calcs") },
                onNavigateToIcu = { navController.navigate("icu_calculators") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("iv_drip") {
            Scaffold { padding ->
                IvDripCalculatorCard(modifier = Modifier.fillMaxSize().padding(padding))
            }
        }

        composable("dosage_calc") { DosageCalculatorScreen() }
        composable("weight_infusion") { WeightInfusionScreen() }
        composable("bsa_calc") { BsaCalculatorScreen() }
        composable("pediatric_rules") { PediatricRulesScreen() }
        composable("unit_conversions") { UnitConversionsScreen() }
        composable("special_calcs") { SpecialCalculationsScreen() }
        composable("emergency_calcs") { EmergencyCalculatorsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("icu_calculators") { IcuCalculatorsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("vasoactive_infusions") { VasoactiveInfusionsScreen(onNavigateBack = { navController.popBackStack() }) }

        composable(
            route = "daily_entry/{claimPeriodId}/{start}/{end}/{wardType}",
            arguments = listOf(
                navArgument("claimPeriodId") { type = NavType.LongType },
                navArgument("start") { type = NavType.StringType },
                navArgument("end") { type = NavType.StringType },
                navArgument("wardType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val claimPeriodId = backStackEntry.arguments?.getLong("claimPeriodId") ?: 0L
            val start = backStackEntry.arguments?.getString("start") ?: ""
            val end = backStackEntry.arguments?.getString("end") ?: ""
            val wardType = backStackEntry.arguments?.getString("wardType") ?: "Normal"

            LaunchedEffect(claimPeriodId) { viewModel.loadEntriesForClaim(claimPeriodId) }

            DailyEntryScreen(
                claimPeriodId = claimPeriodId,
                startDateStr = start,
                endDateStr = end,
                wardType = wardType,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onGeneratePdfRequest = {
                    val dbLogs = viewModel.dailyLogs.value
                    val dbProfile = viewModel.userProfile.value
                    if (dbProfile != null) {
                        val profile = UserProfile(dbProfile.fullName, dbProfile.serviceNo, dbProfile.unit, dbProfile.paySheetNo, dbProfile.grade, dbProfile.basicSalary, dbProfile.otRate)
                        val logs = dbLogs.map { entity ->
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
                        val period = Period(LocalDate.parse(start), LocalDate.parse(end))
                        val totalNormalHrs = logs.sumOf { it.computedNormalHours.toDouble() }.toFloat()
                        val totalOtHrs = logs.sumOf { it.computedOtHours.toDouble() }.toFloat()
                        val phDays = logs.count { it.isPH }
                        val doDays = logs.count { it.isDO }
                        val dayRate = profile.basicSalary / 30.0
                        val summary = PeriodSummary(
                            totalNormalHrs,
                            totalOtHrs,
                            phDays,
                            doDays,
                            totalOtHrs * profile.otRate,
                            phDays * dayRate,
                            doDays * dayRate,
                            (totalOtHrs * profile.otRate) + (phDays * dayRate) + (doDays * dayRate)
                        )
                        PdfGenerator(context).generateAndReturnFile(profile, logs, period, summary)
                    } else null
                },
                onSaveAndSharePdf = { file ->
                    FileShareUtils.savePdfToDownloads(context, file)
                    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share OT Claim Form"))
                }
            )
        }
    }
}
