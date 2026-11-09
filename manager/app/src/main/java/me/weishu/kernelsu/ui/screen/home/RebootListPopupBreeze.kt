package me.weishu.kernelsu.ui.screen.home

import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.icons.MaterialSymbols

@Composable
fun RebootDropdownItems(onItemClick: (String) -> Unit) {
    val options = getRebootListOption()
    options.forEachIndexed { index, option ->
        SelectableDropdownMenuItem(
            selected = false,
            onClick = { onItemClick(option.reason) },
            text = { Text("  " + stringResource(option.labelRes)) },
            shapes = MenuDefaults.itemShape(index = index, count = options.size),
        )
    }
}

@Composable
fun RebootListPopupBreeze() {
    var expanded by remember { mutableStateOf(false) }

    KsuIsValid {
        val onReboot = rememberRebootAction()

        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = MaterialSymbols.Filled.Rounded.RestartAlt,
                contentDescription = stringResource(id = R.string.reboot)
            )
        }

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                RebootDropdownItems { reason ->
                    expanded = false
                    onReboot(reason)
                }
            }
        }
    }
}
