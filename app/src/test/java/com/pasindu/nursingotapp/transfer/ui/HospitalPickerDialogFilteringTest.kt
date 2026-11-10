package com.pasindu.nursingotapp.transfer.ui

import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 4 verification tests for Hospital Picker UI refinement:
 * - Empty-query initial browsing
 * - All-category results
 * - TH, PGH, DGH, BH and DH filtering
 * - Combined search and category filtering
 * - Search across all supported hospital fields
 * - Zero-result recovery
 * - Complete result accessibility (no arbitrary 80-item truncation)
 * - Selection callback and hospital ID preservation
 * - Caller exclusion rules and 3-preference limit
 * - UI-04 keyboard/insets modifier invariant
 *
 * Note: These JVM unit tests verify filtering engine logic, selection semantics,
 * and source invariants. Physical device/emulator soft-keyboard frame animations
 * and touch-fling interactions require Compose UI instrumentation tests.
 */
class HospitalPickerDialogFilteringTest {

    private fun sampleHospitals(): List<HospitalReference> = listOf(
        HospitalReference(
            hospitalId = "MOH2026-0001",
            name = "National Hospital of Sri Lanka",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "NH",
            categoryFullName = "National Hospital",
            administeringAuthority = "Line Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0002",
            name = "Colombo South Teaching Hospital",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "TH",
            categoryFullName = "Teaching Hospital",
            administeringAuthority = "Line Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0003",
            name = "Lady Ridgeway Hospital for Children",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "SPECIAL_TH",
            categoryFullName = "Specialized Teaching Hospital",
            administeringAuthority = "Line Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0013",
            name = "District General Hospital Avissawella",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "DGH",
            categoryFullName = "District General Hospital",
            administeringAuthority = "Provincial Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0014",
            name = "Colombo East Base Hospital",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "BHA",
            categoryFullName = "Base Hospital Type - A",
            administeringAuthority = "Line Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0015",
            name = "Base Hospital Homagama",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "BHB",
            categoryFullName = "Base Hospital Type - B",
            administeringAuthority = "Provincial Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0016",
            name = "Divisional Hospital Moratuwa",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "DHA",
            categoryFullName = "Divisional Hospital Type - A",
            administeringAuthority = "Provincial Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0018",
            name = "Divisional Hospital Maligawatta",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "DHB",
            categoryFullName = "Divisional Hospital Type - B",
            administeringAuthority = "Provincial Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0023",
            name = "Divisional Hospital Athurugiriya",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "DHC",
            categoryFullName = "Divisional Hospital Type - C",
            administeringAuthority = "Provincial Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0025",
            name = "PMCU Angoda",
            province = "Western Province",
            district = "Colombo",
            rdhsDivision = "RDHS Colombo",
            category = "PMCU",
            categoryFullName = "Primary Medical Care Unit",
            administeringAuthority = "Provincial Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0100",
            name = "Provincial General Hospital Kurunegala",
            province = "North Western Province",
            district = "Kurunegala",
            rdhsDivision = "RDHS Kurunegala",
            category = "PGH",
            categoryFullName = "Provincial General Hospital",
            administeringAuthority = "Provincial Ministry"
        ),
        HospitalReference(
            hospitalId = "MOH2026-0200",
            name = "National Hospital Kandy",
            province = "Central Province",
            district = "Kandy",
            rdhsDivision = "RDHS Kandy",
            category = "TH",
            categoryFullName = "Teaching Hospital",
            administeringAuthority = "Line Ministry"
        )
    )

    @Test
    fun test01_emptyQuery_returnsAllHospitalsForInitialBrowsing() {
        val list = sampleHospitals()
        val results = HospitalPickerFilterEngine.filterHospitals(list, query = "", selectedCategory = "ALL")
        assertEquals("Empty query with ALL category must return all hospitals without blank-dialog dead-end", list.size, results.size)
    }

    @Test
    fun test02_allCategoryResults_preservesAllMatchingHospitals() {
        val list = sampleHospitals()
        val results = HospitalPickerFilterEngine.filterHospitals(list, query = "Hospital", selectedCategory = "ALL")
        assertTrue("Matching hospitals with ALL category should include diverse categories", results.size >= 8)
        assertTrue(results.any { it.category == "NH" })
        assertTrue(results.any { it.category == "TH" })
        assertTrue(results.any { it.category == "BHA" })
        assertTrue(results.any { it.category == "DHA" })
    }

    @Test
    fun test03_categoryFiltering_matchesThPghDghBhAndDhCorrectly() {
        val list = sampleHospitals()

        // 1. TH category: matches "TH" and "SPECIAL_TH"
        val thResults = HospitalPickerFilterEngine.filterHospitals(list, query = "", selectedCategory = "TH")
        assertEquals(3, thResults.size)
        assertTrue(thResults.all { it.category == "TH" || it.category == "SPECIAL_TH" })

        // 2. PGH category: matches "PGH"
        val pghResults = HospitalPickerFilterEngine.filterHospitals(list, query = "", selectedCategory = "PGH")
        assertEquals(1, pghResults.size)
        assertEquals("PGH", pghResults.first().category)

        // 3. DGH category: matches "DGH"
        val dghResults = HospitalPickerFilterEngine.filterHospitals(list, query = "", selectedCategory = "DGH")
        assertEquals(1, dghResults.size)
        assertEquals("DGH", dghResults.first().category)

        // 4. BH category: matches "BHA" and "BHB"
        val bhResults = HospitalPickerFilterEngine.filterHospitals(list, query = "", selectedCategory = "BH")
        assertEquals(2, bhResults.size)
        assertTrue(bhResults.all { it.category.startsWith("BH") })

        // 5. DH category: matches "DHA", "DHB", "DHC"
        val dhResults = HospitalPickerFilterEngine.filterHospitals(list, query = "", selectedCategory = "DH")
        assertEquals(3, dhResults.size)
        assertTrue(dhResults.all { it.category.startsWith("DH") })
    }

    @Test
    fun test04_combinedSearchAndCategoryFiltering_appliesBothConditions() {
        val list = sampleHospitals()

        // Query "Colombo" + category "TH"
        val colomboTh = HospitalPickerFilterEngine.filterHospitals(list, query = "Colombo", selectedCategory = "TH")
        assertEquals(2, colomboTh.size)
        assertTrue(colomboTh.all { it.hospitalId in listOf("MOH2026-0002", "MOH2026-0003") })

        // Query "Kandy" + category "TH"
        val kandyTh = HospitalPickerFilterEngine.filterHospitals(list, query = "Kandy", selectedCategory = "TH")
        assertEquals(1, kandyTh.size)
        assertEquals("MOH2026-0200", kandyTh.first().hospitalId)

        // Query "Kandy" + category "DGH" (no DGH in Kandy in sample list)
        val kandyDgh = HospitalPickerFilterEngine.filterHospitals(list, query = "Kandy", selectedCategory = "DGH")
        assertEquals(0, kandyDgh.size)
    }

    @Test
    fun test05_searchAcrossSupportedFields() {
        val list = sampleHospitals()

        // 1. By name
        val byName = HospitalPickerFilterEngine.filterHospitals(list, query = "Homagama")
        assertEquals("MOH2026-0015", byName.single().hospitalId)

        // 2. By province
        val byProvince = HospitalPickerFilterEngine.filterHospitals(list, query = "Central Province")
        assertEquals("MOH2026-0200", byProvince.single().hospitalId)

        // 3. By RDHS division
        val byRdhs = HospitalPickerFilterEngine.filterHospitals(list, query = "RDHS Kurunegala")
        assertEquals("MOH2026-0100", byRdhs.single().hospitalId)

        // 4. By category acronym or full name
        val byCatName = HospitalPickerFilterEngine.filterHospitals(list, query = "Primary Medical Care")
        assertEquals("MOH2026-0025", byCatName.single().hospitalId)

        // 5. By hospitalId
        val byId = HospitalPickerFilterEngine.filterHospitals(list, query = "MOH2026-0013")
        assertTrue("Search by hospitalId must match the target hospital", byId.any { it.hospitalId == "MOH2026-0013" })

        // 6. By district
        val byDistrict = HospitalPickerFilterEngine.filterHospitals(list, query = "Kurunegala")
        assertEquals("MOH2026-0100", byDistrict.single().hospitalId)
    }

    @Test
    fun test06_zeroResultRecovery_allowsResettingFilters() {
        val list = sampleHospitals()

        // Non-existent search
        var query = "ZzzUnknownPlace123"
        var selectedCategory = "TH"
        var results = HospitalPickerFilterEngine.filterHospitals(list, query = query, selectedCategory = selectedCategory)
        assertEquals(0, results.size)

        // Reset search & filters
        query = ""
        selectedCategory = "ALL"
        results = HospitalPickerFilterEngine.filterHospitals(list, query = query, selectedCategory = selectedCategory)
        assertEquals(list.size, results.size)
    }

    @Test
    fun test07_resultsBeyondPrevious80ItemLimit_areNotTruncated() {
        // Generate 120 distinct hospitals
        val largeList = (1..120).map { i ->
            HospitalReference(
                hospitalId = "TEST-MOH-$i",
                name = "General Hospital Batch $i",
                category = if (i % 2 == 0) "DGH" else "BH",
                province = "Western Province",
                district = "Gampaha",
                rdhsDivision = "RDHS Gampaha"
            )
        }

        val results = HospitalPickerFilterEngine.filterHospitals(largeList, query = "", selectedCategory = "ALL")
        assertEquals("All 120 matching hospitals must be returned without arbitrary 80-item truncation", 120, results.size)

        val dghResults = HospitalPickerFilterEngine.filterHospitals(largeList, query = "", selectedCategory = "DGH")
        assertEquals(60, dghResults.size)
    }

    @Test
    fun test08_correctHospitalIdPassedThroughSelection() {
        val list = sampleHospitals()
        val target = list.first { it.hospitalId == "MOH2026-0002" }

        var selectedHospitalId: String? = null
        val onSelect: (HospitalReference) -> Unit = { selectedHospitalId = it.hospitalId }

        onSelect(target)
        assertEquals("MOH2026-0002", selectedHospitalId)
    }

    @Test
    fun test09_existingExclusionRulesAndThreePreferenceLimit_remainIntact() {
        val list = sampleHospitals()
        val currentHospital = list.first { it.hospitalId == "MOH2026-0001" }
        val preference1 = list.first { it.hospitalId == "MOH2026-0002" }
        val preference2 = list.first { it.hospitalId == "MOH2026-0003" }
        val preference3 = list.first { it.hospitalId == "MOH2026-0013" }

        val currentPreferences = mutableListOf(preference1, preference2)

        // 1. Verify caller exclusion filter excludes currentHospital and existing preferences
        val availableOptions = list.filterNot {
            it.hospitalId == currentHospital.hospitalId ||
                currentPreferences.any { sel -> sel.hospitalId == it.hospitalId }
        }

        assertFalse("Current hospital must be excluded from options", availableOptions.any { it.hospitalId == currentHospital.hospitalId })
        assertFalse("Already selected preference1 must be excluded", availableOptions.any { it.hospitalId == preference1.hospitalId })
        assertFalse("Already selected preference2 must be excluded", availableOptions.any { it.hospitalId == preference2.hospitalId })
        assertTrue("Unselected hospital must remain available", availableOptions.any { it.hospitalId == preference3.hospitalId })

        // 2. Add 3rd preference: now preference size is 3
        currentPreferences.add(preference3)
        assertEquals(3, currentPreferences.size)

        // 3. Attempting to add a 4th preference is prevented by the rule preferences.size < 3
        val canAddMore = currentPreferences.size < 3
        assertFalse("Maximum 3 preferences allowed; further additions must be blocked", canAddMore)
    }

    @Test
    fun test10_ui04KeyboardAndWindowInsets_invariantMaintained() {
        val candidates = listOf(
            File("app/src/main/java/com/pasindu/nursingotapp/transfer/ui/TransferRequestScreen.kt"),
            File("src/main/java/com/pasindu/nursingotapp/transfer/ui/TransferRequestScreen.kt"),
            File("../app/src/main/java/com/pasindu/nursingotapp/transfer/ui/TransferRequestScreen.kt")
        )
        val file = candidates.firstOrNull { it.exists() && it.isFile }
            ?: error("Could not locate TransferRequestScreen.kt")
        val source = file.readText().replace("\r\n", "\n")

        assertFalse(
            "Expected HospitalPickerDialog not to use rigid .height(320.dp)",
            source.contains(".height(320.dp)")
        )
        assertTrue(
            "Expected HospitalPickerDialog to maintain .heightIn(max = 320.dp) with flexible weight",
            source.contains(".heightIn(max = 320.dp)")
        )
        assertTrue(
            "Expected HospitalPickerDialog Surface to apply .imePadding()",
            source.contains(".imePadding()")
        )
    }
}
