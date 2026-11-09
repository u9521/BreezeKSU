package me.weishu.kernelsu.ui.screen.template

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.data.model.TemplateInfo
import me.weishu.kernelsu.ui.util.UiMessage

@Immutable
data class TemplateUiState(
    val isRefreshing: Boolean = false,
    val offline: Boolean = false,
    val templates: List<TemplateInfo> = emptyList(),
    val templateList: List<TemplateInfo> = emptyList(),
    val error: Throwable? = null,
    /** One-shot feedback shown by the scaffold; [TemplateActions.onMessageShown] clears it. */
    val message: UiMessage? = null,
)

@Immutable
data class TemplateActions(
    val onBack: () -> Unit,
    val onRefresh: (Boolean) -> Unit,
    val onImport: () -> Unit,
    val onExport: () -> Unit,
    val onCreateTemplate: () -> Unit,
    val onOpenTemplate: (TemplateInfo) -> Unit,
    val onMessageShown: (Long) -> Unit,
)
