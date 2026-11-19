package com.pasindu.nursingotapp.statgraph.data.repository

import com.pasindu.nursingotapp.statgraph.data.local.StatDatasetCodecException
import com.pasindu.nursingotapp.statgraph.data.local.StatDatasetDao
import com.pasindu.nursingotapp.statgraph.data.local.StatDatasetEntity
import com.pasindu.nursingotapp.statgraph.data.local.StatDatasetJsonCodec
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.orchestration.ChartMappingSelection
import com.pasindu.nursingotapp.statgraph.ui.state.WorkspaceSection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Lightweight summary metadata for a persisted [StatDatasetEntity].
 */
data class SavedStatDatasetSummary(
    val id: String,
    val name: String,
    val description: String,
    val sourceLabel: String,
    val activeSection: WorkspaceSection,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

/**
 * Complete hydrated workspace snapshot restored from local persistence.
 */
data class PersistedStatDataset(
    val dataset: StatDataset,
    val mappingSelection: ChartMappingSelection,
    val activeSection: WorkspaceSection,
    val schemaVersion: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

/**
 * Persistence contract for Stat Graph Studio saved datasets and workspace state.
 */
interface StatDatasetRepository {

    fun observeSavedDatasets(): Flow<List<SavedStatDatasetSummary>>

    suspend fun getMostRecentlyUpdated(): Result<PersistedStatDataset?>

    suspend fun getById(datasetId: String): Result<PersistedStatDataset?>

    suspend fun saveWorkspaceSnapshot(
        dataset: StatDataset,
        mappingSelection: ChartMappingSelection,
        activeSection: WorkspaceSection,
        updatedAtEpochMillis: Long = System.currentTimeMillis()
    ): Result<PersistedStatDataset>

    suspend fun deleteDataset(datasetId: String): Result<Boolean>
}

@Singleton
class RoomStatDatasetRepository @Inject constructor(
    private val dao: StatDatasetDao
) : StatDatasetRepository {

    override fun observeSavedDatasets(): Flow<List<SavedStatDatasetSummary>> =
        dao.observeAllOrderedByUpdatedDesc().map { entities ->
            entities.map { entity ->
                SavedStatDatasetSummary(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    sourceLabel = entity.sourceLabel,
                    activeSection = parseWorkspaceSectionOrDefault(entity.activeWorkspaceSection),
                    createdAtEpochMillis = entity.createdAtEpochMillis,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis
                )
            }
        }

    override suspend fun getMostRecentlyUpdated(): Result<PersistedStatDataset?> = safeRun {
        val entity = dao.getMostRecentlyUpdatedOnce() ?: return@safeRun null
        decodeEntity(entity)
    }

    override suspend fun getById(datasetId: String): Result<PersistedStatDataset?> = safeRun {
        if (datasetId.isBlank()) {
            throw IllegalArgumentException("Dataset ID must not be blank.")
        }
        val entity = dao.getById(datasetId) ?: return@safeRun null
        decodeEntity(entity)
    }

    override suspend fun saveWorkspaceSnapshot(
        dataset: StatDataset,
        mappingSelection: ChartMappingSelection,
        activeSection: WorkspaceSection,
        updatedAtEpochMillis: Long
    ): Result<PersistedStatDataset> = safeRun {
        require(dataset.id.isNotBlank()) { "StatDataset ID must not be blank." }
        val existing = dao.getById(dataset.id)
        val createdAt = existing?.createdAtEpochMillis
            ?: dataset.metadata.createdAtEpochMillis.takeIf { it > 0L }
            ?: updatedAtEpochMillis
        val stampedDataset = dataset.copy(
            metadata = dataset.metadata.copy(
                createdAtEpochMillis = createdAt,
                updatedAtEpochMillis = updatedAtEpochMillis
            )
        )
        val payloadJson = StatDatasetJsonCodec.encodeDataset(stampedDataset)
        val mappingJson = StatDatasetJsonCodec.encodeMappingSelection(mappingSelection)
        val entity = StatDatasetEntity(
            id = stampedDataset.id,
            name = stampedDataset.name.ifBlank { "Untitled Dataset" },
            description = stampedDataset.description,
            sourceLabel = stampedDataset.metadata.sourceLabel.ifBlank { "Manual Entry" },
            schemaVersion = StatDatasetJsonCodec.CURRENT_SCHEMA_VERSION,
            datasetPayloadJson = payloadJson,
            mappingSelectionJson = mappingJson,
            activeWorkspaceSection = activeSection.name,
            createdAtEpochMillis = createdAt,
            updatedAtEpochMillis = updatedAtEpochMillis
        )
        dao.upsert(entity)
        PersistedStatDataset(
            dataset = stampedDataset,
            mappingSelection = mappingSelection,
            activeSection = activeSection,
            schemaVersion = entity.schemaVersion,
            createdAtEpochMillis = createdAt,
            updatedAtEpochMillis = updatedAtEpochMillis
        )
    }

    override suspend fun deleteDataset(datasetId: String): Result<Boolean> = safeRun {
        require(datasetId.isNotBlank()) { "Dataset ID must not be blank." }
        dao.deleteById(datasetId) > 0
    }

    private fun decodeEntity(entity: StatDatasetEntity): PersistedStatDataset {
        val dataset = StatDatasetJsonCodec.decodeDataset(
            payloadJson = entity.datasetPayloadJson,
            expectedSchemaVersion = entity.schemaVersion
        ).let { decoded ->
            decoded.copy(
                metadata = decoded.metadata.copy(
                    createdAtEpochMillis = entity.createdAtEpochMillis,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis
                )
            )
        }
        val mapping = StatDatasetJsonCodec.decodeMappingSelection(
            mappingJson = entity.mappingSelectionJson,
            expectedSchemaVersion = entity.schemaVersion
        )
        val section = parseWorkspaceSectionStrict(entity.activeWorkspaceSection)
        return PersistedStatDataset(
            dataset = dataset,
            mappingSelection = mapping,
            activeSection = section,
            schemaVersion = entity.schemaVersion,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis
        )
    }

    private fun parseWorkspaceSectionStrict(raw: String): WorkspaceSection =
        WorkspaceSection.entries.firstOrNull { it.name == raw }
            ?: throw StatDatasetCodecException("Unknown workspace section '$raw'.")

    private fun parseWorkspaceSectionOrDefault(raw: String): WorkspaceSection =
        WorkspaceSection.entries.firstOrNull { it.name == raw } ?: WorkspaceSection.EXPLORE

    private inline fun <T> safeRun(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        Result.failure(t)
    }
}
