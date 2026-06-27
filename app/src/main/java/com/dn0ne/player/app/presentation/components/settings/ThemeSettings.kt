package com.dn0ne.player.app.presentation.components.settings

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastForEach
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.settings.Theme.Appearance
import com.dn0ne.player.app.presentation.components.settings.Theme.PaletteStyle
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.data.Settings
import com.kmpalette.DominantColorState
import com.materialkolor.DynamicMaterialTheme

@Composable
fun ThemeSettings(
    settings: Settings,
    onBackClick: () -> Unit,
    dominantColorState: DominantColorState<ImageBitmap>,
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
                text = context.resources.getString(R.string.theme),
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
        val isDarkTheme = isSystemInDarkTheme()
        val selectedAppearance by settings.appearance.collectAsState()
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val appearanceOptions = remember(surfaceColor, onSurfaceColor) {
            listOf(
                AppearanceOption(
                    title = context.resources.getString(R.string.appearance_system),
                    onSelection = {
                        settings.updateAppearance(Appearance.System)
                    },
                    appearance = Appearance.System,
                    icon = Icons.Rounded.PhonelinkSetup,
                    containerColor = surfaceColor,
                    contentColor = onSurfaceColor
                ),
                AppearanceOption(
                    title = context.resources.getString(R.string.appearance_light),
                    onSelection = {
                        settings.updateAppearance(Appearance.Light)
                    },
                    appearance = Appearance.Light,
                    icon = Icons.Rounded.LightMode,
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                AppearanceOption(
                    title = context.resources.getString(R.string.appearance_dark),
                    onSelection = {
                        settings.updateAppearance(Appearance.Dark)
                    },
                    appearance = Appearance.Dark,
                    icon = Icons.Rounded.DarkMode,
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
            )
        }

        SettingOptionsRow(
            title = context.resources.getString(R.string.appearance),
            options = appearanceOptions,
            modifier = Modifier.fillMaxWidth()
        ) { option ->
            Column(
                modifier = Modifier
                    .clip(ShapeDefaults.Large)
                    .clickable {
                        option.onSelection()
                    }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isSelected by remember {
                    derivedStateOf {
                        selectedAppearance == option.appearance
                    }
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(color = option.containerColor)
                        .border(
                            width = animateDpAsState(
                                targetValue = if (isSelected) 2.dp else (-1).dp,
                                label = ""
                            ).value,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.contentColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val selectedPalette by settings.paletteStyle.collectAsState()
        val paletteOptions = remember {
            listOf(
                PaletteOption(
                    title = context.resources.getString(R.string.palette_tonal_spot),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.TonalSpot)
                    },
                    style = PaletteStyle.TonalSpot
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_neutral),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.Neutral)
                    },
                    style = PaletteStyle.Neutral
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_vibrant),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.Vibrant)
                    },
                    style = PaletteStyle.Vibrant
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_expressive),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.Expressive)
                    },
                    style = PaletteStyle.Expressive
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_rainbow),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.Rainbow)
                    },
                    style = PaletteStyle.Rainbow
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_fruit_salad),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.FruitSalad)
                    },
                    style = PaletteStyle.FruitSalad
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_monochrome),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.Monochrome)
                    },
                    style = PaletteStyle.Monochrome
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_fidelity),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.Fidelity)
                    },
                    style = PaletteStyle.Fidelity
                ),
                PaletteOption(
                    title = context.resources.getString(R.string.palette_content),
                    onSelection = {
                        settings.updatePaletteStyle(PaletteStyle.Content)
                    },
                    style = PaletteStyle.Content
                )
            )
        }

        SettingOptionsRow(
            title = context.resources.getString(R.string.palette_style),
            options = paletteOptions,
            modifier = Modifier.fillMaxWidth()
        ) { option ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(ShapeDefaults.Large)
                    .clickable {
                        option.onSelection()
                    }
                    .padding(8.dp)
            ) {
                val isSelected by remember {
                    derivedStateOf {
                        selectedPalette == option.style
                    }
                }
                Column(
                    modifier = Modifier
                        .clip(ShapeDefaults.Medium)
                        .width(60.dp)
                        .border(
                            width = animateDpAsState(
                                targetValue = if (isSelected) 2.dp else (-1).dp,
                                label = ""
                            ).value,
                            color = MaterialTheme.colorScheme.primary,
                            shape = ShapeDefaults.Medium
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DynamicMaterialTheme(
                        seedColor = dominantColorState.color,
                        isDark = when(selectedAppearance) {
                            Appearance.System -> isDarkTheme
                            Appearance.Light -> false
                            Appearance.Dark -> true
                        },
                        style = when (option.style) {
                            PaletteStyle.TonalSpot -> com.materialkolor.PaletteStyle.TonalSpot
                            PaletteStyle.Neutral -> com.materialkolor.PaletteStyle.Neutral
                            PaletteStyle.Vibrant -> com.materialkolor.PaletteStyle.Vibrant
                            PaletteStyle.Expressive -> com.materialkolor.PaletteStyle.Expressive
                            PaletteStyle.Rainbow -> com.materialkolor.PaletteStyle.Rainbow
                            PaletteStyle.FruitSalad -> com.materialkolor.PaletteStyle.FruitSalad
                            PaletteStyle.Monochrome -> com.materialkolor.PaletteStyle.Monochrome
                            PaletteStyle.Fidelity -> com.materialkolor.PaletteStyle.Fidelity
                            PaletteStyle.Content -> com.materialkolor.PaletteStyle.Content
                        },
                        animate = false
                    ) {
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.primaryContainer,
                        ).fastForEach {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color = it)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val amoledDarkTheme by settings.amoledDarkTheme.collectAsState()
        SettingSwitch(
            title = context.resources.getString(R.string.black_theme),
            supportingText = context.resources.getString(R.string.black_theme_explain),
            icon = Icons.Rounded.Contrast,
            isChecked = amoledDarkTheme,
            onCheckedChange = {
                settings.updateAmoledDarkTheme(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val useDynamicColor by settings.useDynamicColor.collectAsState()
            SettingSwitch(
                title = context.resources.getString(R.string.use_system_key_colors),
                supportingText = context.resources.getString(R.string.use_system_key_colors_explain),
                icon = Icons.Rounded.AutoAwesome,
                isChecked = useDynamicColor,
                onCheckedChange = {
                    settings.updateUseDynamicColor(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        val useAlbumArtColor by settings.useAlbumArtColor.collectAsState()
        SettingSwitch(
            title = context.resources.getString(R.string.use_album_art_color),
            supportingText = context.resources.getString(R.string.use_album_art_color_explain),
            icon = Icons.Rounded.Album,
            isChecked = useAlbumArtColor,
            onCheckedChange = {
                settings.updateUseAlbumArtColor(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        var perTrackArtwork by remember { mutableStateOf(settings.perTrackArtwork) }
        SettingSwitch(
            title = context.resources.getString(R.string.per_track_artwork),
            supportingText = context.resources.getString(R.string.per_track_artwork_explain),
            icon = Icons.Rounded.LibraryMusic,
            isChecked = perTrackArtwork,
            onCheckedChange = {
                settings.perTrackArtwork = it
                perTrackArtwork = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

class AppearanceOption(
    title: String,
    onSelection: () -> Unit,
    val appearance: Appearance,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
) : SettingOption(title, onSelection)

class PaletteOption(
    title: String,
    onSelection: () -> Unit,
    val style: PaletteStyle,
) : SettingOption(title, onSelection)

object Theme {
    enum class Appearance {
        System,
        Light,
        Dark
    }

    enum class PaletteStyle {
        TonalSpot,
        Neutral,
        Vibrant,
        Expressive,
        Rainbow,
        FruitSalad,
        Monochrome,
        Fidelity,
        Content
    }
}
