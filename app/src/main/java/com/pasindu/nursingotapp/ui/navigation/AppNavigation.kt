// com/pasindu/nursingotapp/ui/navigation/AppNavigation.kt
package com.pasindu.nursingotapp.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
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
import com.pasindu.nursingotapp.ui.NursingViewModel
import com.pasindu.nursingotapp.ui.otforms.PdfGenerator
import com.pasindu.nursingotapp.ui.screens.AnalyticsScreen
import com.pasindu.nursingotapp.ui.screens.ClaimPeriodScreen
import com.pasindu.nursingotapp.ui.screens.DailyEntryScreen
import com.pasindu.nursingotapp.ui.screens.ProfileScreen
import com.pasindu.nursingotapp.ui.screens.DosageCalculatorScreen
import com.pasindu.nursingotapp.ui.screens.WeightInfusionScreen
import com.pasindu.nursingotapp.ui.screens.BsaCalculatorScreen
import com.pasindu.nursingotapp.ui.screens.PediatricRulesScreen
import com.pasindu.nursingotapp.ui.screens.UnitConversionsScreen
import com.pasindu.nursingotapp.ui.screens.SpecialCalculationsScreen
import com.pasindu.nursingotapp.ui.screens.EmergencyCalculatorsScreen // NEW: Emergency route import
import com.pasindu.nursingotapp.ui.components.IvDripCalculatorCard
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import java.time.LocalDate

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: NursingViewModel = viewModel()
    val context = LocalContext.current

    val animDuration = 400

    NavHost(
        navController = navController,
        startDestination = "profile",
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) + fadeIn(animationSpec = tween(animDuration)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) + fadeOut(animationSpec = tween(animDuration)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) + fadeIn(animationSpec = tween(animDuration)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) + fadeOut(animationSpec = tween(animDuration)) }
    ) {
        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToClaimPeriod = { _, _ ->
                    navController.navigate("home") { popUpTo("profile") { inclusive = true } }
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
            AnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- HUB SCREEN ---
        composable("home") {
            com.pasindu.nursingotapp.ui.screens.HomeScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        // --- CLINICAL TOOLS MENU ---
        composable("clinical_tools") {
            com.pasindu.nursingotapp.ui.screens.ClinicalToolsScreen(
                onNavigateToIvDrip = { navController.navigate("iv_drip") },
                onNavigateToDosage = { navController.navigate("dosage_calc") },
                onNavigateToWeightInfusion = { navController.navigate("weight_infusion") },
                onNavigateToBsa = { navController.navigate("bsa_calc") },
                onNavigateToPediatric = { navController.navigate("pediatric_rules") },
                onNavigateToConversions = { navController.navigate("unit_conversions") },
                onNavigateToSpecialCalcs = { navController.navigate("special_calcs") },
                onNavigateToEmergency = { navController.navigate("emergency_calcs") }, // NEW: Emergency connection added here
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- INDIVIDUAL CALCULATORS ---
        composable("iv_drip") {
            Scaffold { padding ->
                IvDripCalculatorCard(modifier = Modifier.fillMaxSize().padding(padding))
            }
        }

        composable("dosage_calc") {
            DosageCalculatorScreen()
        }

        composable("weight_infusion") {
            WeightInfusionScreen()
        }

        composable("bsa_calc") {
            BsaCalculatorScreen()
        }

        composable("pediatric_rules") {
            PediatricRulesScreen()
        }

        composable("unit_conversions") {
            UnitConversionsScreen()
        }

        composable("special_calcs") {
            SpecialCalculationsScreen()
        }

        composable("emergency_calcs") { // NEW: Emergency Destination added here
            EmergencyCalculatorsScreen()
        }

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
                                id = entity.id, date = entity.date, isPH = entity.isPH, isDO = entity.isDO, isLeave = entity.isLeave, leaveType = entity.leaveType,
                                reason = entity.reason, wardOverride = entity.wardOverride, normalTimeInStr = entity.normalTimeIn, normalTimeOutStr = entity.normalTimeOut,
                                computedNormalHours = entity.normalHours, otTimeInStr = entity.otTimeIn, otTimeOutStr = entity.otTimeOut, computedOtHours = entity.otHours
                            )
                        }
                        val period = Period(LocalDate.parse(start), LocalDate.parse(end))
                        val totalNormalHrs = logs.sumOf { it.computedNormalHours.toDouble() }.toFloat()
                        val totalOtHrs = logs.sumOf { it.computedOtHours.toDouble() }.toFloat()
                        val phDays = logs.count { it.isPH }
                        val doDays = logs.count { it.isDO }
                        val dayRate = profile.basicSalary / 30.0

                        val summary = PeriodSummary(totalNormalHrs, totalOtHrs, phDays, doDays, totalOtHrs * profile.otRate, phDays * dayRate, doDays * dayRate, (totalOtHrs * profile.otRate) + (phDays * dayRate) + (doDays * dayRate))

                        val generator = PdfGenerator(context)
                        generator.generateAndReturnFile(profile, logs, period, summary)
                    } else null
                },
                onSaveAndSharePdf = { file ->
                    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share OT Claim Form"))
                }
            )
        }
    }
}