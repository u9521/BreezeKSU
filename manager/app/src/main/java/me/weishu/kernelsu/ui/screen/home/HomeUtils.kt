package me.weishu.kernelsu.ui.screen.home

import androidx.compose.runtime.Immutable

@Immutable
data class SystemInfo(
    val kernelVersion: String,
    val managerVersion: String,
    val deviceModel: String,
    val fingerprint: String,
    val selinuxStatus: String,
    val seccompStatus: Int
)
