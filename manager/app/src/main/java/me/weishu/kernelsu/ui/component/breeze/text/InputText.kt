package me.weishu.kernelsu.ui.component.breeze.text

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import me.weishu.kernelsu.ui.util.BreezeFormState

@Composable
fun rememberOutlinedTextEditState(
    text: String,
    validator: (String) -> String? = { null },
    onValueChange: (String) -> Unit,
): OutlinedTextEditState {
    // The holder outlives recomposition, so it must read the latest lambdas rather than the ones
    // the first composition captured — those close over state that changes every frame.
    val latestValidator by rememberUpdatedState(validator)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val state = remember {
        OutlinedTextEditState(text, { latestOnValueChange(it) }, { latestValidator(it) })
    }
    LaunchedEffect(text) { state.syncExternal(text) }
    return state
}

/**
 * Commits on blur, IME Done, [BreezeFormState.commitAll] and disposal — never per keystroke.
 * [state] also carries the live error so a form can refuse to save before anything is written.
 */
@Composable
fun OutlinedTextEdit(
    label: @Composable () -> Unit,
    state: OutlinedTextEditState,
    modifier: Modifier = Modifier,
    defaultSupportingText: @Composable () -> Unit = {},
    enabled: Boolean = true,
    readOnly: Boolean = false,
    formState: BreezeFormState? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
) {
    if (formState != null) {
        DisposableEffect(formState, state) {
            formState.register(state)
            onDispose { formState.unregister(state) }
        }
    }
    // Leaving a field is the commit: disposal, focus loss and Done all end the same edit.
    DisposableEffect(state) {
        onDispose { state.commit() }
    }

    val callerDone = keyboardActions.onDone
    val wrappedActions = remember(keyboardActions) {
        KeyboardActions(
            onDone = {
                state.commit()
                callerDone?.invoke(this)
            },
            onGo = keyboardActions.onGo,
            onNext = keyboardActions.onNext,
            onPrevious = keyboardActions.onPrevious,
            onSearch = keyboardActions.onSearch,
            onSend = keyboardActions.onSend,
        )
    }

    val isError = !state.error.isNullOrBlank()
    OutlinedTextField(
        value = state.text,
        onValueChange = { if (!readOnly) state.edit(it) },
        modifier = modifier.onFocusChanged { if (!it.isFocused) state.commit() },
        label = label,
        isError = isError,
        supportingText = {
            if (isError) {
                Text(
                    text = state.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                defaultSupportingText()
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = wrappedActions,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines
    )
}
