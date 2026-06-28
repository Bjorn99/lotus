package com.dn0ne.player.app.presentation.components.topbar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import com.dn0ne.player.app.presentation.components.animatable.rememberAnimatable
import com.dn0ne.player.app.presentation.components.isSystemInLandscapeOrientation
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun LazyGridWithCollapsibleTabsTopBar(
    topBarTabs: List<Tab>,
    defaultSelectedTab: Tab = Tab.Tracks,
    onTabChange: (tab: Tab) -> Unit = {},
    tabTitleTextStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    tabRowTitleTextStyle: TextStyle = MaterialTheme.typography.titleLarge,
    topBarButtons: @Composable BoxScope.(tab: Tab) -> Unit = {},
    minTopBarHeight: Dp = 60.dp,
    maxTopBarHeight: Dp = 250.dp,
    maxTopBarHeightLandscape: Dp = 150.dp,
    collapsedByDefault: Boolean = false,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentHorizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    contentVerticalArrangement: Arrangement.Vertical = Arrangement.Top,
    enableScrollbar: Boolean = true,
    modifier: Modifier = Modifier,
    gridCells: (tab: Tab) -> GridCells = { GridCells.Fixed(1) },
    tabContent: LazyGridScope.(tab: Tab) -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val isInLandscapeOrientation = isSystemInLandscapeOrientation()
    val minTopBarHeight = remember { with(density) { minTopBarHeight.toPx() } }
    val maxTopBarHeight = remember {
        with(density) {
            if (isInLandscapeOrientation) {
                maxTopBarHeightLandscape.toPx()
            } else maxTopBarHeight.toPx()
        }
    }
    val topBarHeight = rememberAnimatable(
        initialValue = if (collapsedByDefault || isInLandscapeOrientation) {
            minTopBarHeight
        } else maxTopBarHeight
    )

    var selectedTab by rememberSaveable {
        mutableStateOf(defaultSelectedTab)
    }

    LaunchedEffect(isInLandscapeOrientation) {
        if (isInLandscapeOrientation) {
            topBarHeight.snapTo(minTopBarHeight)
        } else {
            topBarHeight.snapTo(maxTopBarHeight)
        }
    }

    // Reset collapsed top bar to full height on tab change.
    // Using snapTo instead of animateTo to avoid layout collision
    // with the tab row's own scrollToItem animation — the two
    // concurrent animations cause "Place was called on a node
    // which was placed already" (IllegalStateException).
    LaunchedEffect(selectedTab) {
        if (topBarHeight.value < maxTopBarHeight) {
            topBarHeight.snapTo(maxTopBarHeight)
        }
    }

    val titleLarge = MaterialTheme.typography.titleLarge
    val displaySmall = MaterialTheme.typography.displaySmall
    val activeTitleTextStyle = remember(topBarHeight.value) {
        val fraction = (topBarHeight.value - minTopBarHeight) / (maxTopBarHeight - minTopBarHeight)
        titleLarge.copy(
            fontSize = lerp(
                titleLarge.fontSize,
                displaySmall.fontSize,
                fraction
            ),
            fontWeight = FontWeight.Bold
        )
    }

    val topBarScrollConnection = remember {
        return@remember object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val previousHeight = topBarHeight.value
                val newHeight = if (gridState.firstVisibleItemIndex >= 0 && available.y < 0) {
                    (previousHeight + available.y).coerceIn(
                        minTopBarHeight,
                        maxTopBarHeight
                    )
                } else if (
                    gridState.firstVisibleItemIndex == 0 &&
                    gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.offset?.y == 0
                ) {
                    (previousHeight + available.y).coerceIn(
                        minTopBarHeight,
                        maxTopBarHeight
                    )
                } else previousHeight

                if (newHeight != previousHeight) {
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }
                return Offset(0f, newHeight - previousHeight)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                coroutineScope.launch {
                    val threshold = (maxTopBarHeight - minTopBarHeight)
                    topBarHeight.animateTo(
                        targetValue = if (topBarHeight.value < threshold) minTopBarHeight else maxTopBarHeight,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                }

                return super.onPostFling(consumed, available)
            }
        }
    }

    var showTabRow by remember {
        mutableStateOf(false)
    }
    val isColumnScrollInProgress by remember {
        derivedStateOf {
            gridState.isScrollInProgress
        }
    }

    LaunchedEffect(isColumnScrollInProgress) {
        if (showTabRow && isColumnScrollInProgress) {
            showTabRow = false
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(topBarScrollConnection)
    ) {
        Column {
            Spacer(
                modifier = Modifier
                    .height(with(density) { topBarHeight.value.toDp() })
            )
            AnimatedContent(
                targetState = selectedTab,
                label = "column-tab-animation",
            ) { tabIndex ->
                if (selectedTab != defaultSelectedTab) {
                    BackHandler {
                        selectedTab = defaultSelectedTab
                        onTabChange(defaultSelectedTab)
                    }
                }

                LazyVerticalGridScrollbar(
                    state = gridState,
                    settings = ScrollbarSettings(
                        enabled = enableScrollbar,
                        thumbUnselectedColor = MaterialTheme.colorScheme.surfaceContainer,
                        thumbSelectedColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                ) {
                    LazyVerticalGrid(
                        columns = gridCells(tabIndex),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        horizontalArrangement = contentHorizontalArrangement,
                        verticalArrangement = contentVerticalArrangement
                    ) {
                        tabContent(tabIndex)

                        item(
                            span = {
                                GridItemSpan(this.maxLineSpan)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { topBarHeight.value.toDp() })
        ) {
            val tabListState = rememberLazyListState()
            val viewportWidth by remember {
                derivedStateOf {
                    tabListState.layoutInfo.viewportSize.width
                }
            }
            val maxTabWidth = 120.dp
            // Fixed dp value — NOT derived from viewportWidth because
            // viewportWidth is 0 during initial tab-row composition,
            // which would cause 0dp max-width items to crash the
            // scrollToItem call (forceRemeasure with zero-width items
            // triggers "Place was called on a node which was placed
            // already" in LookaheadPassDelegate).
            val boundTransformAnimationSpec = remember { spring<Rect>() }
            val contentAnimationSpec = remember { spring<Float>() }

            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                SharedTransitionLayout {
                    val selectedTabIndex by remember {
                        derivedStateOf {
                            topBarTabs.indexOf(selectedTab)
                        }
                    }
                    AnimatedContent(
                        targetState = showTabRow,
                        transitionSpec = {
                            scaleIn(contentAnimationSpec, initialScale = 1.5f) +
                                    fadeIn(contentAnimationSpec) togetherWith
                                    scaleOut(contentAnimationSpec, targetScale = 1.5f) +
                                    fadeOut(contentAnimationSpec)
                        },
                        label = "top-bar-title-animation"
                    ) { state ->
                        when (state) {
                            false -> {
                                val listItemsCount by remember {
                                    derivedStateOf {
                                        tabListState.layoutInfo.totalItemsCount
                                    }
                                }

                                LaunchedEffect(listItemsCount) {
                                    tabListState.scrollToItem(
                                        index = selectedTabIndex + 1,
                                    )

                                    tabListState.scrollToItem(
                                        index = selectedTabIndex + 1,
                                        scrollOffset = -viewportWidth / 2 + (tabListState
                                            .layoutInfo
                                            .visibleItemsInfo
                                            .fastFirstOrNull { it.index == selectedTabIndex + 1 }?.size ?: 0) / 2
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null
                                        ) {
                                            showTabRow = true
                                        }
                                ) {
                                    TabTitle(
                                        selectedTab = selectedTab,
                                        style = activeTitleTextStyle,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundTransformAnimationSpec = boundTransformAnimationSpec,
                                        modifier = Modifier.align(Alignment.Center)
                                    )

                                    topBarButtons(selectedTab)
                                }
                            }

                            true -> {
                                LaunchedEffect(Unit) {
                                    tabListState.scrollToItem(
                                        index = topBarTabs.indexOf(selectedTab) + 1,
                                    )

                                    tabListState.scrollToItem(
                                        index = topBarTabs.indexOf(selectedTab) + 1,
                                        scrollOffset = -viewportWidth / 2 + (tabListState
                                            .layoutInfo
                                            .visibleItemsInfo
                                            .fastFirstOrNull { it.index == topBarTabs.indexOf(selectedTab) + 1 }?.size ?: 0) / 2
                                    )
                                }

                                LazyRow(
                                    state = tabListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            indication = null
                                        ) {
                                            showTabRow = false
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item {
                                        Spacer(
                                            modifier = Modifier.width(
                                                with(density) {
                                                    (viewportWidth / 2.5f).toDp()
                                                }
                                            )
                                        )
                                    }

                                    itemsIndexed(
                                        items = topBarTabs,
                                        key = { index, tab -> "$index-$tab" }
                                    ) { index, tab ->
                                        TabRowTitle(
                                            selectedTab = selectedTab,
                                            tab = tab,
                                            style = tabRowTitleTextStyle,
                                            onClick = {
                                                selectedTab = tab
                                                onTabChange(tab)
                                                showTabRow = false
                                            },
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundTransformAnimationSpec = boundTransformAnimationSpec,
                                            modifier = Modifier.widthIn(max = maxTabWidth)
                                        )
                                    }

                                    item {
                                        Spacer(
                                            modifier = Modifier.width(
                                                with(density) {
                                                    (viewportWidth / 2.5f).toDp()
                                                }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TabTitle(
    selectedTab: Tab,
    style: TextStyle,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundTransformAnimationSpec: FiniteAnimationSpec<Rect>,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Min)
        ) {
            val context = LocalContext.current
            Text(
                text = context.resources.getString(selectedTab.titleResId),
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(selectedTab),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            boundTransformAnimationSpec
                        }
                    )
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TabRowTitle(
    selectedTab: Tab,
    tab: Tab,
    style: TextStyle,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundTransformAnimationSpec: FiniteAnimationSpec<Rect>,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .heightIn(min = 60.dp)
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null
                ) {
                    onClick()
                }
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current
            key("$selectedTab-$tab") {
                Text(
                    text = context.resources.getString(tab.titleResId),
                    style = style,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(tab),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                boundTransformAnimationSpec
                            }
                        )
                )

                val primary = MaterialTheme.colorScheme.primary
                val color by remember {
                    mutableStateOf(
                        if (selectedTab == tab) {
                            primary
                        } else Color.Transparent
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(ShapeDefaults.ExtraLarge)
                        .background(
                            color = color
                        )
                )
            }
        }
    }
}