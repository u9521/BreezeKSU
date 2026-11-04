package me.weishu.kernelsu.ui.component.breeze.text

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.weishu.kernelsu.ui.util.FormField

/**
 * A field's draft next to the value last handed to `onValueChange`, committed on blur, IME Done,
 * form save or disposal rather than per keystroke. Committing per keystroke wrote half-typed values
 * out, and some of those prefixes were valid contexts in their own right.
 */
@Stable
class OutlinedTextEditState internal constructor(
    initialText: String,
    private val onValueChange: (String) -> Unit,
    private val validate: (String) -> String?,
) : FormField {
    var text by mutableStateOf(initialText)
        private set

    override var error by mutableStateOf<String?>(null)
        private set

    private var committed = initialText

    /** Re-validates on every keystroke; only the value waits for a commit. */
    fun edit(value: String) {
        text = value
        error = validate(value)
    }

    /** A no-op while the draft is invalid or unchanged, so only a value worth writing is written. */
    override fun commit() {
        if (error != null || text == committed) return
        committed = text
        onValueChange(text)
    }

    /**
     * Adopts a value set from outside. Compared against the last commit rather than the draft, so a
     * genuinely external change wins over an edit in progress instead of being swallowed by it.
     */
    internal fun syncExternal(value: String) {
        if (value == committed) return
        committed = value
        text = value
        error = validate(value)
    }
}
