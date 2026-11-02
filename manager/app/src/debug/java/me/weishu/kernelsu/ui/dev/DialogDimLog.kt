package me.weishu.kernelsu.ui.dev

import android.util.Log
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Logs the enclosing dialog window's dim attributes after window setup completes.
 */
@Suppress("unused")
@Composable
internal fun LogDialogDim(tag: String) {
    val view = LocalView.current
    val context = LocalContext.current
    val themeDim = remember(context) {
        context.obtainStyledAttributes(intArrayOf(android.R.attr.backgroundDimAmount))
            .use { it.getFloat(0, Float.NaN) }
    }
    LaunchedEffect(tag) {
        repeat(3) { pass ->
            delay(400.milliseconds)
            val window = view.findDialogWindow()
            val attrs = window?.attributes
            val dim = attrs?.dimAmount ?: Float.NaN
            val dimBehind = attrs != null &&
                    (attrs.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0
            Log.i(
                "SheetDim",
                "$tag pass=$pass theme=$themeDim windowDim=$dim " +
                        "flagDimBehind=$dimBehind",
            )
        }
    }
}

/** The window this dialog is drawn in; the provider sits on the DialogLayout, not on LocalView. */
internal fun View.findDialogWindow(): Window? {
    var node: ViewParent? = parent
    while (node != null) {
        if (node is DialogWindowProvider) return node.window
        node = node.parent
    }
    return null
}
