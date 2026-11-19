package com.pasindu.nursingotapp.statgraph.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasindu.nursingotapp.statgraph.data.CsvColumnOverride
import com.pasindu.nursingotapp.statgraph.data.CsvImportEngine
import com.pasindu.nursingotapp.statgraph.data.StatGraphSampleData
import com.pasindu.nursingotapp.statgraph.data.repository.PersistedStatDataset
import com.pasindu.nursingotapp.statgraph.data.repository.SavedStatDatasetSummary
import com.pasindu.nursingotapp.statgraph.data.repository.StatDatasetRepository
import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.orchestration.ChartMappingSelection
import com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryController
import com.pasindu.nursingotapp.statgraph.ui.StatGraphEntryTab
import com.pasindu.nursingotapp.statgraph.ui.state.StatGraphWorkspaceController
import com.pasindu.nursingotapp.statgraph.ui.state.StatGraphWorkspaceSamples
import com.pasindu.nursingotapp.statgraph.ui.state.StatGraphWorkspaceUiState
import com.pasindu.nursingotapp.statgraph.ui.state.WorkspaceSamplePreset
import com.pasindu.nursingotapp.statgraph.ui.state.WorkspaceSection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface StatGraphSaveStatus {
    data object Initializing : StatGraphSaveStatus
    data object DemoPreview : StatGraphSaveStatus
    data object PendingChanges : StatGraphSaveStatus
    data object Saving : StatGraphSaveStatus
    data class Saved(val updatedAtEpochMillis: Long) : StatGraphSaveStatus
    data class Error(val message: String) : StatGraphSaveStatus
}

data class StatGraphStudioUiHolder(
    val workspace: StatGraphWorkspaceUiState = StatGraphWorkspaceController.createInitial(
        dataset = StatGraphWorkspaceSamples.dengueFourMonthDemoDataset()
    ),
    val savedDatasets: List<SavedStatDatasetSummary> = emptyList(),
    val isInitializing: Boolean = true,
    val isDemoMode: Boolean = true,
    val activePersistedDatasetId: String? = null,
    val saveStatus: StatGraphSaveStatus = StatGraphSaveStatus.Initializing,
    val pendingDeleteDatasetId: String? = null,
    val errorMessage: String? = null
) {
    val activeDatasetName: String
        get() = workspace.dataset.name.ifBlank { "Untitled Dataset" }
}

/**
 * Hilt ViewModel coordinating local Room persistence, saved dataset restoration,
 * debounced auto-saves, CSV imports, and multi-dataset switching for Stat Graph Studio.
 */
@HiltViewModel
class StatGraphStudioViewModel(
    private val repository: StatDatasetRepository,
    private val saveDebounceMs: Long = DEFAULT_SAVE_DEBOUNCE_MS,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {

    @Inject
    constructor(repository: StatDatasetRepository) : this(
        repository = repository,
        saveDebounceMs = DEFAULT_SAVE_DEBOUNCE_MS,
        clock = { System.currentTimeMillis() },
        idGenerator = { UUID.randomUUID().toString() }
    )

    private data class SaveSnapshot(
        val generation: Long,
        val dataset: StatDataset,
        val mappingSelection: ChartMappingSelection,
        val activeSection: WorkspaceSection,
        val timestampEpochMillis: Long
    )

    private val saveMutex = Mutex()
    private val deletedDatasetIds = mutableSetOf<String>()
    private var nextGeneration: Long = 0L
    private var latestRequestedGenerationById = mutableMapOf<String, Long>()
    private var latestPersistedGenerationById = mutableMapOf<String, Long>()
    private var pendingSnapshot: SaveSnapshot? = null
    private var pendingSaveJob: Job? = null

    private val _uiState = MutableStateFlow(StatGraphStudioUiHolder())
    val uiState: StateFlow<StatGraphStudioUiHolder> = _uiState.asStateFlow()

    init {
        observeSavedDatasetSummaries()
        restoreLatestWorkspace()
    }

    private fun observeSavedDatasetSummaries() {
        viewModelScope.launch {
            repository.observeSavedDatasets()
                .catch { error ->
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = error.message ?: "Unable to observe saved datasets."
                        )
                    }
                }
                .collect { summaries ->
                    _uiState.update { current ->
                        current.copy(savedDatasets = summaries)
                    }
                }
        }
    }

    private fun restoreLatestWorkspace() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInitializing = true,
                    saveStatus = StatGraphSaveStatus.Initializing
                )
            }
            val result = repository.getMostRecentlyUpdated()
            result.fold(
                onSuccess = { persisted ->
                    if (persisted != null) {
                        applyPersistedDataset(persisted)
                    } else {
                        applyDemoShowcase(WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO)
                    }
                },
                onFailure = { error ->
                    val fallbackDemo = StatGraphWorkspaceController.fromPreset(
                        WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO
                    )
                    _uiState.update { current ->
                        current.copy(
                            workspace = fallbackDemo,
                            isInitializing = false,
                            isDemoMode = true,
                            activePersistedDatasetId = null,
                            saveStatus = StatGraphSaveStatus.Error(
                                error.message ?: "Failed to restore saved dataset."
                            ),
                            errorMessage = "Could not restore saved dataset (${error.message ?: "corrupted data"}). Showing Dengue Demo instead."
                        )
                    }
                }
            )
        }
    }

    fun onWorkspaceStateChange(updatedWorkspace: StatGraphWorkspaceUiState) {
        val currentHolder = _uiState.value
        val previousWorkspace = currentHolder.workspace

        if (updatedWorkspace == previousWorkspace) {
            return
        }

        // Handle preset switching from the TopAppBar Samples menu cleanly.
        val switchedPreset = updatedWorkspace.activePreset
        val switchingToAnotherDataset =
            updatedWorkspace.dataset.id != previousWorkspace.dataset.id
        if (switchingToAnotherDataset && switchedPreset != null) {
            if (switchedPreset == WorkspaceSamplePreset.BLANK) {
                createBlankDataset()
            } else {
                loadDemoPreset(switchedPreset)
            }
            return
        }

        val datasetChanged = updatedWorkspace.dataset != previousWorkspace.dataset
        val mappingChanged = updatedWorkspace.mappingSelection != previousWorkspace.mappingSelection
        val sectionChanged = updatedWorkspace.activeSection != previousWorkspace.activeSection

        if (currentHolder.isDemoMode) {
            if (!datasetChanged) {
                // Navigating sections, tabs, or exploring mappings on a demo never saves silently.
                _uiState.update {
                    it.copy(workspace = updatedWorkspace, errorMessage = null)
                }
                return
            }

            // Editing or importing into the demo promotes it to a personal copy with a new UUID.
            val now = clock()
            val promotedSource = if (updatedWorkspace.dataset.metadata.sourceLabel == "Simulated Demo Dataset") {
                if (updatedWorkspace.entryState.csvPreview != null) "CSV Import" else "Manual Entry"
            } else {
                updatedWorkspace.dataset.metadata.sourceLabel
            }
            val promotedName = if (
                updatedWorkspace.dataset.metadata.sourceLabel.startsWith("CSV") &&
                updatedWorkspace.dataset.name == previousWorkspace.dataset.name
            ) {
                previousWorkspace.entryState.csvFileName
                    ?.substringBeforeLast('.')
                    ?.replace('_', ' ')
                    ?.takeIf { it.isNotBlank() }
                    ?: "Imported CSV Dataset"
            } else {
                updatedWorkspace.dataset.name
            }
            val promotedDataset = updatedWorkspace.dataset.copy(
                id = idGenerator(),
                name = promotedName,
                metadata = DatasetMetadata(
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    sourceLabel = promotedSource
                )
            )
            val promotedWorkspace = StatGraphWorkspaceController.createInitial(
                dataset = promotedDataset,
                activeSection = updatedWorkspace.activeSection,
                mappingSelection = updatedWorkspace.mappingSelection,
                activePreset = null,
                showAllCatalogueCharts = updatedWorkspace.showAllCatalogueCharts
            ).let { rebuilt ->
                rebuilt.copy(
                    entryState = rebuilt.entryState.copy(
                        activeTab = updatedWorkspace.entryState.activeTab,
                        csvRawText = updatedWorkspace.entryState.csvRawText,
                        csvHasHeaderOverride = updatedWorkspace.entryState.csvHasHeaderOverride,
                        csvColumnOverrides = updatedWorkspace.entryState.csvColumnOverrides,
                        csvPreview = updatedWorkspace.entryState.csvPreview,
                        csvFileName = updatedWorkspace.entryState.csvFileName
                    ),
                    isResearchMode = updatedWorkspace.isResearchMode,
                    showGuidedStart = updatedWorkspace.showGuidedStart,
                )
            }

            _uiState.update {
                it.copy(
                    workspace = promotedWorkspace,
                    isDemoMode = false,
                    activePersistedDatasetId = promotedDataset.id,
                    saveStatus = StatGraphSaveStatus.PendingChanges,
                    errorMessage = null
                )
            }
            scheduleDebouncedSave(promotedWorkspace, now)
            return
        }

        // Personal persisted dataset edit: update UI immediately and debounce Room write.
        val now = clock()
        val finalWorkspace = if (datasetChanged) {
            val stamped = updatedWorkspace.dataset.copy(
                metadata = updatedWorkspace.dataset.metadata.copy(updatedAtEpochMillis = now)
            )
            updatedWorkspace.copy(entryState = updatedWorkspace.entryState.copy(dataset = stamped))
        } else {
            updatedWorkspace
        }

        val shouldPersist = datasetChanged || mappingChanged || sectionChanged
        _uiState.update { current ->
            current.copy(
                workspace = finalWorkspace,
                activePersistedDatasetId = finalWorkspace.dataset.id,
                saveStatus = if (shouldPersist) StatGraphSaveStatus.PendingChanges else current.saveStatus,
                errorMessage = null
            )
        }
        if (shouldPersist) {
            scheduleDebouncedSave(finalWorkspace, now)
        }
    }

    fun loadDemoPreset(preset: WorkspaceSamplePreset) {
        if (preset == WorkspaceSamplePreset.BLANK) {
            createBlankDataset()
            return
        }
        viewModelScope.launch {
            flushPendingSaveInternal()
            applyDemoShowcase(preset)
        }
    }

    fun loadStarterTemplate(template: com.pasindu.nursingotapp.statgraph.data.StatGraphStarterTemplate) {
        if (template.id == com.pasindu.nursingotapp.statgraph.data.StatGraphStarterTemplateId.BLANK) {
            createBlankDataset()
            return
        }
        val currentResearchMode = _uiState.value.workspace.isResearchMode
        val workspace = StatGraphWorkspaceController.fromStarterTemplate(
            template = template,
            isResearchMode = currentResearchMode,
        )
        _uiState.update { current ->
            current.copy(
                workspace = workspace,
                isInitializing = false,
                isDemoMode = true,
                activePersistedDatasetId = null,
                saveStatus = StatGraphSaveStatus.DemoPreview,
                errorMessage = null
            )
        }
    }

    fun createBlankDataset(
        name: String = "Untitled Dataset",
        description: String = ""
    ) {
        val now = clock()
        val blank = StatGraphSampleData.emptyDataset().copy(
            id = idGenerator(),
            name = name,
            description = description,
            metadata = DatasetMetadata(
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                sourceLabel = "Manual Entry"
            )
        )
        createAndPersistNewDataset(
            dataset = blank,
            activeSection = WorkspaceSection.DATA,
            initialTab = StatGraphEntryTab.SETUP
        )
    }

    fun saveDemoCopyToLibrary() {
        val current = _uiState.value
        val now = clock()
        val copyName = current.workspace.dataset.name
            .ifBlank { "Stat Graph Dataset" }
            .let { base -> if (base.endsWith("(Copy)")) base else "$base (Copy)" }
        val copiedDataset = current.workspace.dataset.copy(
            id = idGenerator(),
            name = copyName,
            metadata = DatasetMetadata(
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                sourceLabel = if (current.isDemoMode) "Saved Demo Copy" else current.workspace.dataset.metadata.sourceLabel
            )
        )
        createAndPersistNewDataset(
            dataset = copiedDataset,
            activeSection = current.workspace.activeSection,
            mappingSelection = current.workspace.mappingSelection,
            initialTab = current.workspace.entryState.activeTab
        )
    }

    fun importCsvIntoNewDataset(
        rawCsv: String,
        datasetName: String = "Imported CSV Dataset",
        sourceLabel: String = "CSV Import",
        hasHeaderOverride: Boolean? = null,
        columnOverrides: Map<Int, CsvColumnOverride> = emptyMap()
    ) {
        val now = clock()
        val imported = CsvImportEngine.buildPreview(
            rawText = rawCsv,
            datasetName = datasetName,
            hasHeaderOverride = hasHeaderOverride,
            columnOverrides = columnOverrides
        ).candidateDataset.let { ds ->
            ds.copy(
                id = idGenerator(),
                metadata = ds.metadata.copy(
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    sourceLabel = sourceLabel
                )
            )
        }
        val targetSection = if (imported.rows.isNotEmpty() && imported.fields.isNotEmpty()) {
            WorkspaceSection.EXPLORE
        } else {
            WorkspaceSection.DATA
        }
        createAndPersistNewDataset(
            dataset = imported,
            activeSection = targetSection,
            initialTab = StatGraphEntryTab.ROWS
        )
    }

    fun selectSavedDataset(datasetId: String) {
        if (datasetId == _uiState.value.activePersistedDatasetId && !_uiState.value.isDemoMode) {
            return
        }
        viewModelScope.launch {
            flushPendingSaveInternal()
            _uiState.update {
                it.copy(
                    isInitializing = true,
                    saveStatus = StatGraphSaveStatus.Initializing,
                    errorMessage = null
                )
            }
            repository.getById(datasetId).fold(
                onSuccess = { persisted ->
                    if (persisted != null) {
                        applyPersistedDataset(persisted)
                    } else {
                        _uiState.update {
                            it.copy(
                                isInitializing = false,
                                saveStatus = StatGraphSaveStatus.Error("Dataset not found."),
                                errorMessage = "Saved dataset could not be found."
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isInitializing = false,
                            saveStatus = StatGraphSaveStatus.Error(
                                error.message ?: "Unable to load dataset."
                            ),
                            errorMessage = error.message ?: "Unable to load dataset."
                        )
                    }
                }
            )
        }
    }

    fun requestDeleteDataset(datasetId: String) {
        _uiState.update { it.copy(pendingDeleteDatasetId = datasetId) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(pendingDeleteDatasetId = null) }
    }

    fun confirmDeleteDataset() {
        val targetId = _uiState.value.pendingDeleteDatasetId ?: return
        _uiState.update { it.copy(pendingDeleteDatasetId = null) }
        deleteDataset(targetId)
    }

    fun deleteDataset(datasetId: String) {
        deletedDatasetIds.add(datasetId)
        if (pendingSnapshot?.dataset?.id == datasetId) {
            pendingSaveJob?.cancel()
            pendingSaveJob = null
            pendingSnapshot = null
        }

        viewModelScope.launch {
            val wasActive = _uiState.value.activePersistedDatasetId == datasetId && !_uiState.value.isDemoMode
            val result = saveMutex.withLock {
                repository.deleteDataset(datasetId)
            }
            result.fold(
                onSuccess = {
                    if (wasActive) {
                        val nextResult = repository.getMostRecentlyUpdated()
                        nextResult.fold(
                            onSuccess = { nextPersisted ->
                                if (nextPersisted != null) {
                                    applyPersistedDataset(nextPersisted)
                                } else {
                                    applyDemoShowcase(WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO)
                                }
                            },
                            onFailure = { error ->
                                applyDemoShowcase(WorkspaceSamplePreset.DENGUE_FOUR_MONTH_DEMO)
                                _uiState.update {
                                    it.copy(errorMessage = error.message ?: "Unable to load next dataset.")
                                }
                            }
                        )
                    }
                },
                onFailure = { error ->
                    deletedDatasetIds.remove(datasetId)
                    _uiState.update {
                        it.copy(
                            saveStatus = StatGraphSaveStatus.Error(error.message ?: "Delete failed."),
                            errorMessage = error.message ?: "Unable to delete dataset."
                        )
                    }
                }
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    suspend fun flushPendingSaveNow() {
        flushPendingSaveInternal()
    }

    private fun createAndPersistNewDataset(
        dataset: StatDataset,
        activeSection: WorkspaceSection,
        mappingSelection: ChartMappingSelection = ChartMappingSelection(),
        initialTab: StatGraphEntryTab = StatGraphEntryTab.ROWS
    ) {
        viewModelScope.launch {
            flushPendingSaveInternal()
            val newWorkspace = StatGraphWorkspaceController.createInitial(
                dataset = dataset,
                activeSection = activeSection,
                mappingSelection = mappingSelection,
                activePreset = null
            ).let { built ->
                built.copy(entryState = built.entryState.copy(activeTab = initialTab))
            }
            _uiState.update { current ->
                current.copy(
                    workspace = newWorkspace,
                    isInitializing = false,
                    isDemoMode = false,
                    activePersistedDatasetId = dataset.id,
                    saveStatus = StatGraphSaveStatus.Saving,
                    errorMessage = null
                )
            }
            val generation = ++nextGeneration
            latestRequestedGenerationById[dataset.id] = generation
            val snapshot = SaveSnapshot(
                generation = generation,
                dataset = dataset,
                mappingSelection = mappingSelection,
                activeSection = activeSection,
                timestampEpochMillis = dataset.metadata.updatedAtEpochMillis
            )
            persistSnapshot(snapshot)
        }
    }

    private fun scheduleDebouncedSave(
        workspace: StatGraphWorkspaceUiState,
        timestampEpochMillis: Long
    ) {
        val generation = ++nextGeneration
        val snapshot = SaveSnapshot(
            generation = generation,
            dataset = workspace.dataset,
            mappingSelection = workspace.mappingSelection,
            activeSection = workspace.activeSection,
            timestampEpochMillis = timestampEpochMillis
        )
        latestRequestedGenerationById[snapshot.dataset.id] = generation
        pendingSnapshot = snapshot
        pendingSaveJob?.cancel()
        pendingSaveJob = viewModelScope.launch {
            if (saveDebounceMs > 0L) {
                delay(saveDebounceMs)
            }
            if (pendingSnapshot?.generation == generation) {
                pendingSnapshot = null
            }
            persistSnapshot(snapshot)
        }
    }

    private suspend fun flushPendingSaveInternal() {
        val snapshotToFlush = pendingSnapshot ?: return
        pendingSnapshot = null
        pendingSaveJob?.cancel()
        pendingSaveJob = null
        persistSnapshot(snapshotToFlush)
    }

    private suspend fun persistSnapshot(snapshot: SaveSnapshot) {
        val datasetId = snapshot.dataset.id
        if (datasetId in deletedDatasetIds) return

        val saveResult = saveMutex.withLock {
            if (datasetId in deletedDatasetIds) return@withLock null
            val alreadyPersistedGen = latestPersistedGenerationById[datasetId] ?: 0L
            if (snapshot.generation < alreadyPersistedGen) {
                // Newer mutation already persisted; do not overwrite with stale snapshot.
                return@withLock null
            }
            val newestRequested = latestRequestedGenerationById[datasetId] ?: snapshot.generation
            if (snapshot.generation < newestRequested && pendingSnapshot?.dataset?.id == datasetId) {
                // Another pending mutation for this dataset supersedes this one.
                return@withLock null
            }
            _uiState.update { current ->
                if (!current.isDemoMode && current.activePersistedDatasetId == datasetId) {
                    current.copy(saveStatus = StatGraphSaveStatus.Saving)
                } else {
                    current
                }
            }
            repository.saveWorkspaceSnapshot(
                dataset = snapshot.dataset,
                mappingSelection = snapshot.mappingSelection,
                activeSection = snapshot.activeSection,
                updatedAtEpochMillis = snapshot.timestampEpochMillis
            )
        } ?: return

        saveResult.fold(
            onSuccess = { persisted ->
                latestPersistedGenerationById[datasetId] = snapshot.generation
                _uiState.update { current ->
                    val currentGen = latestRequestedGenerationById[datasetId] ?: 0L
                    if (!current.isDemoMode &&
                        current.activePersistedDatasetId == datasetId &&
                        currentGen <= snapshot.generation
                    ) {
                        current.copy(
                            saveStatus = StatGraphSaveStatus.Saved(persisted.updatedAtEpochMillis),
                            errorMessage = null
                        )
                    } else {
                        current
                    }
                }
            },
            onFailure = { error ->
                _uiState.update { current ->
                    current.copy(
                        saveStatus = StatGraphSaveStatus.Error(error.message ?: "Save failed."),
                        errorMessage = error.message ?: "Unable to save dataset changes."
                    )
                }
            }
        )
    }

    private fun applyPersistedDataset(persisted: PersistedStatDataset) {
        val restoredWorkspace = StatGraphWorkspaceController.createInitial(
            dataset = persisted.dataset,
            activeSection = persisted.activeSection,
            mappingSelection = persisted.mappingSelection,
            activePreset = null
        )
        _uiState.update { current ->
            current.copy(
                workspace = restoredWorkspace,
                isInitializing = false,
                isDemoMode = false,
                activePersistedDatasetId = persisted.dataset.id,
                saveStatus = StatGraphSaveStatus.Saved(persisted.updatedAtEpochMillis),
                errorMessage = null
            )
        }
    }

    private fun applyDemoShowcase(preset: WorkspaceSamplePreset) {
        val demoWorkspace = StatGraphWorkspaceController.fromPreset(preset)
        _uiState.update { current ->
            current.copy(
                workspace = demoWorkspace,
                isInitializing = false,
                isDemoMode = true,
                activePersistedDatasetId = null,
                saveStatus = StatGraphSaveStatus.DemoPreview,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        val leftover = pendingSnapshot
        pendingSnapshot = null
        pendingSaveJob?.cancel()
        pendingSaveJob = null
        if (leftover != null && leftover.dataset.id !in deletedDatasetIds) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    saveMutex.withLock {
                        val persistedGen = latestPersistedGenerationById[leftover.dataset.id] ?: 0L
                        if (leftover.generation >= persistedGen &&
                            leftover.dataset.id !in deletedDatasetIds
                        ) {
                            repository.saveWorkspaceSnapshot(
                                dataset = leftover.dataset,
                                mappingSelection = leftover.mappingSelection,
                                activeSection = leftover.activeSection,
                                updatedAtEpochMillis = leftover.timestampEpochMillis
                            )
                        }
                    }
                } catch (_: CancellationException) {
                    // Best-effort flush
                } catch (_: Throwable) {
                    // ViewModel already cleared; ignore background failure
                }
            }
        }
        super.onCleared()
    }

    companion object {
        const val DEFAULT_SAVE_DEBOUNCE_MS: Long = 400L
    }
}
