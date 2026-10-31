package me.weishu.kernelsu.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlin.math.roundToInt
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ColorMode
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.expressive.TonalCard
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedMenuRow
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedSwitchItem
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.theme.keyColorOptions
import me.weishu.kernelsu.ui.theme.rememberBreezeColorScheme
import me.weishu.kernelsu.ui.util.isRailNavbar

/** Page-scale track bounds; the slider is keyed on these, never on the value. */
private const val MinPageScale = 0.8f
private const val MaxPageScale = 1.1f

/** Module description line-count track bounds and the three steps between them. */
private const val MinDescriptionLines = 1f
private const val MaxDescriptionLines = 5f
private const val DescriptionLineSteps = 3

@Composable
fun ColorPaletteScreenBreeze(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val preferences = state.preferences
    val currentColorMode = state.currentColorMode
    val currentKeyColor = preferences.keyColor
    val colorStyle = state.currentPaletteStyle
    val colorSpec = state.currentColorSpec
    BreezeScaffold(
        topBar = {
            BreezeTopAppBar(
                navigationIcon = {
                    BreezeBackButton(
                        onClick = actions.onBack,
                        collapseFraction = scrollBehavior.state.collapsedFraction,
                    )
                },
                title = { Text(stringResource(R.string.settings_theme)) },
            )
        },
    ) { innerPadding ->
        BreezeScrollColumn(
            innerPadding = innerPadding,
            // The color LazyRow and the inner Column both apply their own 16.dp horizontal inset.
            horizontalPadding = 0.dp,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val isDark =
                currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()
            val isAmoled = currentColorMode.isAmoled
            ThemePreviewCard(
                keyColor = currentKeyColor,
                isDark = isDark,
                isAmoled = isAmoled,
                paletteStyle = colorStyle,
                colorSpec = colorSpec,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ColorButtonBreeze(
                        color = Color.Unspecified,
                        isSelected = currentKeyColor == 0,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        paletteStyle = colorStyle,
                        colorSpec = colorSpec,
                        onClick = {
                            actions.onSetKeyColor(0)
                        }
                    )
                }

                items(keyColorOptions) { color ->
                    ColorButtonBreeze(
                        color = Color(color),
                        isSelected = currentKeyColor == color,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        paletteStyle = colorStyle,
                        colorSpec = colorSpec,
                        onClick = {
                            actions.onSetKeyColor(color)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    listOf(ColorMode.SYSTEM) to stringResource(R.string.settings_theme_mode_system),
                    listOf(ColorMode.LIGHT) to stringResource(R.string.settings_theme_mode_light),
                    listOf(ColorMode.DARK) to stringResource(R.string.settings_theme_mode_dark),
                    listOf(ColorMode.DARK_AMOLED) to stringResource(R.string.settings_theme_mode_dark)
                )

                options.chunked(4).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rowOptions.forEachIndexed { index, (modes, label) ->
                            ToggleButton(
                                colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                                checked = currentColorMode in modes,
                                onCheckedChange = {
                                    if (it) {
                                        actions.onSetColorMode(modes.first())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    rowOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                val modeIcon = when (modes.first()) {
                                    ColorMode.SYSTEM -> MaterialSymbols.Filled.Rounded.BrightnessAuto
                                    ColorMode.LIGHT -> MaterialSymbols.Filled.Rounded.Brightness5
                                    ColorMode.DARK -> MaterialSymbols.Filled.Rounded.Brightness2
                                    ColorMode.DARK_AMOLED -> MaterialSymbols.Filled.Rounded.Brightness1
                                }
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = label
                                )
                            }
                        }
                    }
                }


                SegmentedListGroup(
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    val styles = PaletteStyle.entries
                    val selectedStyleIndex = styles.indexOf(colorStyle).coerceAtLeast(0)
                    item(key = "colorStyle") {
                        SegmentedMenuRow(
                            title = stringResource(R.string.settings_color_style),
                            selectedText = styles[selectedStyleIndex].name,
                            leadingIcon = MaterialSymbols.Filled.Rounded.Style,
                            menuContent = { dismissMenu ->
                                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                    styles.forEachIndexed { index, style ->
                                        SelectableDropdownMenuItem(
                                            text = { Text(style.name) },
                                            onClick = {
                                                actions.onSetColorStyle(style)
                                                dismissMenu()
                                            },
                                            shapes = MenuDefaults.itemShape(
                                                index = index,
                                                count = styles.size
                                            ),
                                            selected = index == selectedStyleIndex,
                                            selectedLeadingIcon = {
                                                Icon(
                                                    MaterialSymbols.Filled.Rounded.Check,
                                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                                    contentDescription = null,
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }

                    val specs = ColorSpec.SpecVersion.entries
                    val selectedSpecIndex = specs.indexOf(colorSpec).coerceAtLeast(0)
                    item(key = "colorSpec") {
                        SegmentedMenuRow(
                            title = stringResource(R.string.settings_color_spec),
                            selectedText = specs[selectedSpecIndex].name,
                            leadingIcon = MaterialSymbols.Filled.Rounded.DesignServices,
                            menuContent = { dismissMenu ->
                                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                    specs.forEachIndexed { index, spec ->
                                        SelectableDropdownMenuItem(
                                            text = { Text(spec.name) },
                                            onClick = {
                                                actions.onSetColorSpec(spec)
                                                dismissMenu()
                                            },
                                            shapes = MenuDefaults.itemShape(
                                                index = index,
                                                count = specs.size
                                            ),
                                            selected = index == selectedSpecIndex,
                                            selectedLeadingIcon = {
                                                Icon(
                                                    MaterialSymbols.Filled.Rounded.Check,
                                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                                    contentDescription = null,
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                SegmentedListGroup(
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    item(key = "navigationBadge") {
                        SegmentedSwitchItem(
                            title = stringResource(R.string.settings_navigation_badge),
                            summary = stringResource(R.string.settings_navigation_badge_summary),
                            leadingIcon = MaterialSymbols.Filled.Rounded.Pin,
                            checked = preferences.enableNavigationBadge,
                            onCheckedChange = actions.onSetEnableNavigationBadge,
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SegmentedListGroup(
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        item(
                            key = "predictiveBack",
                            visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                        ) {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.settings_enable_predictive_back),
                                summary = stringResource(R.string.settings_enable_predictive_back_summary),
                                leadingIcon = MaterialSymbols.Rounded.MenuOpen,
                                checked = preferences.enablePredictiveBack,
                                onCheckedChange = actions.onSetEnablePredictiveBack,
                            )
                        }

                        item(key = "blur") {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.settings_enable_blur),
                                summary = stringResource(R.string.settings_enable_blur_summary),
                                leadingIcon = MaterialSymbols.Rounded.BlurOn,
                                checked = preferences.enableBlur,
                                onCheckedChange = actions.onSetEnableBlur,
                            )
                        }
                    }
                }

                TonalCard(
                    modifier = Modifier.padding(top = 4.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ) {
                    // Keyed on the range only: a value-keyed slider state would be rebuilt
                    // mid-gesture once the persisted setting came back.
                    val pageScaleState = remember(MinPageScale, MaxPageScale) {
                        SliderState(
                            value = preferences.pageScale,
                            steps = 0,
                            trackRange = MinPageScale..MaxPageScale,
                        )
                    }
                    LaunchedEffect(preferences.pageScale) {
                        if (pageScaleState.value != preferences.pageScale) {
                            pageScaleState.value = preferences.pageScale
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.AspectRatio,
                                contentDescription = stringResource(id = R.string.settings_page_scale),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_page_scale),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_page_scale_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(pageScaleState.value * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            state = pageScaleState,
                            onValueChange = { pageScaleState.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true,
                            onValueChangeFinished = { actions.onSetPageScale(pageScaleState.value) },
                        )
                    }
                }

                TonalCard(
                    modifier = Modifier.padding(top = 4.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ) {
                    val linesValue = preferences.moduleDescriptionMaxLines.toFloat()
                    val linesState = remember(MinDescriptionLines, MaxDescriptionLines) {
                        SliderState(
                            value = linesValue,
                            steps = DescriptionLineSteps,
                            trackRange = MinDescriptionLines..MaxDescriptionLines,
                        )
                    }
                    LaunchedEffect(linesValue) {
                        if (linesState.value != linesValue) linesState.value = linesValue
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Filled.Rounded.Description,
                                contentDescription = stringResource(id = R.string.settings_module_description_max_lines),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_module_description_max_lines),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_module_description_max_lines_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${linesState.value.roundToInt()} " + stringResource(R.string.unit_lines),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            state = linesState,
                            onValueChange = { linesState.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true,
                            onValueChangeFinished = {
                                actions.onSetModuleDescriptionMaxLines(linesState.value.roundToInt())
                            },
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = isRailNavbar()

    val colorScheme = rememberBreezeColorScheme(
        seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant)
        ) {
            val content: @Composable ColumnScope.() -> Unit = {
                // top bar
                Box(
                    modifier = Modifier
                        .height(if (useRail) 36.dp else 48.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 12.dp,
                                top = if (useRail) 8.dp else 16.dp,
                                bottom = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val showInfoCard = maxHeight >= 72.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TonalCard(
                            containerColor = colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            content = { }
                        )
                        if (showInfoCard) {
                            TonalCard(
                                containerColor = colorScheme.surfaceBright,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                content = { }
                            )
                        }
                    }
                }
            }

            if (useRail) {
                Row {
                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(36.dp),
                            verticalArrangement = Arrangement.spacedBy(
                                10.dp,
                                Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                MaterialSymbols.Filled.Rounded.Home,
                                null,
                                tint = colorScheme.primary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                Column {
                    content()

                    // bottom bar
                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    MaterialSymbols.Filled.Rounded.Home,
                                    null,
                                    tint = colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButtonBreeze(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    onClick: () -> Unit
) {
    val colorScheme = rememberBreezeColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Filled.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
