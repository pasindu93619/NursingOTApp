package com.pasindu.nursingotapp.transfer.data

import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object HospitalReferenceJsonlParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parseLine(line: String): HospitalReference? {
        // Some bundled JSONL generators emit a UTF-8 BOM on the first line.
        // Strip it before JSON parsing so one BOM cannot invalidate the whole dataset.
        val trimmed = line.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return null

        val obj = json.parseToJsonElement(trimmed).jsonObject

        return HospitalReference(
            hospitalId = required(obj, "hospitalId"),
            province = requiredAny(obj, "province", "Province"),
            rdhsDivision = requiredAny(obj, "rdhsDivision", "RDHS Division"),
            category = required(obj, "category"),
            categoryFullName = required(obj, "categoryFullName"),
            name = required(obj, "name"),
            administeringAuthority = requiredAny(obj, "administeringAuthority", "Authority"),
            remarks = nullableValue(obj, "remarks", "Remarks"),
            sourceYear = value(obj, "sourceYear")?.toIntOrNull() ?: 2026,
            sourceReference = value(obj, "sourceReference")
                ?: "Sri Lanka Hospitals List 2026 - All Hospitals",
            datasetVersion = value(obj, "datasetVersion") ?: "MOH-2026-1206"
        )
    }

    fun parseLines(lines: Sequence<String>): List<HospitalReference> =
        lines.mapNotNull(::parseLine).toList()

    private fun required(obj: JsonObject, key: String): String =
        value(obj, key)?.takeIf { it.isNotBlank() }
            ?: error("Hospital reference record is missing required field '$key'")

    private fun requiredAny(obj: JsonObject, vararg keys: String): String =
        value(obj, *keys)?.takeIf { it.isNotBlank() }
            ?: error("Hospital reference record is missing required field: " + keys.joinToString(" / "))

    private fun value(obj: JsonObject, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            obj[key]?.let { element ->
                if (element is JsonNull) null else element.jsonPrimitive.contentOrNull
            }
        }?.trim()

    private fun nullableValue(obj: JsonObject, vararg keys: String): String? =
        value(obj, *keys)?.takeIf { it.isNotBlank() }
}