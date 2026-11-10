package com.pasindu.nursingotapp.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * UI-04 verification tests for keyboard & window-insets configuration across root navigation,
 * scrollable form containers, and input dialogs.
 *
 * Note: These JVM unit tests verify structural modifier contracts and source invariants
 * (consumeWindowInsets + imePadding ordering before verticalScroll, scrollable input dialogs,
 * and replacement of fixed-height hospital-picker list clipping); actual soft-keyboard frame
 * geometry on a device/emulator requires Compose UI instrumentation tests.
 */
class Ui04KeyboardAndWindowInsetsTest {

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File("..", relativePath)
        )
        val file = candidates.firstOrNull { it.exists() && it.isFile }
            ?: error("Could not locate source file: $relativePath")
        return file.readText().replace("\r\n", "\n")
    }

    @Test
    fun rootNavHost_consumesScaffoldInsetsBeforeApplyingImePadding() {
        val navSource = readSource("src/main/java/com/pasindu/nursingotapp/ui/navigation/AppNavigation.kt")
        val navHostPattern = Regex(
            """\.padding\(scaffoldPadding\)\s*\.consumeWindowInsets\(scaffoldPadding\)\s*\.imePadding\(\)"""
        )
        assertTrue(
            "Expected root NavHost to apply .padding(scaffoldPadding).consumeWindowInsets(scaffoldPadding).imePadding()",
            navHostPattern.containsMatchIn(navSource)
        )
    }

    @Test
    fun scaffoldInputScreens_applyConsumeWindowInsetsAndImePaddingBeforeVerticalScroll() {
        val screenPaths = listOf(
            "src/main/java/com/pasindu/nursingotapp/ui/screens/AdvancedFinanceHubScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/VasoactiveInfusionsScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/ProfileScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/transfer/ui/TransferIdentityScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/ClinicalPlanningDashboardScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/DailyEntryScreen.kt"
        )

        for (path in screenPaths) {
            val source = readSource(path)
            val consumeIndex = source.indexOf(".consumeWindowInsets(")
            val imeIndex = source.indexOf(".imePadding()", startIndex = consumeIndex.coerceAtLeast(0))
            val scrollIndex = source.indexOf(".verticalScroll(", startIndex = imeIndex.coerceAtLeast(0))

            assertTrue("Expected .consumeWindowInsets(...) in $path", consumeIndex >= 0)
            assertTrue("Expected .imePadding() after .consumeWindowInsets(...) in $path", imeIndex > consumeIndex)
            assertTrue("Expected .verticalScroll(...) after .imePadding() in $path", scrollIndex > imeIndex)
        }
    }

    @Test
    fun calculatorScreens_applyImePaddingBeforeVerticalScroll() {
        val calculatorPaths = listOf(
            "src/main/java/com/pasindu/nursingotapp/ui/screens/BsaCalculatorScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/DosageCalculatorScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/WeightInfusionScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/EmergencyCalculatorsScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/PediatricRulesScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/screens/UnitConversionsScreen.kt",
            "src/main/java/com/pasindu/nursingotapp/ui/components/IvDripCalculatorCard.kt"
        )

        for (path in calculatorPaths) {
            val source = readSource(path)
            val imeIndex = source.indexOf(".imePadding()")
            val scrollIndex = source.indexOf(".verticalScroll(", startIndex = imeIndex.coerceAtLeast(0))
            assertTrue("Expected .imePadding() in $path", imeIndex >= 0)
            assertTrue("Expected .verticalScroll(...) after .imePadding() in $path", scrollIndex > imeIndex)
        }
    }

    @Test
    fun multiFieldDialogsAndHospitalPicker_supportKeyboardScrollingWithoutFixedHeightClipping() {
        val clinicalPlanning = readSource("src/main/java/com/pasindu/nursingotapp/ui/screens/ClinicalPlanningDashboardScreen.kt")
        val scrollMatches = Regex("""Column\(\s*modifier = Modifier\.verticalScroll\(rememberScrollState\(\)\)\.imePadding\(\)""")
            .findAll(clinicalPlanning)
            .count()
        assertTrue(
            "Expected both ISBAR and Task AlertDialogs in ClinicalPlanningDashboardScreen to be vertically scrollable with imePadding()",
            scrollMatches >= 2
        )

        val transferRequest = readSource("src/main/java/com/pasindu/nursingotapp/transfer/ui/TransferRequestScreen.kt")
        assertFalse(
            "Expected HospitalPickerDialog LazyColumn not to use a rigid .height(320.dp) that clips under keyboard",
            transferRequest.contains(".height(320.dp)")
        )
        assertTrue(
            "Expected HospitalPickerDialog LazyColumn to use .heightIn(max = 320.dp) with flexible weight",
            transferRequest.contains(".heightIn(max = 320.dp)")
        )
    }
}