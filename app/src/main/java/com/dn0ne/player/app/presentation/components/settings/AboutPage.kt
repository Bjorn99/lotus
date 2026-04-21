package com.dn0ne.player.app.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.content.FileProvider
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.crash.CrashReporter
import com.dn0ne.player.core.presentation.AppDetails

@Composable
fun AboutPage(
    onBackClick: () -> Unit,
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
                text = context.resources.getString(R.string.about_app),
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
        contentPadding = PaddingValues(horizontal = 24.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        AppDetails(
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        val uriHandler = LocalUriHandler.current
        val repoIcon = ImageVector.vectorResource(R.drawable.source_control)
        SettingsGroup(
            items = listOf(
                SettingsItem(
                    title = context.resources.getString(R.string.repo),
                    supportingText = context.resources.getString(R.string.repo_explain),
                    icon = repoIcon,
                    onClick = {
                        uriHandler.openUri(context.resources.getString(R.string.repo_url))
                    }
                ),
                SettingsItem(
                    title = context.resources.getString(R.string.feedback),
                    supportingText = context.resources.getString(R.string.feedback_explain),
                    icon = Icons.Rounded.QuestionAnswer,
                    onClick = {
                        uriHandler.openUri(context.resources.getString(R.string.feedback_url))
                    }
                ),
                SettingsItem(
                    title = context.resources.getString(R.string.share_crash_log),
                    supportingText = context.resources.getString(R.string.share_crash_log_explain),
                    icon = Icons.Rounded.BugReport,
                    onClick = {
                        val log = CrashReporter.latestLog(context)
                        if (log == null) {
                            Toast.makeText(
                                context,
                                R.string.no_crash_log_available,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val authority = "${context.packageName}.crashlogs"
                            val uri = FileProvider.getUriForFile(context, authority, log)
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    context.resources.getString(R.string.share_crash_log_subject)
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    share,
                                    context.resources.getString(R.string.share_crash_log)
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        }
                    }
                )
            )
        )
    }
}