package com.dn0ne.player.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.sort.PlaylistSort
import com.dn0ne.player.app.domain.sort.SortOrder
import com.dn0ne.player.app.domain.sort.TrackSort

@Composable
fun TrackSortButton(
    sort: TrackSort,
    order: SortOrder,
    onSortChange: (TrackSort) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Box {
        var isMenuExpanded by remember {
            mutableStateOf(false)
        }

        val context = LocalContext.current
        IconButton(
            onClick = {
                isMenuExpanded = true
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = context.resources.getString(R.string.sort_order)
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = {
                isMenuExpanded = false
            },
            shape = ShapeDefaults.Medium,
            modifier = Modifier
                .width(IntrinsicSize.Min)
        ) {
            SortButton(
                isSelected = sort == TrackSort.Title,
                text = context.resources.getString(R.string.title),
                onClick = {
                    onSortChange(TrackSort.Title)
                }
            )

            SortButton(
                isSelected = sort == TrackSort.Album,
                text = context.resources.getString(R.string.album),
                onClick = {
                    onSortChange(TrackSort.Album)
                }
            )

            SortButton(
                isSelected = sort == TrackSort.Artist,
                text = context.resources.getString(R.string.artist),
                onClick = {
                    onSortChange(TrackSort.Artist)
                }
            )

            SortButton(
                isSelected = sort == TrackSort.Genre,
                text = context.resources.getString(R.string.genre),
                onClick = {
                    onSortChange(TrackSort.Genre)
                }
            )

            SortButton(
                isSelected = sort == TrackSort.Year,
                text = context.resources.getString(R.string.year),
                onClick = {
                    onSortChange(TrackSort.Year)
                }
            )

            SortButton(
                isSelected = sort == TrackSort.TrackNumber,
                text = context.resources.getString(R.string.track_number),
                onClick = {
                    onSortChange(TrackSort.TrackNumber)
                }
            )

            SortButton(
                isSelected = sort == TrackSort.DateModified,
                text = context.resources.getString(R.string.date_modified),
                onClick = {
                    onSortChange(TrackSort.DateModified)
                }
            )

            SortOrderButtonsRow(
                order = order,
                onClick = onSortOrderChange
            )
        }
    }
}

@Composable
fun PlaylistSortButton(
    sort: PlaylistSort,
    order: SortOrder,
    onSortChange: (PlaylistSort) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Box {
        var isMenuExpanded by remember {
            mutableStateOf(false)
        }

        val context = LocalContext.current
        IconButton(
            onClick = {
                isMenuExpanded = true
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = context.resources.getString(R.string.sort_order)
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = {
                isMenuExpanded = false
            },
            shape = ShapeDefaults.Medium,
            modifier = Modifier
                .width(IntrinsicSize.Min)
        ) {
            SortButton(
                isSelected = sort == PlaylistSort.Title,
                text = context.resources.getString(R.string.title),
                onClick = {
                    onSortChange(PlaylistSort.Title)
                }
            )

            SortButton(
                isSelected = sort == PlaylistSort.Artist,
                text = context.resources.getString(R.string.artist),
                onClick = {
                    onSortChange(PlaylistSort.Artist)
                }
            )

            SortButton(
                isSelected = sort == PlaylistSort.Year,
                text = context.resources.getString(R.string.year),
                onClick = {
                    onSortChange(PlaylistSort.Year)
                }
            )

            SortButton(
                isSelected = sort == PlaylistSort.TrackCount,
                text = context.resources.getString(R.string.track_count),
                onClick = {
                    onSortChange(PlaylistSort.TrackCount)
                }
            )

            SortOrderButtonsRow(
                order = order,
                onClick = onSortOrderChange
            )
        }
    }
}

@Composable
fun SortButton(
    isSelected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(ShapeDefaults.Small)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else Color.Transparent
            )
            .clickable {
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SortOrderButtonsRow(
    order: SortOrder,
    onClick: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val context = LocalContext.current
        SortOrderButton(
            isSelected = order == SortOrder.Ascending,
            imageVector = Icons.Rounded.ArrowUpward,
            contentDescription = context.resources.getString(R.string.sort_ascending),
            onClick = {
                onClick(SortOrder.Ascending)
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        SortOrderButton(
            isSelected = order == SortOrder.Descending,
            imageVector = Icons.Rounded.ArrowDownward,
            contentDescription = context.resources.getString(R.string.sort_descending),
            onClick = {
                onClick(SortOrder.Descending)
            }
        )
    }
}

@Composable
fun SortOrderButton(
    isSelected: Boolean,
    imageVector: ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else Color.Transparent
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}