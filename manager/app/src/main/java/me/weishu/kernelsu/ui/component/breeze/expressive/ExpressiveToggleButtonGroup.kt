package me.weishu.kernelsu.ui.component.breeze.expressive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/** Connected single-select toggle buttons with leading/middle/trailing shapes and [Role.RadioButton] semantics. */
@Composable
fun ExpressiveSingleSelectToggleButtonGroup(
    options: List<@Composable () -> Unit>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConnectedToggleButtons(
        count = options.size,
        modifier = modifier,
    ) { index, shapes ->
        ExpressiveToggleButton(
            checked = index == selectedIndex,
            onCheckedChange = { onSelect(index) },
            shapes = shapes,
            modifier = Modifier.semantics { role = Role.RadioButton },
            content = { options[index]() },
        )
    }
}

/** Connected multi-select toggle buttons with leading/middle/trailing shapes. */
@Composable
fun ExpressiveMultiSelectToggleButtonGroup(
    options: List<@Composable () -> Unit>,
    checkedIndices: Set<Int>,
    onToggle: (index: Int, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConnectedToggleButtons(
        count = options.size,
        modifier = modifier,
    ) { index, shapes ->
        ExpressiveToggleButton(
            checked = index in checkedIndices,
            onCheckedChange = { onToggle(index, it) },
            shapes = shapes,
            content = { options[index]() },
        )
    }
}

/** Lays out [count] connected toggle buttons in a wrapping [FlowRow]. */
@Composable
private fun ConnectedToggleButtons(
    count: Int,
    modifier: Modifier = Modifier,
    button: @Composable (index: Int, shapes: ToggleButtonShapes) -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        for (index in 0 until count) {
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            button(index, shapes)
        }
    }
}
