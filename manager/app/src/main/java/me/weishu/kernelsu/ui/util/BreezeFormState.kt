package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Stable

/**
 * One field of a form, as far as the form is concerned. Declared here rather than beside the
 * implementation so a screen's wiring layer can drive a form without importing the component
 * library.
 */
interface FormField {
    val error: String?
    fun commit()
}

/** Every editable field of one form, so a Save can flush them together and refuse the whole form. */
@Stable
class BreezeFormState {
    private val fields = mutableListOf<FormField>()

    fun register(field: FormField) {
        if (!fields.contains(field)) fields.add(field)
    }

    fun unregister(field: FormField) {
        fields.remove(field)
    }

    /** False when any field is still in error, checked before a single value is committed. */
    fun commitAll(): Boolean {
        if (fields.any { it.error != null }) return false
        fields.forEach { it.commit() }
        return true
    }
}
