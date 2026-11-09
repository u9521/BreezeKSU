package me.weishu.kernelsu.ui.component.breeze.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.weishu.kernelsu.ui.util.LocalPaneSlot
import me.weishu.kernelsu.ui.util.PaneSlot

/**
 * Back button for a page that may share the screen with a second column. It renders only outside
 * the end column: there the page is covered by the entry that pushed it, so its own app bar must
 * not offer a competing way out.
 */
@Composable
fun PaneBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapseFraction: Float = 0f,
    contentDescription: String? = null,
) {
    if (LocalPaneSlot.current == PaneSlot.End) return
    BreezeBackButton(
        onClick = onClick,
        modifier = modifier,
        collapseFraction = collapseFraction,
        contentDescription = contentDescription,
    )
}
