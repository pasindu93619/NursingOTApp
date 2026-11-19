package com.pasindu.nursingotapp.statgraph.data.local

import com.pasindu.nursingotapp.statgraph.model.DatasetMetadata
import com.pasindu.nursingotapp.statgraph.model.FieldRole
import com.pasindu.nursingotapp.statgraph.model.StatDataset
import com.pasindu.nursingotapp.statgraph.model.StatField
import com.pasindu.nursingotapp.statgraph.model.StatRow
import com.pasindu.nursingotapp.statgraph.model.StatValue
import com.pasindu.nursingotapp.statgraph.model.TimePeriod
import com.pasindu.nursingotapp.statgraph.model.TimePeriodParser
import com.pasindu.nursingotapp.statgraph.orchestration.ChartMappingSelection
import com.pasindu.nursingotapp.statgraph.stats.DerivedCalculationType
import com.pasindu.nursingotapp.statgraph.stats.DerivedFieldDefinition
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Thrown when serialized Stat Graph Studio dataset payloads cannot be safely
 * encoded, decoded, or match an unsupported schema version.
 */
class StatDatasetCodecException(
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

/**
 * Versioned JSON codec for Stat Graph Studio domain models.
 *
 * Guarantees:
 * - Explicit and stable type discriminators for polymorphic models ([FieldRole],
 *   [TimePeriod], [StatValue], and [DerivedCalculationType]).
 * - Strict preservation of [StatValue.Missing] vs explicit numeric zero (`0` / `0.0`).
 * - Safe failure with [StatDatasetCodecException] on unknown or unsupported schema versions
 *   instead of silently coercing or corrupting saved data.
 */
@OptIn(ExperimentalSerializationApi::class)
object StatDatasetJsonCodec {

    const val CURRENT_SCHEMA_VERSION: Int = 1

    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
        classDiscriminator = "kind"
    }

    fun encodeDataset(dataset: StatDataset): String {
        val envelope = DatasetPayloadEnvelopeDto(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            dataset = dataset.toDto()
        )
        return json.encodeToString(DatasetPayloadEnvelopeDto.serializer(), envelope)
    }

    fun decodeDataset(
        payloadJson: String,
        expectedSchemaVersion: Int = CURRENT_SCHEMA_VERSION
    ): StatDataset {
        validateSchemaVersion(expectedSchemaVersion)
        if (payloadJson.isBlank()) {
            throw StatDatasetCodecException("Dataset payload JSON is blank.")
        }
        val envelope = try {
            json.decodeFromString(DatasetPayloadEnvelopeDto.serializer(), payloadJson)
        } catch (t: Throwable) {
            throw StatDatasetCodecException(
                "Failed to parse StatDataset payload JSON: ${t.message ?: "malformed JSON"}",
                t
            )
        }
        validateSchemaVersion(envelope.schemaVersion)
        return envelope.dataset.toDomain()
    }

    fun encodeMappingSelection(selection: ChartMappingSelection): String {
        val dto = MappingSelectionEnvelopeDto(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            chartSpecId = selection.chartSpecId,
            xFieldId = selection.xFieldId,
            yFieldIds = selection.yFieldIds,
            groupFieldId = selection.groupFieldId,
            derivedFieldIds = selection.derivedFieldIds,
            userIntentQuery = selection.userIntentQuery,
            customTitle = selection.customTitle
        )
        return json.encodeToString(MappingSelectionEnvelopeDto.serializer(), dto)
    }

    fun decodeMappingSelection(
        mappingJson: String,
        expectedSchemaVersion: Int = CURRENT_SCHEMA_VERSION
    ): ChartMappingSelection {
        validateSchemaVersion(expectedSchemaVersion)
        if (mappingJson.isBlank()) {
            throw StatDatasetCodecException("Mapping selection JSON is blank.")
        }
        val dto = try {
            json.decodeFromString(MappingSelectionEnvelopeDto.serializer(), mappingJson)
        } catch (t: Throwable) {
            throw StatDatasetCodecException(
                "Failed to parse ChartMappingSelection JSON: ${t.message ?: "malformed JSON"}",
                t
            )
        }
        validateSchemaVersion(dto.schemaVersion)
        return ChartMappingSelection(
            chartSpecId = dto.chartSpecId,
            xFieldId = dto.xFieldId,
            yFieldIds = dto.yFieldIds,
            groupFieldId = dto.groupFieldId,
            derivedFieldIds = dto.derivedFieldIds,
            userIntentQuery = dto.userIntentQuery,
            customTitle = dto.customTitle
        )
    }

    private fun validateSchemaVersion(version: Int) {
        if (version != CURRENT_SCHEMA_VERSION) {
            throw StatDatasetCodecException(
                "Unsupported StatDataset schema version $version (supported: $CURRENT_SCHEMA_VERSION)."
            )
        }
    }

    // ------------------------------------------------------------------------
    // DTO definitions
    // ------------------------------------------------------------------------

    @Serializable
    private data class DatasetPayloadEnvelopeDto(
        @EncodeDefault val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val dataset: StatDatasetDto
    )

    @Serializable
    private data class MappingSelectionEnvelopeDto(
        @EncodeDefault val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val chartSpecId: String? = null,
        val xFieldId: String? = null,
        val yFieldIds: List<String> = emptyList(),
        val groupFieldId: String? = null,
        val derivedFieldIds: List<String> = emptyList(),
        val userIntentQuery: String? = null,
        val customTitle: String? = null
    )

    @Serializable
    private data class StatDatasetDto(
        val id: String,
        val name: String,
        val description: String = "",
        val fields: List<StatFieldDto> = emptyList(),
        val rows: List<StatRowDto> = emptyList(),
        val derivedFields: List<DerivedFieldDefinitionDto> = emptyList(),
        val metadata: DatasetMetadataDto
    )

    @Serializable
    private data class DatasetMetadataDto(
        val createdAtEpochMillis: Long,
        val updatedAtEpochMillis: Long,
        val sourceLabel: String
    )

    @Serializable
    private data class StatFieldDto(
        val id: String,
        val name: String,
        val unit: String = "",
        val role: FieldRoleDto,
        val suggestedRole: FieldRoleDto? = null,
        val isRoleConfirmed: Boolean = true,
        val minValue: Double? = null,
        val maxValue: Double? = null,
        val isRequired: Boolean = false,
        val allowedCategories: List<String> = emptyList()
    )

    @Serializable
    private data class StatRowDto(
        val id: String,
        val periodOrCategoryLabel: String = "",
        val values: Map<String, StatValueDto?> = emptyMap()
    )

    @Serializable
    private data class DerivedFieldDefinitionDto(
        val id: String,
        val name: String,
        val unit: String,
        val calculation: DerivedCalculationTypeDto,
        val formulaSummary: String = "",
        val decimalPlaces: Int = 2
    )

    @Serializable
    private sealed interface FieldRoleDto {
        @Serializable
        @SerialName("time_day")
        data object TimeDay : FieldRoleDto

        @Serializable
        @SerialName("time_week")
        data object TimeWeek : FieldRoleDto

        @Serializable
        @SerialName("time_month")
        data object TimeMonth : FieldRoleDto

        @Serializable
        @SerialName("time_year")
        data object TimeYear : FieldRoleDto

        @Serializable
        @SerialName("category")
        data object Category : FieldRoleDto

        @Serializable
        @SerialName("measure")
        data object Measure : FieldRoleDto

        @Serializable
        @SerialName("part_of_measure")
        data class PartOfMeasure(val parentFieldId: String? = null) : FieldRoleDto

        @Serializable
        @SerialName("rate_continuous")
        data class RateContinuous(
            val allowNegativeValues: Boolean = false,
            val scaleHint: String? = null
        ) : FieldRoleDto

        @Serializable
        @SerialName("group")
        data object Group : FieldRoleDto

        @Serializable
        @SerialName("estimate_with_limits")
        data class EstimateWithLimits(
            val lowerLabel: String = "Lower Limit",
            val upperLabel: String = "Upper Limit"
        ) : FieldRoleDto

        @Serializable
        @SerialName("duration_with_event")
        data class DurationWithEvent(
            val eventLabel: String = "Event Occurred"
        ) : FieldRoleDto

        @Serializable
        @SerialName("label")
        data object Label : FieldRoleDto
    }

    @Serializable
    private sealed interface TimePeriodDto {
        @Serializable
        @SerialName("day")
        data class Day(val isoDate: String) : TimePeriodDto

        @Serializable
        @SerialName("week")
        data class Week(val year: Int, val weekOfYear: Int) : TimePeriodDto

        @Serializable
        @SerialName("month")
        data class Month(val year: Int, val month: Int) : TimePeriodDto

        @Serializable
        @SerialName("year")
        data class Year(val year: Int) : TimePeriodDto
    }

    @Serializable
    private sealed interface StatValueDto {
        @Serializable
        @SerialName("missing")
        data object Missing : StatValueDto

        @Serializable
        @SerialName("whole_number")
        data class WholeNumber(val value: Long) : StatValueDto

        @Serializable
        @SerialName("decimal_number")
        data class DecimalNumber(val value: Double) : StatValueDto

        @Serializable
        @SerialName("text")
        data class TextValue(val value: String) : StatValueDto

        @Serializable
        @SerialName("time_period")
        data class TimePeriodValue(
            val period: TimePeriodDto? = null,
            val rawText: String = ""
        ) : StatValueDto

        @Serializable
        @SerialName("estimate")
        data class EstimateValue(
            val estimate: Double? = null,
            val lowerLimit: Double? = null,
            val upperLimit: Double? = null
        ) : StatValueDto

        @Serializable
        @SerialName("duration_event")
        data class DurationEventValue(
            val duration: Double? = null,
            val eventOccurred: Boolean? = null
        ) : StatValueDto
    }

    @Serializable
    private sealed interface DerivedCalculationTypeDto {
        @Serializable
        @SerialName("rate_percentage")
        data class RatePercentage(
            val numeratorFieldId: String,
            val denominatorFieldId: String? = null,
            val constantDenominator: Double? = null
        ) : DerivedCalculationTypeDto

        @Serializable
        @SerialName("per_thousand")
        data class PerThousand(
            val numeratorFieldId: String,
            val denominatorFieldId: String? = null,
            val constantDenominator: Double? = null
        ) : DerivedCalculationTypeDto

        @Serializable
        @SerialName("moving_average")
        data class MovingAverage(
            val sourceFieldId: String,
            val windowSize: Int = 3,
            val requireFullWindow: Boolean = false
        ) : DerivedCalculationTypeDto

        @Serializable
        @SerialName("cumulative_total")
        data class CumulativeTotal(
            val sourceFieldId: String,
            val groupByFieldId: String? = null
        ) : DerivedCalculationTypeDto
    }

    // ------------------------------------------------------------------------
    // Mappers: Domain <-> DTO
    // ------------------------------------------------------------------------

    private fun StatDataset.toDto(): StatDatasetDto = StatDatasetDto(
        id = id,
        name = name,
        description = description,
        fields = fields.map { it.toDto() },
        rows = rows.map { it.toDto() },
        derivedFields = derivedFields.map { it.toDto() },
        metadata = DatasetMetadataDto(
            createdAtEpochMillis = metadata.createdAtEpochMillis,
            updatedAtEpochMillis = metadata.updatedAtEpochMillis,
            sourceLabel = metadata.sourceLabel
        )
    )

    private fun StatDatasetDto.toDomain(): StatDataset = StatDataset(
        id = id,
        name = name,
        description = description,
        fields = fields.map { it.toDomain() },
        rows = rows.map { it.toDomain() },
        derivedFields = derivedFields.map { it.toDomain() },
        metadata = DatasetMetadata(
            createdAtEpochMillis = metadata.createdAtEpochMillis,
            updatedAtEpochMillis = metadata.updatedAtEpochMillis,
            sourceLabel = metadata.sourceLabel
        )
    )

    private fun StatField.toDto(): StatFieldDto = StatFieldDto(
        id = id,
        name = name,
        unit = unit,
        role = role.toDto(),
        suggestedRole = suggestedRole?.toDto(),
        isRoleConfirmed = isRoleConfirmed,
        minValue = minValue?.takeIf { it.isFinite() },
        maxValue = maxValue?.takeIf { it.isFinite() },
        isRequired = isRequired,
        allowedCategories = allowedCategories
    )

    private fun StatFieldDto.toDomain(): StatField = StatField(
        id = id,
        name = name,
        unit = unit,
        role = role.toDomain(),
        suggestedRole = suggestedRole?.toDomain(),
        isRoleConfirmed = isRoleConfirmed,
        minValue = minValue?.takeIf { it.isFinite() },
        maxValue = maxValue?.takeIf { it.isFinite() },
        isRequired = isRequired,
        allowedCategories = allowedCategories
    )

    private fun StatRow.toDto(): StatRowDto = StatRowDto(
        id = id,
        periodOrCategoryLabel = periodOrCategoryLabel,
        values = values.mapValues { (_, value) -> value?.toDto() }
    )

    private fun StatRowDto.toDomain(): StatRow = StatRow(
        id = id,
        periodOrCategoryLabel = periodOrCategoryLabel,
        values = values.mapValues { (_, dto) -> dto?.toDomain() }
    )

    private fun DerivedFieldDefinition.toDto(): DerivedFieldDefinitionDto = DerivedFieldDefinitionDto(
        id = id,
        name = name,
        unit = unit,
        calculation = calculation.toDto(),
        formulaSummary = formulaSummary,
        decimalPlaces = decimalPlaces
    )

    private fun DerivedFieldDefinitionDto.toDomain(): DerivedFieldDefinition = DerivedFieldDefinition(
        id = id,
        name = name,
        unit = unit,
        calculation = calculation.toDomain(),
        formulaSummary = formulaSummary,
        decimalPlaces = decimalPlaces
    )

    private fun FieldRole.toDto(): FieldRoleDto = when (this) {
        FieldRole.Time.Day -> FieldRoleDto.TimeDay
        FieldRole.Time.Week -> FieldRoleDto.TimeWeek
        FieldRole.Time.Month -> FieldRoleDto.TimeMonth
        FieldRole.Time.Year -> FieldRoleDto.TimeYear
        FieldRole.Category -> FieldRoleDto.Category
        FieldRole.Measure -> FieldRoleDto.Measure
        is FieldRole.PartOfMeasure -> FieldRoleDto.PartOfMeasure(parentFieldId = parentFieldId)
        is FieldRole.RateContinuous -> FieldRoleDto.RateContinuous(
            allowNegativeValues = allowNegativeValues,
            scaleHint = scaleHint
        )
        FieldRole.Group -> FieldRoleDto.Group
        is FieldRole.EstimateWithLimits -> FieldRoleDto.EstimateWithLimits(
            lowerLabel = lowerLabel,
            upperLabel = upperLabel
        )
        is FieldRole.DurationWithEvent -> FieldRoleDto.DurationWithEvent(eventLabel = eventLabel)
        FieldRole.Label -> FieldRoleDto.Label
    }

    private fun FieldRoleDto.toDomain(): FieldRole = when (this) {
        FieldRoleDto.TimeDay -> FieldRole.Time.Day
        FieldRoleDto.TimeWeek -> FieldRole.Time.Week
        FieldRoleDto.TimeMonth -> FieldRole.Time.Month
        FieldRoleDto.TimeYear -> FieldRole.Time.Year
        FieldRoleDto.Category -> FieldRole.Category
        FieldRoleDto.Measure -> FieldRole.Measure
        is FieldRoleDto.PartOfMeasure -> FieldRole.PartOfMeasure(parentFieldId = parentFieldId)
        is FieldRoleDto.RateContinuous -> FieldRole.RateContinuous(
            allowNegativeValues = allowNegativeValues,
            scaleHint = scaleHint
        )
        FieldRoleDto.Group -> FieldRole.Group
        is FieldRoleDto.EstimateWithLimits -> FieldRole.EstimateWithLimits(
            lowerLabel = lowerLabel,
            upperLabel = upperLabel
        )
        is FieldRoleDto.DurationWithEvent -> FieldRole.DurationWithEvent(eventLabel = eventLabel)
        FieldRoleDto.Label -> FieldRole.Label
    }

    private fun TimePeriod.toDto(): TimePeriodDto = when (this) {
        is TimePeriod.Day -> TimePeriodDto.Day(formatIso())
        is TimePeriod.Week -> TimePeriodDto.Week(year = year, weekOfYear = weekOfYear)
        is TimePeriod.Month -> TimePeriodDto.Month(year = yearMonth.year, month = yearMonth.monthValue)
        is TimePeriod.Year -> TimePeriodDto.Year(year = year)
    }

    private fun TimePeriodDto.toDomain(): TimePeriod = when (this) {
        is TimePeriodDto.Day -> {
            val parsed = TimePeriodParser.parse(isoDate) as? TimePeriod.Day
                ?: TimePeriod.Day(LocalDate.parse(isoDate))
            parsed
        }
        is TimePeriodDto.Week -> TimePeriod.Week(year = year, weekOfYear = weekOfYear)
        is TimePeriodDto.Month -> TimePeriod.Month(YearMonth.of(year, month))
        is TimePeriodDto.Year -> TimePeriod.Year(year = year)
    }

    private fun StatValue.toDto(): StatValueDto = when (this) {
        StatValue.Missing -> StatValueDto.Missing
        is StatValue.WholeNumber -> StatValueDto.WholeNumber(value = value)
        is StatValue.DecimalNumber -> {
            // Non-finite doubles represent missing data and are never coerced to zero.
            if (value.isFinite()) StatValueDto.DecimalNumber(value = value)
            else StatValueDto.Missing
        }
        is StatValue.TextValue -> StatValueDto.TextValue(value = value)
        is StatValue.TimePeriodValue -> StatValueDto.TimePeriodValue(
            period = period?.toDto(),
            rawText = rawText
        )
        is StatValue.EstimateValue -> StatValueDto.EstimateValue(
            estimate = estimate?.takeIf { it.isFinite() },
            lowerLimit = lowerLimit?.takeIf { it.isFinite() },
            upperLimit = upperLimit?.takeIf { it.isFinite() }
        )
        is StatValue.DurationEventValue -> StatValueDto.DurationEventValue(
            duration = duration?.takeIf { it.isFinite() },
            eventOccurred = eventOccurred
        )
    }

    private fun StatValueDto.toDomain(): StatValue = when (this) {
        StatValueDto.Missing -> StatValue.Missing
        is StatValueDto.WholeNumber -> StatValue.WholeNumber(value = value)
        is StatValueDto.DecimalNumber -> {
            if (value.isFinite()) StatValue.DecimalNumber(value = value)
            else StatValue.Missing
        }
        is StatValueDto.TextValue -> StatValue.TextValue(value = value)
        is StatValueDto.TimePeriodValue -> StatValue.TimePeriodValue(
            period = period?.toDomain(),
            rawText = rawText
        )
        is StatValueDto.EstimateValue -> StatValue.EstimateValue(
            estimate = estimate?.takeIf { it.isFinite() },
            lowerLimit = lowerLimit?.takeIf { it.isFinite() },
            upperLimit = upperLimit?.takeIf { it.isFinite() }
        )
        is StatValueDto.DurationEventValue -> StatValue.DurationEventValue(
            duration = duration?.takeIf { it.isFinite() },
            eventOccurred = eventOccurred
        )
    }

    private fun DerivedCalculationType.toDto(): DerivedCalculationTypeDto = when (this) {
        is DerivedCalculationType.RatePercentage -> DerivedCalculationTypeDto.RatePercentage(
            numeratorFieldId = numeratorFieldId,
            denominatorFieldId = denominatorFieldId,
            constantDenominator = constantDenominator?.takeIf { it.isFinite() }
        )
        is DerivedCalculationType.PerThousand -> DerivedCalculationTypeDto.PerThousand(
            numeratorFieldId = numeratorFieldId,
            denominatorFieldId = denominatorFieldId,
            constantDenominator = constantDenominator?.takeIf { it.isFinite() }
        )
        is DerivedCalculationType.MovingAverage -> DerivedCalculationTypeDto.MovingAverage(
            sourceFieldId = sourceFieldId,
            windowSize = windowSize,
            requireFullWindow = requireFullWindow
        )
        is DerivedCalculationType.CumulativeTotal -> DerivedCalculationTypeDto.CumulativeTotal(
            sourceFieldId = sourceFieldId,
            groupByFieldId = groupByFieldId
        )
    }

    private fun DerivedCalculationTypeDto.toDomain(): DerivedCalculationType = when (this) {
        is DerivedCalculationTypeDto.RatePercentage -> DerivedCalculationType.RatePercentage(
            numeratorFieldId = numeratorFieldId,
            denominatorFieldId = denominatorFieldId,
            constantDenominator = constantDenominator?.takeIf { it.isFinite() }
        )
        is DerivedCalculationTypeDto.PerThousand -> DerivedCalculationType.PerThousand(
            numeratorFieldId = numeratorFieldId,
            denominatorFieldId = denominatorFieldId,
            constantDenominator = constantDenominator?.takeIf { it.isFinite() }
        )
        is DerivedCalculationTypeDto.MovingAverage -> DerivedCalculationType.MovingAverage(
            sourceFieldId = sourceFieldId,
            windowSize = windowSize,
            requireFullWindow = requireFullWindow
        )
        is DerivedCalculationTypeDto.CumulativeTotal -> DerivedCalculationType.CumulativeTotal(
            sourceFieldId = sourceFieldId,
            groupByFieldId = groupByFieldId
        )
    }
}
