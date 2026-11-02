package me.weishu.kernelsu.ui.component.breeze.button

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.LocalIsDetailPane
import me.weishu.kernelsu.ui.util.LocalSplitRatio
import me.weishu.kernelsu.ui.util.PopupFeedBack

/**
 * Split-ratio button bound to [LocalSplitRatio]; renders only inside a detail pane of a
 * list-detail scene and keeps the list half at least [MinPaneWidthDp] wide.
 */
@Composable
fun SplitScreenRatioButton() {
    val ratioState = LocalSplitRatio.current ?: return
    if (!LocalIsDetailPane.current) return

    val screenWidthDp = LocalWindowInfo.current.containerDpSize.width
    val minRatio = if (screenWidthDp.value > 0f) {
        (MinPaneWidthDp / screenWidthDp).coerceIn(0.1f, 0.5f)
    } else {
        0.1f
    }

    SplitScreenRatioButton(
        ratio = ratioState.floatValue,
        onRatioChange = { ratioState.floatValue = it },
        minRatio = minRatio,
        maxRatio = 1f - minRatio,
    )
}

/** Ratio slider button with explicit bounds; the caller owns the ratio value. */
@Composable
fun SplitScreenRatioButton(
    ratio: Float,
    onRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minRatio: Float = 0.1f,
    maxRatio: Float = 0.9f,
) {
    var expanded by remember { mutableStateOf(false) }

    // Key on the range only: a value-keyed slider state would be rebuilt on every drag frame
    // and drop the gesture as soon as the external value comes back.
    val sliderState = remember(minRatio, maxRatio) {
        SliderState(value = ratio, steps = 0, trackRange = minRatio..maxRatio)
    }
    LaunchedEffect(ratio) {
        if (sliderState.value != ratio) sliderState.value = ratio
    }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = MaterialSymbols.Rounded.SplitscreenRight,
                contentDescription = "Adjust split ratio",
            )
        }

        DropdownMenuPopup(
            expanded = expanded,
            modifier = Modifier.padding(end = 16.dp),
            onDismissRequest = { expanded = false },
        ) {
            PopupFeedBack()
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                Slider(
                    state = sliderState,
                    modifier = Modifier
                        .width(200.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    enabled = true,
                    onValueChange = { newValue ->
                        sliderState.value = newValue
                        onRatioChange(newValue)
                    },
                )
            }
        }
    }
}

/** Narrowest list pane the split ratio keeps visible, in dp. */
private val MinPaneWidthDp = 250.dp
