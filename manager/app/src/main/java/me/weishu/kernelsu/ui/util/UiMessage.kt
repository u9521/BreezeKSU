package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Immutable

/**
 * One-shot message shown by a host scaffold; [id] changes per message so the same text can repeat.
 * [onAction] runs only when the user taps [actionLabel].
 */
@Immutable
data class UiMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)
