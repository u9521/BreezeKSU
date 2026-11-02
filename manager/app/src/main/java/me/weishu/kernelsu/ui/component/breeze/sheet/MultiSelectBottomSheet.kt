package me.weishu.kernelsu.ui.component.breeze.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.breeze.overlay.BreezeOverlaySheet
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.PopupFeedBack
import me.weishu.kernelsu.ui.util.keyDownFeedBack

/** Searchable multi-select bottom sheet with select-all on filtered items, [maxSelection] cap, and [readOnly] mode. */
@Composable
fun <T> MultiSelectBottomSheet(
    title: String,
    options: List<T>,
    initialSelection: List<T>,
    optionTitle: (T) -> String,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    maxSelection: Int = Int.MAX_VALUE,
    readOnly: Boolean = false,
    scrollToItem: T? = null,
    optionSubtitle: (T) -> String? = { null },
    searchLabel: String = "Search",
    selectAllLabel: String = "Select all",
    deselectAllLabel: String = "Deselect all",
    emptyLabel: String = "No results",
    closeLabel: String = "Close",
) {
    val kdFeedback = keyDownFeedBack()
    val selectedItems = remember { initialSelection.toMutableStateList() }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredList = remember(options, searchQuery) {
        if (searchQuery.isBlank()) {
            options
        } else {
            options.filter {
                optionTitle(it).contains(searchQuery, ignoreCase = true) ||
                        (optionSubtitle(it)?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    val isLimitEnabled = maxSelection < Int.MAX_VALUE
    val isAllFilteredSelected = remember(selectedItems.size, filteredList) {
        filteredList.isNotEmpty() && selectedItems.containsAll(filteredList)
    }

    LaunchedEffect(scrollToItem) {
        if (scrollToItem != null && searchQuery.isEmpty()) {
            val index = options.indexOf(scrollToItem)
            if (index >= 0) listState.scrollToItem(index)
        }
    }

    PopupFeedBack()

    BreezeOverlaySheet(
        onDismissRequest = onDismissRequest,
        visible = true,
        modifier = modifier.statusBarsPadding(),
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isDeselectMode: Boolean
                val showToggleButton: Boolean
                if (isLimitEnabled) {
                    showToggleButton = selectedItems.isNotEmpty()
                    isDeselectMode = true
                } else {
                    showToggleButton = true
                    isDeselectMode = isAllFilteredSelected
                }

                if (showToggleButton) {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        enabled = !readOnly,
                        onClick = {
                            kdFeedback()
                            if (isDeselectMode) {
                                selectedItems.clear()
                            } else {
                                selectedItems.addAll(filteredList.filter {
                                    !selectedItems.contains(
                                        it
                                    )
                                })
                            }
                        },
                    ) {
                        val actionText = if (isDeselectMode) deselectAllLabel else selectAllLabel
                        Text("$actionText (${selectedItems.size})")
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (readOnly) {
                    OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = {
                        kdFeedback()
                        onDismissRequest()
                    }) { Text(closeLabel) }
                    return@Row
                }

                Button(
                    enabled = !isLimitEnabled || selectedItems.size <= maxSelection,
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        kdFeedback()
                        onConfirm(selectedItems.toSet())
                        onDismissRequest()
                    },
                ) { Text("OK") }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = { Text(searchLabel) },
            leadingIcon = {
                Icon(MaterialSymbols.Filled.Rounded.Search, contentDescription = searchLabel)
            },
            trailingIcon = {
                val motionScheme = MaterialTheme.motionScheme
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(motionScheme.defaultEffectsSpec()) +
                            scaleIn(motionScheme.defaultSpatialSpec()),
                    exit = fadeOut(motionScheme.defaultEffectsSpec()) +
                            scaleOut(motionScheme.defaultSpatialSpec()),
                ) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(MaterialSymbols.Filled.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f, fill = true)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            items(
                items = filteredList,
                key = { optionTitle(it) to (optionSubtitle(it) ?: "") },
            ) { item ->
                val isSelected = selectedItems.contains(item)
                val canToggle = isSelected || (!isLimitEnabled || selectedItems.size < maxSelection)
                SegmentedListItem(
                    modifier = Modifier.heightIn(min = 72.dp),
                    checked = isSelected,
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                    enabled = canToggle && !readOnly,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            selectedItems.remove(item)
                        } else if (!isLimitEnabled || selectedItems.size < maxSelection) {
                            selectedItems.add(item)
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(
                        filteredList.indexOf(item),
                        filteredList.size
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = {
                        Checkbox(
                            checked = isSelected,
                            enabled = canToggle && !readOnly,
                            onCheckedChange = null
                        )
                    },
                    content = { Text(text = optionTitle(item)) },
                    supportingContent = optionSubtitle(item)?.takeIf { it.isNotEmpty() }
                        ?.let { subtitle ->
                            { Text(text = subtitle) }
                        },
                )
            }
            item("bottomPadding") {
                // The sheet's contentWindowInsets already clears the navigation bar.
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyLabel, color = Color.Gray)
            }
        }
    }
}
