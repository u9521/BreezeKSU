package me.weishu.kernelsu.ui.webui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.webui.model.WebUIBanner
import me.weishu.kernelsu.ui.webui.model.WebUIState
import me.weishu.kernelsu.ui.webui.ui.component.OutdatedWebViewBanner
import me.weishu.kernelsu.ui.webui.ui.component.dialog.WebUIDialogContent
import me.weishu.kernelsu.ui.webui.viewmodel.WebUIViewModel

/** Banners and the active JavaScript dialog, hosted above the page. */
@Composable
fun BoxScope.WebUIOverlayHost(
    state: WebUIState,
    viewModel: WebUIViewModel,
) {
    if (state.banners.isNotEmpty()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.banners.forEach { banner ->
                key(banner.id) {
                    when (banner) {
                        is WebUIBanner.OutdatedWebView -> OutdatedWebViewBanner(
                            currentVersion = banner.currentVersion,
                            requiredVersion = banner.requiredVersion,
                            onDismiss = { viewModel.dismissBanner(banner.id) },
                            onUpdateClick = viewModel::updateWebView,
                        )
                    }
                }
            }
        }
    }

    state.activeDialog?.let { dialog ->
        // The module name is the dialog title; before a module resolves, fall back to the fork's
        // own name rather than to an empty bar.
        val title = state.moduleInfo?.name ?: stringResource(R.string.app_name)
        WebUIDialogContent(
            dialog = dialog,
            title = title,
            onClear = viewModel::clearDialog,
        )
    }
}
