package com.dn0ne.player.app.presentation.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew

import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.playback.PlainLyricsLine
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.data.Settings

@Composable
fun LyricsSettings(
    settings: Settings,
    onBackClick: () -> Unit,
    onLyricsFolderPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var collapseFraction by remember {
        mutableFloatStateOf(0f)
    }

    ColumnWithCollapsibleTopBar(
        topBarContent = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = context.resources.getString(R.string.back)
                )
            }

            Text(
                text = context.resources.getString(R.string.lyrics),
                fontSize = lerp(
                    MaterialTheme.typography.titleLarge.fontSize,
                    MaterialTheme.typography.displaySmall.fontSize,
                    collapseFraction
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            )
        },
        collapseFraction = {
            collapseFraction = it
        },
        contentPadding = PaddingValues(horizontal = 28.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        var useDarkPaletteOnLyricsSheet by remember {
            mutableStateOf(settings.useDarkPaletteOnLyricsSheet)
        }
        val appearance by settings.appearance.collectAsState()
        val isDarkTheme = when (appearance) {
            Theme.Appearance.System -> isSystemInDarkTheme()
            Theme.Appearance.Light -> false
            Theme.Appearance.Dark -> true
        }

        SettingSwitch(
            title = context.resources.getString(R.string.dark_theme_for_lyrics),
            supportingText = context.resources.getString(R.string.dark_theme_for_lyrics_explain),
            icon = Icons.Rounded.DarkMode,
            isChecked = useDarkPaletteOnLyricsSheet,
            onCheckedChange = {
                settings.useDarkPaletteOnLyricsSheet = it
                useDarkPaletteOnLyricsSheet = it
            },
            modifier = Modifier.fillMaxWidth()
        )

        SettingIconButton(
            title = context.resources.getString(R.string.lyrics_folder),
            supportingText = context.resources.getString(R.string.lyrics_folder_supporting_text),
            icon = Icons.Rounded.Folder,
            buttonIcon = Icons.Rounded.Folder,
            buttonContentDescription = context.resources.getString(R.string.pick_lyrics_folder),
            onButtonClick = onLyricsFolderPick,
            modifier = Modifier.fillMaxWidth()
        )

        var lyricsFontSize by remember {
            mutableStateOf(settings.lyricsFontSize)
        }
        var lyricsFontWeight by remember {
            mutableIntStateOf(settings.lyricsFontWeight)
        }
        var lyricsAlignment by remember {
            mutableStateOf(settings.lyricsAlignment)
        }
        var lyricsLineHeight by remember {
            mutableStateOf(settings.lyricsLineHeight)
        }
        var lyricsLetterSpacing by remember {
            mutableStateOf(settings.lyricsLetterSpacing)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeDefaults.Large)
                .background(
                    color = when {
                        useDarkPaletteOnLyricsSheet && !isDarkTheme -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.inversePrimary
                    }
                )
                .padding(16.dp)
        ) {
            PlainLyricsLine(
                line = context.resources.getString(R.string.lyrics_preview_string),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = lyricsFontSize,
                    fontWeight = FontWeight(lyricsFontWeight),
                    lineHeight = lyricsLineHeight,
                    letterSpacing = lyricsLetterSpacing,
                    textAlign = lyricsAlignment
                ),
                color = when {
                    useDarkPaletteOnLyricsSheet && !isDarkTheme -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.inverseSurface
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingSlider(
            title = context.resources.getString(R.string.alignment),
            value = when (lyricsAlignment) {
                TextAlign.Center -> 1f
                TextAlign.End -> 2f
                else -> 0f
            },
            valueToShow = when (lyricsAlignment) {
                TextAlign.Center -> context.resources.getString(R.string.center)
                TextAlign.End -> context.resources.getString(R.string.end)
                else -> context.resources.getString(R.string.start)
            },
            onValueChange = {
                lyricsAlignment = when (it) {
                    1f -> TextAlign.Center
                    2f -> TextAlign.End
                    else -> TextAlign.Start
                }
            },
            onValueChangeFinished = {
                settings.lyricsAlignment = lyricsAlignment
            },
            steps = 1,
            valueRange = 0f..2f
        )

        SettingSlider(
            title = context.resources.getString(R.string.font_size),
            value = lyricsFontSize.value,
            onValueChange = {
                lyricsFontSize = it.sp
            },
            onValueChangeFinished = {
                settings.lyricsFontSize = lyricsFontSize
            },
            steps = 33,
            valueRange = 16f..50f
        )

        SettingSlider(
            title = context.resources.getString(R.string.font_weight),
            value = lyricsFontWeight.toFloat(),
            onValueChange = {
                lyricsFontWeight = it.toInt()
            },
            onValueChangeFinished = {
                settings.lyricsFontWeight = lyricsFontWeight
            },
            steps = 7,
            valueRange = 100f..900f
        )

        SettingSlider(
            title = context.resources.getString(R.string.line_height),
            value = lyricsLineHeight.value,
            onValueChange = {
                lyricsLineHeight = it.sp
            },
            onValueChangeFinished = {
                settings.lyricsLineHeight = lyricsLineHeight
            },
            steps = 33,
            valueRange = 16f..50f
        )

        SettingSlider(
            title = context.resources.getString(R.string.letter_spacing),
            value = lyricsLetterSpacing.value,
            onValueChange = {
                lyricsLetterSpacing = it.sp
            },
            onValueChangeFinished = {
                settings.lyricsLetterSpacing = lyricsLetterSpacing
            },
            steps = 3,
            valueRange = -2f..2f
        )

        FilledTonalButton(
            onClick = {
                settings.resetLyricsStyle()
                lyricsFontSize = settings.lyricsFontSize
                lyricsFontWeight = settings.lyricsFontWeight
                lyricsAlignment = settings.lyricsAlignment
                lyricsLineHeight = settings.lyricsLineHeight
                lyricsLetterSpacing = settings.lyricsLetterSpacing
            }
        ) {
            Text(text = context.resources.getString(R.string.reset))
        }
    }
}