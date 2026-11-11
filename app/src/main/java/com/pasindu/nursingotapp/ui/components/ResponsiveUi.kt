package com.pasindu.nursingotapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasindu.nursingotapp.ui.theme.BrandIndigo
import com.pasindu.nursingotapp.ui.theme.NursingDimensions
import com.pasindu.nursingotapp.ui.theme.NursingMotion
import com.pasindu.nursingotapp.ui.theme.Slate
import com.pasindu.nursingotapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay

data class NursingBottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

internal fun shouldStackTopBarActions(
    fontScale: Float,
    stackWideActionsAtLargeFont: Boolean
): Boolean = stackWideActionsAtLargeFont && fontScale >= 1.5f

internal fun shouldUseScrollableBottomNav(fontScale: Float): Boolean = fontScale >= 1.35f

internal fun shouldUseCompactMonthGridText(fontScale: Float): Boolean = fontScale > 1.5f

/**
 * Adaptive Material 3 header surface that maintains standard 64.dp bar ergonomics at 1.0x
 * font scale while expanding vertically via [heightIn] and [wrapContentHeight] so multi-line
 * titles and subtitles never clip or overlap scrolled Scaffold body content at up to 2.0x font scale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NursingAdaptiveTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    stackWideActionsAtLargeFont: Boolean = false
) {
    val fontScale = LocalDensity.current.fontScale
    val stackActions = actions != null && shouldStackTopBarActions(fontScale, stackWideActionsAtLargeFont)

    Surface(
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .heightIn(min = 64.dp)
                .wrapContentHeight()
                .padding(
                    start = if (navigationIcon != null) 4.dp else 16.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = NursingDimensions.TouchTarget.minimum),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                    Spacer(Modifier.width(4.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    title()
                }
                if (actions != null && !stackActions) {
                    Spacer(Modifier.width(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }

            if (actions != null && stackActions) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (navigationIcon != null) 48.dp else 0.dp,
                            top = 4.dp,
                            bottom = 2.dp
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
        }
    }
}

/**
 * Shared root bottom navigation bar that preserves equal-width Material 3 [NavigationBar] tabs
 * at normal/medium font sizes and switches to a horizontally scrollable Material 3 pill row at
 * large accessibility scales (>= 1.35x) so 5-tab labels never split mid-word.
 */
@Composable
fun NursingAdaptiveBottomBar(
    items: List<NursingBottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets
) {
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val selectedColor = BrandIndigo
    val indicatorColor = BrandIndigo.copy(alpha = 0.12f)
    val unselectedIconColor = TextSecondary
    val unselectedTextColor = TextSecondary
    val dividerColor = Color(0xFFE2E8F0)

    // Optimistic selection feedback synchronizes immediately on tap while resetting cleanly
    // when NavController commits the new destination (or after a brief guard timeout).
    var pendingRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentRoute) {
        pendingRoute = null
    }
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            delay(300L)
            pendingRoute = null
        }
    }
    val activeRoute = pendingRoute ?: currentRoute
    val handleItemClick: (String) -> Unit = { route ->
        if (route != currentRoute) {
            pendingRoute = route
        }
        onNavigate(route)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = dividerColor)
        if (!shouldUseScrollableBottomNav(fontScale)) {
            NavigationBar(
                containerColor = containerColor,
                contentColor = Slate,
                tonalElevation = 0.dp,
                windowInsets = windowInsets
            ) {
                items.forEach { destination ->
                    key(destination.route) {
                        val selected = activeRoute == destination.route
                        val interactionSource = remember(destination.route) { MutableInteractionSource() }
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.05f else 1f,
                            animationSpec = tween(
                                durationMillis = NursingMotion.microInteractionDurationMs,
                                easing = NursingMotion.standardEasing
                            ),
                            label = "bottom_nav_icon_scale_${destination.route}"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = { handleItemClick(destination.route) },
                            alwaysShowLabel = true,
                            interactionSource = interactionSource,
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = selectedColor,
                                selectedTextColor = selectedColor,
                                indicatorColor = indicatorColor,
                                unselectedIconColor = unselectedIconColor,
                                unselectedTextColor = unselectedTextColor
                            )
                        )
                    }
                }
            }
        } else {
            val scrollState = rememberScrollState()
            var viewportWidthPx by remember { mutableIntStateOf(0) }
            val itemBounds = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
            val edgeMarginPx = with(density) { 16.dp.roundToPx() }

            LaunchedEffect(activeRoute, viewportWidthPx, itemBounds.size) {
                val route = activeRoute ?: return@LaunchedEffect
                val bounds = itemBounds[route] ?: return@LaunchedEffect
                if (viewportWidthPx <= 0 || scrollState.maxValue <= 0) return@LaunchedEffect
                val (left, right) = bounds
                val visibleStart = scrollState.value
                val visibleEnd = visibleStart + viewportWidthPx
                val targetScroll = when {
                    left < visibleStart + edgeMarginPx -> (left - edgeMarginPx).coerceAtLeast(0)
                    right > visibleEnd - edgeMarginPx ->
                        (right - viewportWidthPx + edgeMarginPx).coerceAtMost(scrollState.maxValue)
                    else -> null
                }
                if (targetScroll != null && targetScroll != scrollState.value) {
                    scrollState.animateScrollTo(
                        value = targetScroll,
                        animationSpec = tween(
                            durationMillis = NursingMotion.inputAnimationDurationMs,
                            easing = NursingMotion.standardEasing
                        )
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = containerColor,
                contentColor = Slate
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(windowInsets)
                        .onSizeChanged { viewportWidthPx = it.width }
                        .heightIn(min = 80.dp)
                        .horizontalScroll(scrollState)
                        .padding(horizontal = NursingDimensions.Spacing.sm)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(NursingDimensions.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { destination ->
                        key(destination.route) {
                            val selected = activeRoute == destination.route
                            val interactionSource = remember(destination.route) { MutableInteractionSource() }
                            val pillWidth by animateDpAsState(
                                targetValue = if (selected) 56.dp else 28.dp,
                                animationSpec = tween(
                                    durationMillis = NursingMotion.inputAnimationDurationMs,
                                    easing = NursingMotion.standardEasing
                                ),
                                label = "scrollable_nav_pill_width_${destination.route}"
                            )
                            val pillColor by animateColorAsState(
                                targetValue = if (selected) indicatorColor else Color.Transparent,
                                animationSpec = tween(
                                    durationMillis = NursingMotion.microInteractionDurationMs,
                                    easing = NursingMotion.standardEasing
                                ),
                                label = "scrollable_nav_pill_color_${destination.route}"
                            )
                            val iconTint by animateColorAsState(
                                targetValue = if (selected) selectedColor else unselectedIconColor,
                                animationSpec = tween(
                                    durationMillis = NursingMotion.microInteractionDurationMs,
                                    easing = NursingMotion.standardEasing
                                ),
                                label = "scrollable_nav_icon_tint_${destination.route}"
                            )
                            val labelColor by animateColorAsState(
                                targetValue = if (selected) selectedColor else unselectedTextColor,
                                animationSpec = tween(
                                    durationMillis = NursingMotion.microInteractionDurationMs,
                                    easing = NursingMotion.standardEasing
                                ),
                                label = "scrollable_nav_label_color_${destination.route}"
                            )
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.05f else 1f,
                                animationSpec = tween(
                                    durationMillis = NursingMotion.microInteractionDurationMs,
                                    easing = NursingMotion.standardEasing
                                ),
                                label = "scrollable_nav_icon_scale_${destination.route}"
                            )

                            Column(
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        val left = coords.positionInParent().x.toInt()
                                        itemBounds[destination.route] = left to (left + coords.size.width)
                                    }
                                    .widthIn(min = 80.dp)
                                    .heightIn(min = 76.dp)
                                    .clip(RoundedCornerShape(NursingDimensions.Radius.large))
                                    .selectable(
                                        selected = selected,
                                        interactionSource = interactionSource,
                                        indication = ripple(bounded = true, color = selectedColor),
                                        onClick = { handleItemClick(destination.route) },
                                        role = Role.Tab
                                    )
                                    .padding(horizontal = NursingDimensions.Spacing.md, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(pillWidth)
                                            .height(32.dp)
                                            .clip(CircleShape)
                                            .background(pillColor)
                                    )
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                        tint = iconTint,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = labelColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
