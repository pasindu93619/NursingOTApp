package com.pasindu.nursingotapp.transfer.data

import android.content.Context
import com.pasindu.nursingotapp.transfer.data.model.HospitalReference
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HospitalReferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val loadMutex = Mutex()
    @Volatile
    private var cachedHospitals: List<HospitalReference>? = null

    suspend fun getAll(): List<HospitalReference> {
        cachedHospitals?.let { return it }

        return loadMutex.withLock {
            cachedHospitals?.let { return@withLock it }

            val hospitals = ASSET_PARTS
                .asSequence()
                .flatMap { readAssetPart(it).asSequence() }
                .toList()

            require(hospitals.size == EXPECTED_HOSPITAL_COUNT) {
                "Hospital reference dataset integrity check failed: expected " +
                    "$EXPECTED_HOSPITAL_COUNT records, found \${hospitals.size}"
            }

            require(hospitals.map { it.hospitalId }.distinct().size == hospitals.size) {
                "Hospital reference dataset integrity check failed: duplicate hospitalId detected"
            }

            require(hospitals.all { it.datasetVersion == DATASET_VERSION }) {
                "Hospital reference dataset integrity check failed: unexpected dataset version"
            }

            hospitals.also { cachedHospitals = it }
        }
    }

    suspend fun search(
        query: String = "",
        province: String? = null,
        authority: String? = null
    ): List<HospitalReference> {
        val normalizedQuery = query.trim().lowercase()
        val normalizedProvince = province?.trim()?.lowercase()
        val normalizedAuthority = authority?.trim()?.lowercase()

        return getAll().asSequence()
            .filter { hospital ->
                normalizedQuery.isBlank() ||
                    hospital.name.lowercase().contains(normalizedQuery) ||
                    hospital.hospitalId.lowercase().contains(normalizedQuery) ||
                    hospital.category.lowercase().contains(normalizedQuery) ||
                    hospital.categoryFullName.lowercase().contains(normalizedQuery) ||
                    hospital.rdhsDivision.lowercase().contains(normalizedQuery)
            }
            .filter { hospital ->
                normalizedProvince.isNullOrBlank() ||
                    hospital.province.lowercase() == normalizedProvince
            }
            .filter { hospital ->
                normalizedAuthority.isNullOrBlank() ||
                    hospital.administeringAuthority.lowercase() == normalizedAuthority
            }
            .toList()
    }

    fun clearMemoryCache() {
        cachedHospitals = null
    }

    private fun readAssetPart(assetPath: String): List<HospitalReference> =
        context.assets.open(assetPath).use { input ->
            GZIPInputStream(input).use { gzip ->
                BufferedReader(InputStreamReader(gzip, Charsets.UTF_8)).use { reader ->
                    HospitalReferenceJsonlParser.parseLines(reader.lineSequence())
                }
            }
        }

    private companion object {
        const val EXPECTED_HOSPITAL_COUNT = 1206
        const val DATASET_VERSION = "MOH-2026-1206"

        val ASSET_PARTS = (1..7).map { index ->
            "hospitals_2026/part%02d.jsonl.gz.data".format(index)
        }
    }
}
