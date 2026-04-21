package com.dn0ne.player.app.presentation.components.playback

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastFirstOrNull
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.topbar.LazyColumnWithCollapsibleTopBar
import com.dn0ne.player.app.presentation.components.ProviderText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun LyricsSheet(
    playbackStateFlow: StateFlow<PlaybackState>,
    lyricsTextStyle: TextStyle,
    containerColor: Color,
    contentColor: Color,
    onBackClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }
    val playbackState by playbackStateFlow.collectAsState()
    val isLoadingLyrics by remember {
        derivedStateOf {
            playbackState.isLoadingLyrics
        }
    }
    val lyrics by remember {
        derivedStateOf {
            playbackState.lyrics
        }
    }
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }
    val context = LocalContext.current

    var showSyncedLyrics by remember(lyrics) {
        mutableStateOf<Boolean?>(
            when {
                lyrics?.synced != null -> true
                lyrics?.plain != null -> false
                else -> null
            }
        )
    }

    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(lyrics) {
            listState.scrollToItem(0)
        }

        if (showSyncedLyrics == true) {
            LaunchedEffect(Unit) {
                val index = lyrics?.synced?.indexOfFirst { playbackState.position < it.first } ?: -1
                if (index >= 0) {
                    listState.animateScrollToItem(
                        index = index,
                        scrollOffset = -listState.layoutInfo.viewportSize.height / 3
                    )
                }
            }
        }

        LazyColumnWithCollapsibleTopBar(
            topBarContent = {
                CompositionLocalProvider(
                    LocalContentColor provides contentColor
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = context.resources.getString(R.string.close_lyrics_sheet)
                        )
                    }

                    Text(
                        text = context.resources.getString(R.string.lyrics),
                        fontSize = lerp(
                            MaterialTheme.typography.titleLarge.fontSize,
                            MaterialTheme.typography.displaySmall.fontSize,
                            collapseFraction
                        ),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    val synced = showSyncedLyrics
                    if (lyrics?.synced != null && lyrics.plain != null && synced != null) {
                        LyricsTypeSwitch(
                            isSynced = synced,
                            onIsSyncedSwitch = {
                                showSyncedLyrics = it
                            },
                            enabled = collapseFraction == 1f,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .alpha(2 * (collapseFraction - 0.5f))
                        )
                    }
                }
            },
            collapseFraction = {
                collapseFraction = it
            },
            listState = listState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            enableScrollbar = false,
            modifier = modifier
                .background(color = containerColor)
                .clickable(enabled = false, onClick = {})
                .safeDrawingPadding()
        ) {
            when (showSyncedLyrics) {
                null -> {
                    item(key = isLoadingLyrics) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isLoadingLyrics) {
                                Text(
                                    text = context.resources.getString(R.string.loading_lyrics),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .5f),
                                    modifier = Modifier.width(100.dp)
                                )
                            } else {
                                Text(text = context.resources.getString(R.string.cant_find_lyrics))
                            }
                        }
                    }
                }

                true -> {
                    lyrics?.synced?.let { synced ->
                        val synced = synced.firstOrNull()?.first?.let { firstTime ->
                            if (firstTime > 5000) {
                                synced.toMutableList().apply {
                                    add(0, 0 to "")
                                }
                            } else synced
                        } ?: synced

                        itemsIndexed(
                            items = synced,
                            key = { index, (time, _) -> "$index-$time" }
                        ) { index, (time, line) ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val nextTime = remember {
                                    synced.getOrNull(index + 1)?.first ?: Int.MAX_VALUE
                                }

                                if (line.isNotBlank()) {
                                    SyncedLyricsLine(
                                        positionFlow = playbackStateFlow.map { it.position },
                                        time = time,
                                        nextTime = nextTime,
                                        line = line,
                                        style = lyricsTextStyle,
                                        onClick = {
                                            onSeekTo(time.toLong())
                                        },
                                        onBecomeCurrent = { textHeight ->
                                            val isItemVisible = listState.layoutInfo
                                                .visibleItemsInfo
                                                .fastFirstOrNull { it.index == index } != null

                                            if (isItemVisible && !listState.isScrollInProgress) {
                                                val offsetToCenterText =
                                                    textHeight.toInt() -
                                                            listState.layoutInfo.viewportSize.height / 2

                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(
                                                        index = index,
                                                        scrollOffset = offsetToCenterText
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    BubblesLine(
                                        positionFlow = playbackStateFlow.map { it.position },
                                        time = time,
                                        nextTime = nextTime,
                                        modifier = Modifier.align(
                                            when(lyricsTextStyle.textAlign) {
                                                TextAlign.Center -> Alignment.CenterHorizontally
                                                TextAlign.End -> Alignment.End
                                                else -> Alignment.Start
                                            }
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        if (lyrics?.areFromRemote == true) {
                            item {
                                ProviderText(
                                    providerText = context.resources.getString(R.string.lyrics_provided_by),
                                    uri = context.resources.getString(R.string.lrclib_uri),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                false -> {
                    lyrics?.plain?.let { plain ->

                        itemsIndexed(
                            items = plain,
                            key = { index, line -> "$index-$line" }
                        ) { index, line ->
                            Column {
                                PlainLyricsLine(
                                    line = line,
                                    style = lyricsTextStyle,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        if (lyrics?.areFromRemote == true) {
                            item {
                                ProviderText(
                                    providerText = context.resources.getString(R.string.lyrics_provided_by),
                                    uri = context.resources.getString(R.string.lrclib_uri),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsTypeSwitch(
    isSynced: Boolean,
    onIsSyncedSwitch: (Boolean) -> Unit,
    contentColor: Color = LocalContentColor.current,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min)
            .clip(ShapeDefaults.ExtraLarge)
            .background(color = contentColor.copy(alpha = .1f))
    ) {
        var midPoint by remember {
            mutableStateOf(0.dp)
        }
        val density = LocalDensity.current
        val capsuleOffset by animateDpAsState(
            targetValue = if (isSynced) 0.dp else midPoint,
            label = "capsule-offset-animation"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(.5f)
                .offset(x = capsuleOffset)
                .clip(ShapeDefaults.ExtraLarge)
                .background(color = contentColor.copy(alpha = .1f))
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .onGloballyPositioned {
                    midPoint = with(density) { (it.size.width / 2).toDp() }
                }
        ) {
            Text(
                text = context.resources.getString(R.string.synced),
                color = contentColor,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapeDefaults.ExtraLarge)
                    .clickable(
                        enabled = !isSynced && enabled,
                        indication = null,
                        interactionSource = remember {
                            MutableInteractionSource()
                        }
                    ) {
                        onIsSyncedSwitch(true)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                text = context.resources.getString(R.string.plain),
                color = contentColor,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(ShapeDefaults.ExtraLarge)
                    .clickable(
                        enabled = isSynced && enabled,
                        indication = null,
                        interactionSource = remember {
                            MutableInteractionSource()
                        }
                    ) {
                        onIsSyncedSwitch(false)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun PlainLyricsLine(
    line: String,
    style: TextStyle,
    color: Color = LocalContentColor.current,
    modifier: Modifier = Modifier
) {
    Text(
        text = line,
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
fun SyncedLyricsLine(
    positionFlow: Flow<Long>,
    time: Int,
    nextTime: Int,
    line: String,
    style: TextStyle,
    onClick: () -> Unit,
    onBecomeCurrent: (textHeight: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val position by positionFlow.collectAsState(0)
    var textHeight by remember {
        mutableFloatStateOf(0f)
    }
    val isCurrentLine by remember {
        derivedStateOf {
            position in time..nextTime
        }
    }

    LaunchedEffect(isCurrentLine) {
        if (isCurrentLine) {
            onBecomeCurrent(textHeight)
        }
    }

    val progressFraction by remember {
        derivedStateOf {
            ((position.toFloat() - time) / (nextTime - time))
                .coerceIn(0f, 1f)
        }
    }

    val gradientOrigin by remember {
        derivedStateOf {
            progressFraction * textHeight
        }
    }

    val localContentColor = LocalContentColor.current

    val color by animateColorAsState(
        targetValue = if (isCurrentLine) {
            localContentColor
        } else localContentColor.copy(alpha = .5f),
        label = "lyrics-color-animation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isCurrentLine) 1.05f else 1f,
        label = "current-line-scale-animation"
    )

    Text(
        text = line,
        style = style
            .copy(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = color.alpha / 2)
                    ),
                    startY = gradientOrigin - 10f,
                    endY = gradientOrigin + 10f
                ),
                shadow = Shadow(
                    color = localContentColor.copy(alpha = .5f),
                    blurRadius = if (isCurrentLine) progressFraction * 20f else 0f
                )
            ),
        color = color,
        modifier = modifier
            .onGloballyPositioned {
                textHeight = it.size.height.toFloat()
            }
            .graphicsLayer {
                transformOrigin = when (style.textAlign) {
                    TextAlign.Center -> TransformOrigin(.5f, .5f)
                    TextAlign.End -> TransformOrigin(1f, .5f)
                    else -> TransformOrigin(0f, .5f)
                }
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

@Composable
fun BubblesLine(
    positionFlow: Flow<Long>,
    time: Int,
    nextTime: Int,
    modifier: Modifier = Modifier
) {
    val position by positionFlow.collectAsState(-1)
    var bubblesContainerHeight by remember {
        mutableFloatStateOf(0f)
    }
    val isCurrentLine by remember {
        derivedStateOf {
            position >= 0L && position in time..nextTime
        }
    }

    val progressFraction by remember {
        derivedStateOf {
            ((position.toFloat() - time) / (nextTime - time))
                .coerceIn(0f, 1f)
        }
    }

    val density = LocalDensity.current
    val height = with(density) {
        MaterialTheme.typography.headlineMedium.fontSize.toDp()
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "bubbles-transition"
    )

    val firstBubbleProgress by remember {
        derivedStateOf {
            (progressFraction / .33f).coerceIn(0f, 1f)
        }
    }

    val firstBubbleTranslationX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "first-bubble-translation-x"
    )

    val secondBubbleProgress by remember {
        derivedStateOf {
            ((progressFraction - .33f) / .33f).coerceIn(0f, 1f)
        }
    }

    val secondBubbleTranslationX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = 500,
                offsetType = StartOffsetType.FastForward
            )
        ),
        label = "first-bubble-translation-x"
    )

    val thirdBubbleProgress by remember {
        derivedStateOf {
            ((progressFraction - .33f * 2) / .33f).coerceIn(0f, 1f)
        }
    }

    val thirdBubbleTranslationX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = 1000,
                offsetType = StartOffsetType.FastForward
            )
        ),
        label = "first-bubble-translation-x"
    )

    val scale by animateFloatAsState(
        targetValue = if (progressFraction < .97f) 1f else 1.2f,
        label = "bubbles-scale-before-next-line"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(height)
            .onGloballyPositioned {
                bubblesContainerHeight = it.size.height.toFloat()
            },
    ) {
        AnimatedVisibility(
            visible = isCurrentLine,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = firstBubbleProgress,
                    translationX = firstBubbleTranslationX,
                    translationOffset = secondBubbleTranslationX
                )

                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = secondBubbleProgress,
                    translationX = secondBubbleTranslationX,
                    translationOffset = thirdBubbleTranslationX
                )

                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = thirdBubbleProgress,
                    translationX = thirdBubbleTranslationX,
                    translationOffset = firstBubbleTranslationX
                )
            }
        }
    }
}

@Composable
fun Bubble(
    bubbleHeight: Dp,
    containerHeight: Float,
    animationProgress: Float,
    translationX: Float,
    translationOffset: Float,
    color: Color = LocalContentColor.current,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(bubbleHeight * .7f)
            .graphicsLayer {
                this.translationY =
                    -containerHeight / 6 *
                            (sin(20 * (animationProgress - .25f) / PI.toFloat()) / 2 + .5f) +
                            translationX * translationOffset / 2

                this.translationX = translationX
                val scale = .5f + animationProgress / 2
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                drawCircle(
                    radius = size.width,
                    brush = Brush.radialGradient(
                        0f to Color.Transparent,
                        .5f to Color.Transparent,
                        .5f to color.copy(alpha = animationProgress / 2 - .25f),
                        .6f to color.copy(alpha = animationProgress / 3 - .25f),
                        .8f to Color.Transparent,
                        radius = size.width
                    )
                )

                drawCircle(
                    color = color.copy(
                        alpha = .25f + animationProgress
                    )
                )
            }
    )
}