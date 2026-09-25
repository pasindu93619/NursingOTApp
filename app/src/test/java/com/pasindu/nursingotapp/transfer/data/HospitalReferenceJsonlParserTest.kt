package com.pasindu.nursingotapp.transfer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HospitalReferenceJsonlParserTest {

    @Test
    fun parsesCanonicalRecord() {
        val hospital = HospitalReferenceJsonlParser.parseLine(
            """{"hospitalId":"MOH2026-0001","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"NH","categoryFullName":"National Hospital","name":"NHSL","administeringAuthority":"Line Ministry","remarks":null,"sourceYear":2026,"sourceReference":"Sri Lanka Hospitals List 2026 - All Hospitals","datasetVersion":"MOH-2026-1206"}"""
        )

        requireNotNull(hospital)
        assertEquals("MOH2026-0001", hospital.hospitalId)
        assertEquals("NHSL", hospital.name)
        assertEquals("Line Ministry", hospital.administeringAuthority)
        assertEquals(2026, hospital.sourceYear)
        assertEquals("MOH-2026-1206", hospital.datasetVersion)
    }

    @Test
    fun acceptsSourceColumnNamesForCompatibility() {
        val hospital = HospitalReferenceJsonlParser.parseLine(
            """{"hospitalId":"MOH2026-0002","Province":"Western Province","RDHS Division":"RDHS Colombo","category":"TH","categoryFullName":"Teaching Hospital","name":"Colombo South TH","Authority":"Line Ministry","Remarks":"note"}"""
        )

        requireNotNull(hospital)
        assertEquals("Western Province", hospital.province)
        assertEquals("RDHS Colombo", hospital.rdhsDivision)
        assertEquals("Line Ministry", hospital.administeringAuthority)
        assertEquals("note", hospital.remarks)
    }

    @Test
    fun parsesRecordWithUtf8Bom() {
        val hospital = HospitalReferenceJsonlParser.parseLine(
            """\uFEFF{"hospitalId":"MOH2026-0003","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"TH","categoryFullName":"Teaching Hospital","name":"Apeksha Hospital","administeringAuthority":"Line Ministry","sourceYear":2026,"datasetVersion":"MOH-2026-1206"}"""
        )

        requireNotNull(hospital)
        assertEquals("Apeksha Hospital", hospital.name)
    }

    @Test
    fun ignoresBlankLines() {
        assertNull(HospitalReferenceJsonlParser.parseLine("   "))
    }
}
