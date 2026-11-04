package me.weishu.kernelsu.ui.webui.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Covers the page while it loads, so a half-painted WebView never flashes on screen. */
@Composable
fun WebUILoadingMask(modifier: Modifier = Modifier) {
    Surface {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    }
}
