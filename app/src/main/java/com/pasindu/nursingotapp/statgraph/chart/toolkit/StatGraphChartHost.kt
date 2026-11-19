package com.pasindu.nursingotapp.statgraph.chart.toolkit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.AxisAndGridPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.BarChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ChartInteractionState
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ChartLegendRow
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.InteractiveChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.LineChartPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.ScatterPlotPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.SeriesMarkersPainter
import com.pasindu.nursingotapp.statgraph.chart.toolkit.painters.toPixelInsets
import com.pasindu.nursingotapp.statgraph.ui.theme.StatGraphDesignTokens
import kotlin.math.max
import kotlin.math.roundToInt

private val MaterialEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/**
 * Interactive Compose host for statistical visualizations rendered via native Compose [Canvas]
 * and [InteractiveChartPainter] implementations.
 *
 * Features:
 * - Calm Material 3 framing aligned with clinical typography and tabular numerals
 * - Draw-phase entrance reveal animation reading [Animatable.value] only inside `Canvas`
 * - Tap-to-inspect, horizontal drag scrub, two-finger pinch-to-zoom, double-tap/pill zoom reset
 * - Horizontal scroll when category density or zoom exceeds the card viewport
 * - Viewport-clamped inspection tooltip preserving series color + missingness semantics
 * - Accessible screen-reader summary (`Modifier.semantics`) + interactive data-table fallback
 */
@Composable
fun StatGraphChartHost(
    data: ChartRenderData,
    modifier: Modifier = Modifier,
    style: ChartStyle = rememberStatGraphChartStyle(),
    chartHeight: Dp = 256.dp,
    onPointSelected: ((HitTestMatch?) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val axisAndGridPainter = remember(textMeasurer) { AxisAndGridPainter(textMeasurer) }
    val markersPainter = remember { SeriesMarkersPainter() }
    val painter: InteractiveChartPainter = remember(data.chartKind, axisAndGridPainter, markersPainter) {
        com.pasindu.nursingotapp.statgraph.orchestration.ChartPainterResolver.createPainter(
            chartKind = data.chartKind,
            axisAndGridPainter = axisAndGridPainter,
            markersPainter = markersPainter,
        )
    }

    var showDataTable by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var selectedMatch by remember(data, zoomScale) { mutableStateOf<HitTestMatch?>(null) }

    val entranceProgress = remember { Animatable(0f) }
    val animationSignature = remember(data) {
        Triple(data.title, data.chartKind, data.series.sumOf { it.points.size })
    }
    LaunchedEffect(animationSignature, data.hasDrawablePoints) {
        if (data.hasDrawablePoints && !data.isLoading && data.errorMessage == null) {
            entranceProgress.snapTo(0f)
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = style.animation.entranceDurationMillis,
                    easing = MaterialEmphasizedDecelerate,
                ),
            )
        } else {
            entranceProgress.snapTo(1f)
        }
    }

    val accessibleSummary = remember(data) { ChartGeometryResolver.buildAccessibleSummary(data) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = false) {
                contentDescription = accessibleSummary
            },
        shape = StatGraphDesignTokens.Shapes.large,
        color = style.cardBackground,
        border = BorderStroke(1.dp, style.cardBorder),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ChartHeaderRow(
                data = data,
                style = style,
                zoomScale = zoomScale,
                showDataTable = showDataTable,
                onResetZoom = { zoomScale = 1f },
                onToggleTable = {
                    showDataTable = !showDataTable
                    if (showDataTable) {
                        selectedMatch = null
                    }
                },
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {
                data.isLoading -> ChartLoadingState(chartHeight = chartHeight, style = style)
                data.errorMessage != null -> ChartErrorState(
                    message = data.errorMessage,
                    chartHeight = chartHeight,
                    style = style,
                )
                !data.hasDrawablePoints -> ChartEmptyState(
                    message = data.emptyMessage,
                    chartHeight = chartHeight,
                    style = style,
                )
                showDataTable -> AccessibleChartDataTable(data = data, style = style)
                else -> {
                    InteractiveChartCanvasArea(
                        data = data,
                        style = style,
                        painter = painter,
                        chartHeight = chartHeight,
                        zoomScale = zoomScale,
                        onZoomDelta = { delta ->
                            zoomScale = (zoomScale * delta).coerceIn(1f, 3.2f)
                        },
                        onResetZoom = { zoomScale = 1f },
                        progressProvider = { entranceProgress.value },
                        selectedMatch = selectedMatch,
                        onSelectMatch = { match ->
                            selectedMatch = match
                            onPointSelected?.invoke(match)
                        },
                    )
                }
            }

            if (data.series.size > 1 && data.hasDrawablePoints && !data.isLoading && data.errorMessage == null && !showDataTable) {
                Spacer(modifier = Modifier.height(12.dp))
                ChartLegendRow(series = data.series, style = style, chartKind = data.chartKind)
            }
        }
    }
}

@Composable
private fun ChartHeaderRow(
    data: ChartRenderData,
    style: ChartStyle,
    zoomScale: Float,
    showDataTable: Boolean,
    onResetZoom: () -> Unit,
    onToggleTable: () -> Unit,
) {
    val baselineNote = remember(data) {
        when {
            data.enforceZeroBaseline -> null
            data.chartKind == ChartKind.LINE || data.chartKind == ChartKind.MULTI_LINE || data.chartKind == ChartKind.SCATTER -> {
                val values = data.series.flatMap { it.points }.mapNotNull { it.numericValue }
                val minVal = values.minOrNull()
                val maxVal = values.maxOrNull()
                if (minVal != null && maxVal != null && (minVal > 0.0 || maxVal < 0.0)) {
                    "Scale cropped near data range"
                } else {
                    null
                }
            }
            else -> null
        }
    }

    val combinedSubtitle = remember(data.subtitle, data.valueUnit, baselineNote) {
        listOfNotNull(
            data.subtitle.takeIf { it.isNotBlank() },
            data.valueUnit.takeIf { it.isNotBlank() }?.let { "Unit: $it" },
            baselineNote,
        ).joinToString(" • ")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                style = style.titleStyle,
                color = style.titleColor,
                modifier = Modifier.semantics { heading() },
            )
            if (combinedSubtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = combinedSubtitle,
                    style = style.subtitleStyle,
                    color = style.subtitleColor,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (zoomScale > 1.05f && !showDataTable) {
                TextButton(
                    onClick = onResetZoom,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomOutMap,
                        contentDescription = "Reset zoom",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            if (data.hasDrawablePoints) {
                IconButton(
                    onClick = onToggleTable,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (showDataTable) Icons.AutoMirrored.Rounded.ShowChart else Icons.Rounded.TableChart,
                        contentDescription = if (showDataTable) "Show chart view" else "Show accessible data table",
                        tint = style.subtitleColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveChartCanvasArea(
    data: ChartRenderData,
    style: ChartStyle,
    painter: InteractiveChartPainter,
    chartHeight: Dp,
    zoomScale: Float,
    onZoomDelta: (Float) -> Unit,
    onResetZoom: () -> Unit,
    progressProvider: () -> Float,
    selectedMatch: HitTestMatch?,
    onSelectMatch: (HitTestMatch?) -> Unit,
) {
    val density = LocalDensity.current
    val insetsPx = remember(density, style) { with(density) { toPixelInsets(style) } }
    val hostTextMeasurer = rememberTextMeasurer()

    Column(modifier = Modifier.fillMaxWidth()) {
        var activeAbbreviationKey by remember(data) { mutableStateOf<List<CategoryAbbreviationEntry>>(emptyList()) }
        var activeAdaptationNote by remember(data) { mutableStateOf<String?>(null) }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val viewportWidthPx = with(density) { maxWidth.toPx() }
            val defaultHeightPx = with(density) { chartHeight.toPx() }
            val fontSizePx = with(density) { style.axisLabelStyle.fontSize.toPx() }

            val layoutDecision = remember(
                data,
                viewportWidthPx,
                defaultHeightPx,
                density.density,
                fontSizePx,
                insetsPx,
                hostTextMeasurer,
            ) {
                val axisMeasurer = ComposeAxisTextMeasurer(hostTextMeasurer, style.axisLabelStyle, density)
                ChartLayoutDecisionEngine.decideForScreen(
                    data = data,
                    viewportWidthPx = viewportWidthPx,
                    defaultChartHeightPx = defaultHeightPx,
                    densityScale = density.density,
                    fontSizePx = fontSizePx,
                    baseInsets = insetsPx,
                    userExplicitlySelectedChart = data.userExplicitlySelectedChart,
                    preferredLabelMode = data.preferredLabelMode,
                    preferredBarColorMode = data.preferredBarColorMode,
                    textMeasurer = axisMeasurer,
                )
            }

            val adaptiveChartHeightDp = with(density) { layoutDecision.targetCanvasHeightPx.toDp() }
            val viewportHeightPx = layoutDecision.targetCanvasHeightPx
            val targetCanvasWidthPx = max(viewportWidthPx, layoutDecision.targetCanvasWidthPx) * zoomScale
            val isScrollable = targetCanvasWidthPx > viewportWidthPx + 2f
            val canvasWidthDp = with(density) { targetCanvasWidthPx.toDp() }
            val scrollState = rememberScrollState()

            val twoPassLayout = remember(targetCanvasWidthPx, viewportHeightPx, data, insetsPx, style, density, hostTextMeasurer) {
                AxisLabelRenderer.resolveTwoPassGeometry(
                    outerRect = ChartRect(0f, 0f, targetCanvasWidthPx, viewportHeightPx),
                    data = data,
                    baseInsets = insetsPx,
                    style = style,
                    textMeasurer = hostTextMeasurer,
                    density = density,
                )
            }
            val resolvedGeometry = twoPassLayout.geometry
            val xLabelPlan = twoPassLayout.xLabelPlan

            LaunchedEffect(xLabelPlan?.abbreviationKey, xLabelPlan?.strategy, layoutDecision.adaptationReasons) {
                activeAbbreviationKey = xLabelPlan?.abbreviationKey.orEmpty()
                activeAdaptationNote = when (xLabelPlan?.strategy) {
                    AxisLabelStrategy.ABBREVIATED_HORIZONTAL,
                    AxisLabelStrategy.ABBREVIATED_ROTATED_45,
                    AxisLabelStrategy.ABBREVIATED_ROTATED_90 ->
                        "Adaptive layout: Showing all ${data.domainLabels.size} bars with abbreviated category codes and full reference key below."
                    AxisLabelStrategy.ROTATED_45 ->
                        if (data.domainLabels.size > 6) "Adaptive layout: Category labels rotated 45° to prevent overlap." else null
                    AxisLabelStrategy.ROTATED_90 ->
                        "Adaptive layout: Category labels rotated 90° vertically to fit all ${data.domainLabels.size} categories."
                    else -> layoutDecision.adaptationReasons.firstOrNull()
                }
            }

            var tooltipSize by remember { mutableStateOf(IntSize(width = 360, height = 140)) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adaptiveChartHeightDp)
                    // Dedicated multi-touch pinch detector; single-pointer gestures pass through untouched.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            var zoomGestureActive = false
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    zoomGestureActive = true
                                    val zoomChange = event.calculateZoom()
                                    if (zoomChange != 1f && zoomChange.isFinite()) {
                                        onZoomDelta(zoomChange)
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (zoomGestureActive && pressedCount == 0) {
                                    zoomGestureActive = false
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier,
                        ),
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(canvasWidthDp)
                            .height(adaptiveChartHeightDp)
                            .semantics {
                                contentDescription = "${data.title} interactive ${data.chartKind.name.lowercase().replace('_', ' ')} chart"
                            }
                            .pointerInput(data, resolvedGeometry, zoomScale > 1.05f) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val match = ChartGeometryResolver.hitTest(
                                            touchX = offset.x,
                                            touchY = offset.y,
                                            data = data,
                                            geometry = resolvedGeometry,
                                        )
                                        onSelectMatch(match)
                                    },
                                    onDoubleTap = if (zoomScale > 1.05f) {
                                        {
                                            onSelectMatch(null)
                                            onResetZoom()
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                            .pointerInput(data, resolvedGeometry, isScrollable) {
                                if (!isScrollable) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, _ ->
                                            change.consume()
                                            val match = ChartGeometryResolver.hitTest(
                                                touchX = change.position.x,
                                                touchY = change.position.y,
                                                data = data,
                                                geometry = resolvedGeometry,
                                            )
                                            onSelectMatch(match)
                                        },
                                    )
                                } else {
                                    detectDragGesturesAfterLongPress(
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val match = ChartGeometryResolver.hitTest(
                                                touchX = change.position.x,
                                                touchY = change.position.y,
                                                data = data,
                                                geometry = resolvedGeometry,
                                            )
                                            onSelectMatch(match)
                                        },
                                    )
                                }
                            },
                    ) {
                        val rect = ChartRect(0f, 0f, size.width, size.height)
                        painter.draw(
                            canvas = this,
                            rect = rect,
                            data = data,
                            style = style,
                            progress = progressProvider(),
                            interaction = ChartInteractionState(
                                selectedDomainIndex = selectedMatch?.domainIndex,
                                showTooltipCrosshair = true,
                            ),
                            preResolvedGeometry = resolvedGeometry,
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedMatch != null,
                    enter = fadeIn(animationSpec = tween(style.animation.tooltipFadeMillis)),
                    exit = fadeOut(animationSpec = tween(style.animation.tooltipFadeMillis)),
                ) {
                    selectedMatch?.let { match ->
                        val viewportAnchorX = (match.anchorPoint.x - scrollState.value.toFloat())
                            .coerceIn(0f, viewportWidthPx)
                        val clampedPos = AxisLabelRenderer.clampTooltipPosition(
                            anchorX = viewportAnchorX,
                            anchorY = match.anchorPoint.y,
                            tooltipWidth = tooltipSize.width.toFloat(),
                            tooltipHeight = tooltipSize.height.toFloat(),
                            container = ChartRect(8f, 4f, viewportWidthPx - 8f, viewportHeightPx - 4f),
                            gap = with(density) { 10.dp.toPx() },
                        )

                        ChartInspectionTooltip(
                            match = match,
                            style = style,
                            onDismiss = { onSelectMatch(null) },
                            modifier = Modifier
                                .offset { IntOffset(clampedPos.x.roundToInt(), clampedPos.y.roundToInt()) }
                                .onSizeChanged { tooltipSize = it },
                        )
                    }
                }
            }
        }

        if (!activeAdaptationNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = activeAdaptationNote!!,
                style = style.subtitleStyle,
                color = style.subtitleColor,
            )
        }

        if (activeAbbreviationKey.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            CategoryReferenceKeySection(
                abbreviationKey = activeAbbreviationKey,
                data = data,
                style = style,
                selectedDomainIndex = selectedMatch?.domainIndex,
            )
        }
    }
}

@Composable
fun CategoryReferenceKeySection(
    abbreviationKey: List<CategoryAbbreviationEntry>,
    data: ChartRenderData,
    style: ChartStyle,
    selectedDomainIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    if (abbreviationKey.isEmpty()) return
    val useDistinctColors = ChartColorPlanner.shouldUseDistinctCategoryColors(
        data = data,
        mode = data.preferredBarColorMode,
        hasAbbreviationKey = true,
    )
    var showAllItems by remember(abbreviationKey.size) { mutableStateOf(false) }
    val maxCollapsedItems = 20
    val visibleEntries = if (showAllItems || abbreviationKey.size <= maxCollapsedItems) {
        abbreviationKey
    } else {
        abbreviationKey.take(maxCollapsedItems)
    }
    val columns = if (visibleEntries.size >= 6) 2 else 1
    val rows = visibleEntries.chunked(columns)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, style.cardBorder, StatGraphDesignTokens.Shapes.small)
            .background(
                color = style.cardBackground,
                shape = StatGraphDesignTokens.Shapes.small,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Category Reference Key (Code → Full category)",
                style = style.tooltipValueStyle,
                fontWeight = FontWeight.SemiBold,
                color = style.titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${abbreviationKey.size} items",
                style = style.subtitleStyle,
                color = style.subtitleColor,
                maxLines = 1,
            )
        }
        HorizontalDivider(color = style.cardBorder)

        rows.forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowEntries.forEach { entry ->
                    val isSelected = selectedDomainIndex == entry.domainIndex
                    val swatchColor = ChartColorPlanner.colorForCategory(
                        domainIndex = entry.domainIndex,
                        domainLabel = entry.fullLabel,
                        style = style,
                        useDistinctColors = useDistinctColors,
                        defaultSeriesColorIndex = data.series.firstOrNull()?.colorIndex ?: 0,
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(swatchColor),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${entry.shortCode}: ",
                            style = style.axisLabelStyle.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) style.crosshairColor else style.titleColor,
                            maxLines = 1,
                        )
                        Text(
                            text = entry.fullLabel,
                            style = style.axisLabelStyle,
                            color = if (isSelected) style.titleColor else style.subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rowEntries.size < columns) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        if (abbreviationKey.size > maxCollapsedItems) {
            Text(
                text = if (showAllItems) {
                    "Show first $maxCollapsedItems categories"
                } else {
                    "Show all ${abbreviationKey.size} categories (+${abbreviationKey.size - maxCollapsedItems} more)"
                },
                style = style.tooltipValueStyle,
                fontWeight = FontWeight.SemiBold,
                color = style.crosshairColor,
                modifier = Modifier
                    .clickable { showAllItems = !showAllItems }
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ChartInspectionTooltip(
    match: HitTestMatch,
    style: ChartStyle,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(min = 128.dp, max = 230.dp),
        shape = StatGraphDesignTokens.Shapes.small,
        color = style.tooltipBackground,
        border = BorderStroke(1.dp, style.tooltipBorder),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = match.domainLabel,
                    style = style.tooltipValueStyle,
                    color = style.tooltipTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss point details",
                        tint = style.subtitleColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            match.seriesEntries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(style.seriesColor(entry.colorIndex)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${entry.seriesName}: ",
                        style = style.subtitleStyle,
                        color = style.subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (entry.isMissing) "Missing" else entry.formattedValue,
                        style = style.tooltipValueStyle,
                        color = if (entry.isMissing) style.subtitleColor else style.tooltipTextColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessibleChartDataTable(
    data: ChartRenderData,
    style: ChartStyle,
) {
    val maxPreviewRows = 12
    val visibleDomainIndices = remember(data) {
        if (data.chartKind == ChartKind.SCATTER) {
            data.series.flatMap { it.points.map { p -> p.domainIndex } }.distinct().take(maxPreviewRows)
        } else {
            data.domainLabels.indices.take(maxPreviewRows)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, style.cardBorder, StatGraphDesignTokens.Shapes.small)
            .padding(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = data.domainAxisTitle.ifBlank { "Category" },
                style = style.tooltipValueStyle,
                color = style.titleColor,
                modifier = Modifier.weight(1.2f),
            )
            data.series.forEach { series ->
                Text(
                    text = series.name,
                    style = style.tooltipValueStyle,
                    color = style.seriesColor(series.colorIndex),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = style.cardBorder)

        visibleDomainIndices.forEach { domainIndex ->
            val domainLabel = data.domainLabels.getOrNull(domainIndex) ?: "#${domainIndex + 1}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = domainLabel,
                    style = style.axisLabelStyle,
                    color = style.titleColor,
                    modifier = Modifier.weight(1.2f),
                )
                data.series.forEach { series ->
                    val point = series.points.firstOrNull { it.domainIndex == domainIndex }
                    val display = when {
                        point == null || point.numericValue == null -> "Missing"
                        point.formattedValue.isNotBlank() -> point.formattedValue
                        else -> point.numericValue.toString()
                    }
                    Text(
                        text = display,
                        style = style.axisLabelStyle,
                        color = style.subtitleColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider(color = style.cardBorder.copy(alpha = 0.5f))
        }

        val totalCount = data.domainLabels.size
        if (totalCount > maxPreviewRows) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Showing first $maxPreviewRows of $totalCount records.",
                style = style.subtitleStyle,
                color = style.subtitleColor,
            )
        }
    }
}

@Composable
private fun ChartLoadingState(
    chartHeight: Dp,
    style: ChartStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preparing visualization...",
                style = style.subtitleStyle,
                color = style.subtitleColor,
            )
        }
    }
}

@Composable
private fun ChartEmptyState(
    message: String,
    chartHeight: Dp,
    style: ChartStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = StatGraphDesignTokens.Shapes.small,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = style.subtitleColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = style.subtitleStyle,
                color = style.subtitleColor,
            )
        }
    }
}

@Composable
private fun ChartErrorState(
    message: String,
    chartHeight: Dp,
    style: ChartStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .background(
                color = style.errorColor.copy(alpha = 0.06f),
                shape = StatGraphDesignTokens.Shapes.small,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = style.errorColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = style.subtitleStyle,
                color = style.errorColor,
            )
        }
    }
}

// =================================================================================================
// Compose Design-Time Previews
// =================================================================================================

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DengueTrendLineChartPreview() {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
    val sampleData = ChartRenderData(
        title = "Monthly Dengue Admissions",
        subtitle = "Ward 4B & 5A aggregate surveillance",
        chartKind = ChartKind.MULTI_LINE,
        domainAxisTitle = "Month",
        valueAxisTitle = "Admissions",
        valueUnit = "cases",
        domainLabels = months,
        series = listOf(
            ChartDataSeries(
                id = "w4b",
                name = "Ward 4B",
                unit = "cases",
                colorIndex = 0,
                markerShape = SeriesMarkerShape.CIRCLE,
                points = listOf(
                    ChartDatum(0, "Jan", 42.0, lowerLimit = 36.0, upperLimit = 48.0, formattedValue = "42 cases"),
                    ChartDatum(1, "Feb", 48.0, lowerLimit = 41.0, upperLimit = 55.0, formattedValue = "48 cases"),
                    ChartDatum(2, "Mar", null, formattedValue = "Missing"),
                    ChartDatum(3, "Apr", 61.0, lowerLimit = 52.0, upperLimit = 70.0, formattedValue = "61 cases"),
                    ChartDatum(4, "May", 54.0, lowerLimit = 47.0, upperLimit = 61.0, formattedValue = "54 cases"),
                    ChartDatum(5, "Jun", 67.0, lowerLimit = 58.0, upperLimit = 76.0, formattedValue = "67 cases"),
                ),
            ),
            ChartDataSeries(
                id = "w5a",
                name = "Ward 5A",
                unit = "cases",
                colorIndex = 1,
                markerShape = SeriesMarkerShape.DIAMOND,
                dashed = true,
                points = listOf(
                    ChartDatum(0, "Jan", 29.0, formattedValue = "29 cases"),
                    ChartDatum(1, "Feb", 33.0, formattedValue = "33 cases"),
                    ChartDatum(2, "Mar", 38.0, formattedValue = "38 cases"),
                    ChartDatum(3, "Apr", 44.0, formattedValue = "44 cases"),
                    ChartDatum(4, "May", 39.0, formattedValue = "39 cases"),
                    ChartDatum(5, "Jun", 46.0, formattedValue = "46 cases"),
                ),
            ),
        ),
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StatGraphChartHost(data = sampleData)
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DengueGroupedBarChartPreview() {
    val wards = listOf("ICU", "Ward 4B", "Ward 5A", "Paeds")
    val barData = ChartRenderData(
        title = "Severity Mix by Unit",
        subtitle = "Zero-anchored comparison",
        chartKind = ChartKind.GROUPED_BAR,
        domainAxisTitle = "Clinical Unit",
        valueAxisTitle = "Patients",
        valueUnit = "pts",
        domainLabels = wards,
        series = listOf(
            ChartDataSeries(
                id = "df",
                name = "Dengue Fever",
                colorIndex = 0,
                markerShape = SeriesMarkerShape.SQUARE,
                points = listOf(
                    ChartDatum(0, "ICU", 8.0, formattedValue = "8"),
                    ChartDatum(1, "Ward 4B", 31.0, formattedValue = "31"),
                    ChartDatum(2, "Ward 5A", 24.0, formattedValue = "24"),
                    ChartDatum(3, "Paeds", 19.0, formattedValue = "19"),
                ),
            ),
            ChartDataSeries(
                id = "dhf",
                name = "DHF",
                colorIndex = 2,
                markerShape = SeriesMarkerShape.TRIANGLE,
                points = listOf(
                    ChartDatum(0, "ICU", 14.0, formattedValue = "14"),
                    ChartDatum(1, "Ward 4B", 9.0, formattedValue = "9"),
                    ChartDatum(2, "Ward 5A", 6.0, formattedValue = "6"),
                    ChartDatum(3, "Paeds", 11.0, formattedValue = "11"),
                ),
            ),
        ),
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StatGraphChartHost(data = barData)
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DenseOverflowAndEmptyPreview() {
    val denseLabels = (1..16).map { "W$it" }
    val denseData = ChartRenderData(
        title = "16-Week Triage Volume",
        subtitle = "Scrollable horizontal viewport",
        chartKind = ChartKind.LINE,
        domainLabels = denseLabels,
        series = listOf(
            ChartDataSeries(
                id = "triage",
                name = "Screened",
                colorIndex = 0,
                points = denseLabels.mapIndexed { idx, label ->
                    val v = 80.0 + (idx * 5) % 35
                    ChartDatum(idx, label, v, formattedValue = "${v.toInt()}")
                },
            ),
        ),
    )
    val emptyData = ChartRenderData(
        title = "Post-Discharge Follow-up",
        chartKind = ChartKind.BAR,
        domainLabels = emptyList(),
        series = emptyList(),
        emptyMessage = "No aggregate observations recorded for the selected period.",
    )

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StatGraphChartHost(data = denseData, chartHeight = 200.dp)
            Spacer(modifier = Modifier.height(12.dp))
            StatGraphChartHost(data = emptyData, chartHeight = 140.dp)
        }
    }
}
