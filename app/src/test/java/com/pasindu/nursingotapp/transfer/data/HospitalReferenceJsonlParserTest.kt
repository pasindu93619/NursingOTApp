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
            "\uFEFF" + """{"hospitalId":"MOH2026-0003","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"TH","categoryFullName":"Teaching Hospital","name":"Apeksha Hospital","administeringAuthority":"Line Ministry","sourceYear":2026,"datasetVersion":"MOH-2026-1206"}"""
        )

        requireNotNull(hospital)
        assertEquals("Apeksha Hospital", hospital.name)
    }

    @Test
    fun ignoresBlankLines() {
        assertNull(HospitalReferenceJsonlParser.parseLine("   "))
    }

    @Test
    fun parsesDistrictLatitudeAndLongitude() {
        val hospital = HospitalReferenceJsonlParser.parseLine(
            """{"hospitalId":"MOH2026-0001","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"NH","categoryFullName":"National Hospital","name":"NHSL","administeringAuthority":"Line Ministry","district":"Colombo","latitude":6.9183276,"longitude":79.8683654}"""
        )

        requireNotNull(hospital)
        assertEquals("Colombo", hospital.district)
        assertEquals(6.9183276, hospital.latitude)
        assertEquals(79.8683654, hospital.longitude)
    }

    @Test
    fun acceptsSourceCapitalizedGeographicFieldNames() {
        val hospital = HospitalReferenceJsonlParser.parseLine(
            """{"hospitalId":"MOH2026-0001","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"NH","categoryFullName":"National Hospital","name":"NHSL","administeringAuthority":"Line Ministry","District":"Colombo","Latitude":"6.9183276","Longitude":"79.8683654"}"""
        )

        requireNotNull(hospital)
        assertEquals("Colombo", hospital.district)
        assertEquals(6.9183276, hospital.latitude)
        assertEquals(79.8683654, hospital.longitude)
    }

    @Test
    fun preservesNullForMissingGeographicFields() {
        val hospital = HospitalReferenceJsonlParser.parseLine(
            """{"hospitalId":"MOH2026-0020","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"DHB","categoryFullName":"Divisional Hospital Type B","name":"DHB Padukka","administeringAuthority":"Provincial Ministry","district":null,"latitude":null,"longitude":null}"""
        )

        requireNotNull(hospital)
        assertNull(hospital.district)
        assertNull(hospital.latitude)
        assertNull(hospital.longitude)
    }

    @Test
    fun handlesInvalidCoordinatesAsNull() {
        val nonNumeric = HospitalReferenceJsonlParser.parseLine(
            """{"hospitalId":"MOH2026-0020","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"DHB","categoryFullName":"Divisional Hospital Type B","name":"DHB Padukka","administeringAuthority":"Provincial Ministry","latitude":"invalid","longitude":"unknown"}"""
        )
        requireNotNull(nonNumeric)
        assertNull(nonNumeric.latitude)
        assertNull(nonNumeric.longitude)

        val zeroCoordinate = HospitalReferenceJsonlParser.parseLine(
            """{"hospitalId":"MOH2026-0020","province":"Western Province","rdhsDivision":"RDHS Colombo","category":"DHB","categoryFullName":"Divisional Hospital Type B","name":"DHB Padukka","administeringAuthority":"Provincial Ministry","latitude":0.0,"longitude":0.0}"""
        )
        requireNotNull(zeroCoordinate)
        assertNull(zeroCoordinate.latitude)
        assertNull(zeroCoordinate.longitude)
    }
}
