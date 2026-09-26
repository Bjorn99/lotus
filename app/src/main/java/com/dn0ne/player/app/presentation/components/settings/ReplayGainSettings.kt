package com.dn0ne.player.app.presentation.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.replaygain.FALLBACK_RANGE_DB
import com.dn0ne.player.app.domain.replaygain.PREAMP_RANGE_DB
import com.dn0ne.player.app.domain.replaygain.ReplayGainMode
import com.dn0ne.player.core.data.Settings
import java.util.Locale

@Composable
fun ReplayGainSettings(
    settings: Settings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mode by settings.replayGainMode.collectAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = null,
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = context.resources.getString(R.string.replaygain),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = context.resources.getString(R.string.replaygain_explain),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val labels = listOf(
            ReplayGainMode.OFF to R.string.replaygain_off,
            ReplayGainMode.TRACK to R.string.replaygain_track,
            ReplayGainMode.ALBUM to R.string.replaygain_album,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = mode == option,
                    onClick = { settings.updateReplayGainMode(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                ) {
                    Text(text = context.resources.getString(label))
                }
            }
        }

        AnimatedVisibility(visible = mode != ReplayGainMode.OFF) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DbSlider(
                    title = context.resources.getString(R.string.replaygain_preamp),
                    supportingText = context.resources.getString(R.string.replaygain_preamp_explain),
                    stored = settings.replayGainPreAmpDb.collectAsState().value,
                    range = PREAMP_RANGE_DB,
                    onCommit = settings::updateReplayGainPreAmpDb,
                )
                DbSlider(
                    title = context.resources.getString(R.string.replaygain_fallback),
                    supportingText = context.resources.getString(R.string.replaygain_fallback_explain),
                    stored = settings.replayGainFallbackDb.collectAsState().value,
                    range = FALLBACK_RANGE_DB,
                    onCommit = settings::updateReplayGainFallbackDb,
                )
            }
        }
    }
}

/** 0.5 dB steps. Commits on release, so the player ramps once per gesture. */
@Composable
private fun DbSlider(
    title: String,
    supportingText: String,
    stored: Float,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Float) -> Unit,
) {
    var value by remember(stored) { mutableFloatStateOf(stored) }
    Column {
        SettingSlider(
            title = title,
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onCommit(value) },
            valueToShow = String.format(Locale.ROOT, "%+.1f dB", value),
            steps = ((range.endInclusive - range.start) / 0.5f).toInt() - 1,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
