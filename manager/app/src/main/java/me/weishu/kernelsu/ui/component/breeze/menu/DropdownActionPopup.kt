package me.weishu.kernelsu.ui.component.breeze.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.PopupFeedBack

/** One entry of [DropdownActionPopup]. */
data class DropdownAction(
    val label: String,
    val onClick: () -> Unit,
)

/** Icon button that opens a grouped Material 3 [DropdownMenuPopup]. */
@Composable
fun DropdownActionPopup(
    actions: List<DropdownAction>,
    icon: ImageVector = MaterialSymbols.Filled.Rounded.MoreVert,
    contentDescription: String = "More options",
) {
    var expanded by remember { mutableStateOf(false) }

    if (expanded) PopupFeedBack()

    // Wrap in Box so DropdownMenuPopup's 0x0 anchor probe binds to the icon cell rather than an enclosing Row.
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Anchor to End so trailing overflow menus open inward.
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.End,
            ),
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                actions.forEachIndexed { index, action ->
                    SelectableDropdownMenuItem(
                        selected = false,
                        onClick = {
                            expanded = false
                            action.onClick()
                        },
                        text = { Text("  " + action.label) },
                        shapes = MenuDefaults.itemShape(index = index, count = actions.size),
                    )
                }
            }
        }
    }
}
