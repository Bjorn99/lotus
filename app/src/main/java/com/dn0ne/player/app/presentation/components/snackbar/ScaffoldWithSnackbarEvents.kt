package com.dn0ne.player.app.presentation.components.snackbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun ScaffoldWithSnackbarEvents(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(flow = SnackbarController.events) { event ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()

            val result = snackbarHostState.showSnackbar(
                message = if (event.formatArgs.isNotEmpty()) {
                    context.resources.getString(event.message, *event.formatArgs.toTypedArray())
                } else {
                    context.resources.getString(event.message)
                },
                actionLabel = event.action?.let { context.resources.getString(it.name) }
            )

            if (result == SnackbarResult.ActionPerformed) {
                event.action?.action?.invoke()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
    ) {
        content(it)
    }
}