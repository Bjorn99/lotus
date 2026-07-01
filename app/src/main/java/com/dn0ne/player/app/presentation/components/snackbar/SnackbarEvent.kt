package com.dn0ne.player.app.presentation.components.snackbar

import androidx.annotation.StringRes

data class SnackbarEvent(
    @StringRes val message: Int,
    val action: SnackbarAction? = null,
    val formatArgs: List<Any> = emptyList()
)