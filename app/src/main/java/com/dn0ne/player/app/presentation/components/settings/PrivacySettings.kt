package com.dn0ne.player.app.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.NoteCard
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.data.Settings

@Composable
fun PrivacySettings(
    settings: Settings,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var collapseFraction by remember { mutableFloatStateOf(0f) }

    ColumnWithCollapsibleTopBar(
        topBarContent = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = context.resources.getString(R.string.back),
                )
            }

            Text(
                text = context.resources.getString(R.string.privacy),
                fontSize = lerp(
                    MaterialTheme.typography.titleLarge.fontSize,
                    MaterialTheme.typography.displaySmall.fontSize,
                    collapseFraction,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
            )
        },
        collapseFraction = { collapseFraction = it },
        contentPadding = PaddingValues(horizontal = 28.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        var networkLookupsEnabled by remember {
            mutableStateOf(settings.networkLookupsEnabled)
        }
        SettingSwitch(
            title = context.resources.getString(R.string.network_lookups),
            supportingText = context.resources.getString(R.string.network_lookups_explain),
            icon = Icons.Rounded.CloudOff,
            isChecked = networkLookupsEnabled,
            onCheckedChange = {
                settings.networkLookupsEnabled = it
                networkLookupsEnabled = it
            },
            modifier = Modifier.fillMaxWidth(),
        )

        val trackPlayStats by settings.trackPlayStats.collectAsState()
        SettingSwitch(
            title = context.resources.getString(R.string.track_play_stats),
            supportingText = context.resources.getString(R.string.track_play_stats_explain),
            icon = Icons.Rounded.Insights,
            isChecked = trackPlayStats,
            onCheckedChange = settings::updateTrackPlayStats,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        SectionLabel(text = context.resources.getString(R.string.privacy_what_leaves))

        NoteCard(
            label = context.resources.getString(R.string.privacy_lrclib_title),
            leadingIcon = Icons.Rounded.Cloud,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = context.resources.getString(R.string.privacy_lrclib_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        NoteCard(
            label = context.resources.getString(R.string.privacy_netease_title),
            leadingIcon = Icons.Rounded.Cloud,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = context.resources.getString(R.string.privacy_netease_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        NoteCard(
            label = context.resources.getString(R.string.privacy_musicbrainz_title),
            leadingIcon = Icons.Rounded.Public,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = context.resources.getString(R.string.privacy_musicbrainz_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionLabel(text = context.resources.getString(R.string.privacy_what_stays))

        NoteCard(
            label = context.resources.getString(R.string.privacy_local_title),
            leadingIcon = Icons.Rounded.Folder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = context.resources.getString(R.string.privacy_local_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        NoteCard(
            label = context.resources.getString(R.string.privacy_no_telemetry_title),
            leadingIcon = Icons.Rounded.Shield,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = context.resources.getString(R.string.privacy_no_telemetry_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
    )
}
