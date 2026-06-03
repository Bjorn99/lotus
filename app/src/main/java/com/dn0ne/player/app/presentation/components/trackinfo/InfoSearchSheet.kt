package com.dn0ne.player.app.presentation.components.trackinfo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.metadata.MetadataSearchResult
import com.dn0ne.player.app.presentation.components.ProviderText

@Composable
fun InfoSearchSheet(
    state: InfoSearchSheetState,
    onBackClick: () -> Unit,
    onSearch: (String) -> Unit,
    onSearchResultClick: (MetadataSearchResult) -> Unit,
    matchDuration: Boolean,
    onMatchDurationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchHint by remember {
        mutableStateOf(false)
    }

    if (showSearchHint) {
        SearchHintDialog(
            onDismiss = {
                showSearchHint = false
            }
        )
    }

    Column(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        SearchBox(
            isLoading = state.isLoading,
            onSearch = onSearch,
            onBackClick = onBackClick,
            onHintClick = {
                showSearchHint = true
            },
            matchDuration = matchDuration,
            onMatchDurationClick = onMatchDurationClick,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(
                items = state.searchResults,
                key = { index, result -> "$index-${result.id}-${result.albumId}" }
            ) { _, result ->
                SearchResultItem(
                    searchResult = result,
                    onClick = onSearchResultClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()

                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.searchResults.isNotEmpty()) {
                item {
                    val context = LocalContext.current
                    ProviderText(
                        providerText = context.resources.getString(R.string.search_results_provided_by),
                        uri = context.resources.getString(R.string.musicbrainz_uri),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun SearchBox(
    isLoading: Boolean,
    onSearch: (String) -> Unit,
    onBackClick: () -> Unit,
    onHintClick: () -> Unit,
    matchDuration: Boolean,
    onMatchDurationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackClick
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBackIosNew,
                contentDescription = context.resources.getString(R.string.back_to_track_info)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(ShapeDefaults.ExtraLarge)
        ) {
            SearchField(
                onSearch = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
            )

            this@Row.AnimatedVisibility(
                visible = isLoading,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box {
            var showDropdownMenu by remember {
                mutableStateOf(false)
            }
            IconButton(
                onClick = { showDropdownMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = context.resources.getString(R.string.search_menu)
                )
            }

            DropdownMenu(
                expanded = showDropdownMenu,
                onDismissRequest = {
                    showDropdownMenu = false
                },
                shape = ShapeDefaults.Medium
            ) {
                DropdownMenuItem(
                    text = {
                        Text(text = context.resources.getString(R.string.match_duration))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (matchDuration) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)
                        )
                    },
                    onClick = {
                        onMatchDurationClick()
                        showDropdownMenu = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(text = context.resources.getString(R.string.search_tips))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onHintClick()
                        showDropdownMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun SearchField(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(ShapeDefaults.ExtraLarge)
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        val focusManager = LocalFocusManager.current
        var value by rememberSaveable {
            mutableStateOf("")
        }
        Box {
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it.trimStart()
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (value.isNotBlank()) {
                            focusManager.clearFocus()
                            onSearch(value.trim())
                        }
                    }
                ),

                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )

            val context = LocalContext.current
            value.ifEmpty {
                Text(
                    text = context.resources.getString(R.string.search),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(alignment = Alignment.CenterStart)
                )
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector = Icons.Rounded.Search,
    placeholder: String = stringResource(R.string.search),
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(ShapeDefaults.ExtraLarge)
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )

            value.ifEmpty {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(alignment = Alignment.CenterStart)
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    searchResult: MetadataSearchResult,
    onClick: (MetadataSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(ShapeDefaults.Medium)
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                onClick(searchResult)
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = searchResult.title,
            style = MaterialTheme.typography.headlineMedium
        )

        val context = LocalContext.current
        searchResult.description?.let { description ->
            TagRow(
                tag = context.resources.getString(R.string.description),
                value = description,
                enableCopy = false
            )
        }

        TagRow(
            tag = context.resources.getString(R.string.album),
            value = searchResult.album,
            enableCopy = false
        )

        searchResult.albumDescription?.let { description ->
            TagRow(
                tag = context.resources.getString(R.string.album_description),
                value = description,
                enableCopy = false
            )
        }

        TagRow(
            tag = context.resources.getString(R.string.artist),
            value = searchResult.artist,
            enableCopy = false
        )

        TagRow(
            tag = context.resources.getString(R.string.album_artist),
            value = searchResult.albumArtist,
            enableCopy = false
        )

        searchResult.trackNumber?.let { trackNumber ->
            TagRow(
                tag = context.resources.getString(R.string.track_number),
                value = trackNumber,
                enableCopy = false
            )
        }

        searchResult.year?.let { year ->
            TagRow(
                tag = context.resources.getString(R.string.year),
                value = year,
                enableCopy = false
            )
        }

        searchResult.genres?.let { genres ->
            TagRow(
                tag = context.resources.getString(R.string.genre),
                value = genres.joinToString(" / "),
                enableCopy = false
            )
        }
    }
}

@Composable
fun SearchHintDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = context.resources.getString(R.string.got_it))
            }
        },
        title = {
            Text(text = context.resources.getString(R.string.search_tips))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(.7f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = context.resources.getString(R.string.search_explain))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.resources.getString(R.string.quotes_explain_header),
                    fontWeight = FontWeight.Bold
                )

                Text(text = context.resources.getString(R.string.quotes_explain))

                Text(
                    text = context.resources.getString(R.string.quotes_example_1),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(ShapeDefaults.Small)
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp)
                )

                Text(
                    text = context.resources.getString(R.string.quotes_example_2),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(ShapeDefaults.Small)
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.resources.getString(R.string.field_explain_header),
                    fontWeight = FontWeight.Bold
                )

                Text(text = context.resources.getString(R.string.field_explain))

                Text(
                    text = context.resources.getString(R.string.field_example_1),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(ShapeDefaults.Small)
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp)
                )

                Text(
                    text = context.resources.getString(R.string.field_example_2),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(ShapeDefaults.Small)
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.resources.getString(R.string.combine_explain_header),
                    fontWeight = FontWeight.Bold
                )

                Text(text = context.resources.getString(R.string.combine_explain))

                Text(
                    text = context.resources.getString(R.string.combine_example),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(ShapeDefaults.Small)
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.resources.getString(R.string.mbid_lookup_header),
                    fontWeight = FontWeight.Bold
                )

                Text(text = context.resources.getString(R.string.mbid_lookup_explain))

                Text(
                    text = context.resources.getString(R.string.mbid_example),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(ShapeDefaults.Small)
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.resources.getString(R.string.duration_toggle_header),
                    fontWeight = FontWeight.Bold
                )

                Text(text = context.resources.getString(R.string.duration_toggle_explain))
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = null
            )
        },
        modifier = modifier
    )
}