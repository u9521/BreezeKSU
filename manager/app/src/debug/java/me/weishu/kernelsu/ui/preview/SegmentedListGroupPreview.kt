package me.weishu.kernelsu.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedCheckboxItem
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedMenuRow
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedNavItem
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedSwitchItem

@Composable
fun SegmentedListGroupPreview() {
    var wifiEnabled by remember { mutableStateOf(true) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var nfcEnabled by remember { mutableStateOf(true) }
    var airplaneMode by remember { mutableStateOf(false) }
    var customDpi by remember { mutableStateOf(false) }
    var verboseLogging by remember { mutableStateOf(true) }

    var sortOrder by remember { mutableStateOf("Name") }
    var themeMode by remember { mutableStateOf("System") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "SegmentedListGroup — All Item Types",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SegmentedListGroup(title = "Connectivity") {
            item(key = "wifi") {
                SegmentedSwitchItem(
                    title = "Wi-Fi",
                    summary = "Enable or disable Wi-Fi",
                    checked = wifiEnabled,
                    onCheckedChange = { wifiEnabled = it },
                )
            }
            item(key = "bluetooth") {
                SegmentedSwitchItem(
                    title = "Bluetooth",
                    summary = "Enable or disable Bluetooth",
                    checked = bluetoothEnabled,
                    onCheckedChange = { bluetoothEnabled = it },
                )
            }
            item(key = "nfc") {
                SegmentedSwitchItem(
                    title = "NFC",
                    summary = "Near Field Communication",
                    checked = nfcEnabled,
                    onCheckedChange = { nfcEnabled = it },
                )
            }
            item(key = "airplane") {
                SegmentedSwitchItem(
                    title = "Airplane Mode",
                    summary = "Disable all wireless radios",
                    checked = airplaneMode,
                    onCheckedChange = { airplaneMode = it },
                )
            }
        }

        SegmentedListGroup(title = "Appearance") {
            item(key = "sortOrder") {
                SegmentedMenuRow(
                    title = "Sort Order",
                    selectedText = sortOrder,
                    menuContent = { dismiss ->
                        DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                            listOf("Name", "Size", "Date").forEachIndexed { index, label ->
                                SelectableDropdownMenuItem(
                                    selected = sortOrder == label,
                                    shapes = MenuDefaults.itemShape(index = index, count = 3),
                                    text = { Text(label) },
                                    onClick = { sortOrder = label; dismiss() }
                                )
                            }
                        }
                    },
                )
            }
            item(key = "themeMode") {
                SegmentedMenuRow(
                    title = "Theme Mode",
                    selectedText = themeMode,
                    menuContent = { dismiss ->
                        DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                            listOf("System", "Light", "Dark", "Battery Saver").forEachIndexed { index, label ->
                                SelectableDropdownMenuItem(
                                    selected = themeMode == label,
                                    shapes = MenuDefaults.itemShape(index = index, count = 4),
                                    text = { Text(label) },
                                    onClick = { themeMode = label; dismiss() }
                                )
                            }
                        }
                    },
                )
            }
        }

        SegmentedListGroup(title = "Developer Options") {
            item(key = "customDpi") {
                SegmentedSwitchItem(
                    title = "Custom DPI",
                    summary = "Override screen density",
                    checked = customDpi,
                    onCheckedChange = { customDpi = it },
                )
            }
            item(key = "verboseLogging") {
                SegmentedCheckboxItem(
                    title = "Verbose Logging",
                    summary = "Enable detailed debug logs",
                    checked = verboseLogging,
                    onCheckedChange = { verboseLogging = it },
                )
            }
            item(key = "manage") {
                SegmentedNavItem(
                    title = "Manage Developer Settings",
                    onClick = {},
                )
            }
        }

        Text(
            "Wi-Fi: ${if (wifiEnabled) "ON" else "OFF"}  ·  " +
                    "Sort: $sortOrder  ·  " +
                    "Theme: $themeMode",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSegmentedListGroup() {
    SegmentedListGroupPreview()
}
