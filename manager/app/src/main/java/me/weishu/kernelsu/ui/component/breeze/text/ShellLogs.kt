package me.weishu.kernelsu.ui.component.breeze.text

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn
import me.weishu.kernelsu.ui.component.common.KeyEventBlocker
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.UiMessage

/** Monospace log viewer page; auto-collapses the top bar and scrolls to the bottom when [text] updates. */
@Composable
fun ShellLogScaffold(
    title: @Composable () -> Unit,
    text: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    fabLift: Boolean = false,
    fabVisible: Boolean = true,
    message: UiMessage? = null,
    onMessageShown: (Long) -> Unit = {},
    snackBarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val showScrollbar by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0.99f }
    }

    BreezeScaffold(
        snackbarHostState = snackBarHost,
        message = message,
        onMessageShown = onMessageShown,
        scrollBehavior = scrollBehavior,
        topBar = {
            BreezeTopAppBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = {
                    BreezeBackButton(
                        onClick = onBack,
                        collapseFraction = scrollBehavior.state.collapsedFraction,
                    )
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = MaterialSymbols.Filled.Rounded.Save,
                            contentDescription = "Save",
                        )
                    }
                },
            )
        },
        floatingActionButton = floatingActionButton,
        fabLift = fabLift,
        fabVisible = fabVisible,
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        BreezeScrollColumn(
            innerPadding = innerPadding,
            scrollState = scrollState,
            showScrollbar = showScrollbar,
            horizontalPadding = 0.dp,
            bottomExtraSpacing = 0.dp,
            verticalArrangement = Arrangement.Top,
        ) {
            ShellLogText(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                scrollState = scrollState,
                scrollBehavior = scrollBehavior,
            )
        }
    }
}

@Composable
fun ShellLogText(
    modifier: Modifier = Modifier,
    text: String,
    scrollState: ScrollState,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(text) {
        if (scrollState.maxValue > 0) {
            launch {
                val currentOffset = scrollBehavior.state.heightOffset
                val targetOffset = scrollBehavior.state.heightOffsetLimit
                if (currentOffset > targetOffset + 0.5f) {
                    animate(
                        initialValue = currentOffset,
                        targetValue = targetOffset,
                        animationSpec = animationSpec,
                    ) { value, _ ->
                        scrollBehavior.state.heightOffset = value
                    }
                }
            }
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Text(
        modifier = modifier.padding(8.dp),
        text = text,
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        fontFamily = FontFamily.Monospace,
        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
    )
}
