package me.weishu.kernelsu.ui.component.breeze.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/** Content and actions of [ConfirmDialogBreeze], controlled via [ConfirmDialogState.setConfirm]. */
@Stable
data class ConfirmBody(
    val title: String,
    val body: Body = Body.Text(""),
    val confirm: String? = null,
    val dismiss: String? = null,
    /** While true, a spinner replaces [body] and the confirm button is disabled. */
    val loading: Boolean = false,
    val confirmEnabled: Boolean = true,
    val onConfirm: () -> Unit = {},
    /** Dismiss handler, or null to omit the dismiss button (back and outside taps still dismiss). */
    val onDismiss: (() -> Unit)? = null,
) {
    sealed interface Body {
        /** Plain text, shown as-is. */
        data class Text(val text: String) : Body

        /** GitHub-flavoured markdown. */
        data class Markdown(val markdown: String) : Body

        /** Empty state: a muted icon, a message, and at most one follow-up action. */
        data class Empty(
            val icon: ImageVector,
            val message: String,
            val action: Action? = null,
        ) : Body {
            data class Action(val label: String, val onClick: () -> Unit)
        }

        /** Arbitrary composable body for layouts outside the standard shapes. */
        class Custom(val content: @Composable () -> Unit) : Body
    }
}

/** Holds the current [ConfirmBody]; pass `null` to [setConfirm] to close the dialog. */
@Stable
class ConfirmDialogState internal constructor() {
    var body: ConfirmBody? by mutableStateOf(null)
        internal set

    fun setConfirm(body: ConfirmBody?) {
        this.body = body
    }
}

/** Remembers a [ConfirmDialogState] and renders it with the Breeze dialog. */
@Composable
fun rememberConfirmDialog(): ConfirmDialogState {
    val state = remember { ConfirmDialogState() }
    val body = state.body
    ConfirmDialogBreeze(
        body = body,
        onConfirm = {
            val current = body ?: return@ConfirmDialogBreeze
            state.setConfirm(null)
            current.onConfirm()
        },
        onDismiss = {
            val current = body ?: return@ConfirmDialogBreeze
            state.setConfirm(null)
            current.onDismiss?.invoke()
        },
    )
    return state
}

/** Visibility state of the non-dismissible spinner dialog. */
@Stable
class LoadingDialogState internal constructor() {
    var visible: Boolean by mutableStateOf(false)
        private set

    fun show() {
        visible = true
    }

    fun hide() {
        visible = false
    }

    /** Runs [block] with the dialog up, closing it whatever the block does. */
    @Suppress("unused")
    suspend fun <R> withLoading(block: suspend () -> R): R {
        visible = true
        return try {
            block()
        } finally {
            visible = false
        }
    }
}

@Composable
fun rememberLoadingDialog(): LoadingDialogState {
    val state = remember { LoadingDialogState() }
    LoadingDialogBreeze(state.visible)
    return state
}
