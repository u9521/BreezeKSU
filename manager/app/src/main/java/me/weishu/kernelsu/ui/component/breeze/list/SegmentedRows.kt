package me.weishu.kernelsu.ui.component.breeze.list

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.component.breeze.expressive.ExpressiveSwitch
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.PopupFeedBack
import me.weishu.kernelsu.ui.util.switchHapticFeedBack

private val RowMinHeight = 72.dp

/** Reads shapes from [SegmentedListGroup], falling back to a single standalone row. */
@Composable
private fun segmentedRowShapes(): ListItemShapes =
    LocalSegmentedItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)

@Composable
private fun segmentedRowColors(): ListItemColors =
    ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceBright)

private fun Modifier.segmentedRowLayout(): Modifier =
    this.defaultMinSize(minHeight = RowMinHeight)

/** A row that opens another screen. Trailing defaults to a chevron. */
@Composable
fun SegmentedNavItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = segmentedRowShapes(),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.segmentedRowLayout(),
        enabled = enabled,
        colors = segmentedRowColors(),
        leadingContent = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        supportingContent = summary?.let { text -> { Text(text) } },
        trailingContent = trailing,
    ) {
        Text(title)
    }
}

/** A row with a switch; tapping anywhere on the row toggles it. */
@Composable
fun SegmentedSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val haptic = switchHapticFeedBack()
    val interactionSource = remember { MutableInteractionSource() }
    val toggle = {
        haptic(!checked)
        onCheckedChange(!checked)
    }
    SegmentedListItem(
        onClick = toggle,
        shapes = segmentedRowShapes(),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.segmentedRowLayout(),
        enabled = enabled,
        colors = segmentedRowColors(),
        interactionSource = interactionSource,
        leadingContent = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        supportingContent = summary?.let { text -> { Text(text) } },
        trailingContent = {
            ExpressiveSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { onCheckedChange(it) },
            )
        },
    ) {
        Text(title)
    }
}

/** A row with a checkbox; the checkbox itself is not independently clickable. */
@Composable
fun SegmentedCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val haptic = switchHapticFeedBack()
    SegmentedListItem(
        onClick = { haptic(!checked); onCheckedChange(!checked) },
        shapes = segmentedRowShapes(),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.segmentedRowLayout(),
        enabled = enabled,
        colors = segmentedRowColors(),
        leadingContent = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                modifier = Modifier.size(24.dp),
            )
        },
        supportingContent = summary?.let { text -> { Text(text) } },
    ) {
        Text(title)
    }
}

/** A row that selects one option out of its group. */
@Composable
fun SegmentedRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val haptic = switchHapticFeedBack()
    SegmentedListItem(
        selected = selected,
        onClick = { haptic(true); onClick() },
        shapes = segmentedRowShapes(),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.segmentedRowLayout(),
        enabled = enabled,
        colors = segmentedRowColors(),
        leadingContent = {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
        },
        supportingContent = summary?.let { text -> { Text(text) } },
    ) {
        Text(title)
    }
}

/** A row whose value is picked from [items]; the current value sits at the trailing edge. */
@Composable
fun SegmentedDropdownItem(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val safeIndex = if (items.isEmpty()) -1 else selectedIndex.coerceIn(0, items.lastIndex)
    SegmentedMenuRow(
        title = title,
        modifier = modifier,
        summary = summary,
        leadingIcon = leadingIcon,
        enabled = enabled,
        selectedText = if (safeIndex >= 0) items[safeIndex] else null,
        menuContent = { dismissMenu ->
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                items.forEachIndexed { index, label ->
                    SelectableDropdownMenuItem(
                        selected = index == safeIndex,
                        onClick = {
                            if (index in items.indices) onItemSelected(index)
                            dismissMenu()
                        },
                        text = { Text(label) },
                        shapes = MenuDefaults.itemShape(index, items.size),
                        selectedLeadingIcon = {
                            Icon(
                                MaterialSymbols.Filled.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            )
                        },
                    )
                }
            }
        },
    )
}

/** Row that opens a popup menu anchored at the press coordinates. */
@Composable
fun SegmentedMenuRow(
    title: String,
    menuContent: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    selectedText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var pressPoint by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val dismissMenu = { expanded = false }
    val openAt = { point: Offset ->
        pressPoint = point
        expanded = true
    }
    Box(
        modifier = modifier
            .clip(segmentedRowShapes().shape)
            .indication(interactionSource, LocalIndication.current)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
                onClick {
                    openAt(Offset.Zero)
                    true
                }
            }
            .onKeyEvent { event ->
                if (!enabled) return@onKeyEvent false
                val isEnter = event.key == Key.Enter ||
                        event.key == Key.NumPadEnter ||
                        event.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER
                if (event.type == KeyEventType.KeyUp && isEnter) {
                    openAt(Offset.Zero)
                    true
                } else {
                    false
                }
            },
    ) {
        // Apply minHeight to SegmentedListItem rather than the outer Box so content centers vertically.
        SegmentedListItem(
            modifier = Modifier.segmentedRowLayout(),
            shapes = segmentedRowShapes(),
            verticalAlignment = Alignment.CenterVertically,
            enabled = enabled,
            colors = segmentedRowColors(),
            leadingContent = leadingContent
                ?: leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
            supportingContent = supportingContent ?: summary?.let { text -> { Text(text) } },
            trailingContent = trailingContent ?: selectedText?.let { value ->
                {
                    Text(
                        text = value,
                        textAlign = TextAlign.End,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
        ) {
            Text(title)
        }
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { point ->
                                val press = PressInteraction.Press(point)
                                interactionSource.emit(press)
                                val released = tryAwaitRelease()
                                if (released) {
                                    interactionSource.emit(PressInteraction.Release(press))
                                } else {
                                    interactionSource.emit(PressInteraction.Cancel(press))
                                }
                            },
                            onTap = { point -> openAt(point) },
                        )
                    },
            )
        }
        Box(modifier = Modifier.offset { IntOffset(pressPoint.x.toInt(), pressPoint.y.toInt()) }) {
            DropdownMenuPopup(expanded = expanded, onDismissRequest = dismissMenu) {
                PopupFeedBack()
                menuContent(dismissMenu)
            }
        }
    }
}

/** Inline text-field row that focuses on row tap and scrolls itself into view when focused. */
@Composable
fun SegmentedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholder: String? = null,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val colors = segmentedRowColors()
    SegmentedListItem(
        onClick = { focusRequester.requestFocus() },
        shapes = segmentedRowShapes(),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .segmentedRowLayout()
            .bringIntoViewRequester(bringIntoView)
            .focusRequester(focusRequester),
        enabled = enabled,
        colors = colors,
        supportingContent = supportingText?.let { text ->
            {
                Text(
                    text = text,
                    color = if (isError) MaterialTheme.colorScheme.error else colors.supportingContentColor,
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = if (isError) MaterialTheme.colorScheme.error else colors.contentColor,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) scope.launch { bringIntoView.bringIntoView() }
                    },
                enabled = enabled,
                readOnly = readOnly,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.supportingContentColor,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder != null) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.supportingContentColor,
                            )
                        }
                    }
                    innerTextField()
                },
            )
        }
    }
}

/** Slider row displaying the formatted current value above the track. */
@Composable
fun SegmentedSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueText: (Float) -> String = { it.toString() },
    onValueChangeFinished: () -> Unit = {},
    enabled: Boolean = true,
) {
    val colors = segmentedRowColors()
    // Key on steps/valueRange only: a value-keyed slider state would be rebuilt on every drag
    // frame and drop the gesture as soon as the external value comes back.
    val sliderState = remember(steps, valueRange) {
        SliderState(value = value, steps = steps, trackRange = valueRange)
    }
    LaunchedEffect(value) {
        if (sliderState.value != value) sliderState.value = value
    }
    SegmentedListItem(
        shapes = segmentedRowShapes(),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.segmentedRowLayout(),
        enabled = enabled,
        colors = colors,
        supportingContent = supportingText?.let { text -> { Text(text) } },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, modifier = Modifier.weight(1f))
                Text(
                    text = valueText(sliderState.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.supportingContentColor,
                )
            }
            Slider(
                state = sliderState,
                onValueChange = { onValueChange(it) },
                onValueChangeFinished = onValueChangeFinished,
                enabled = enabled,
            )
        }
    }
}