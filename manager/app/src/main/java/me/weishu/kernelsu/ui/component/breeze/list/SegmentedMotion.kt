package me.weishu.kernelsu.ui.component.breeze.list

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Tween specs for segmented-row 3D fold/insert/remove animations.
 * Uses `tween` rather than `MaterialTheme.motionScheme` springs so fold rotation completes before
 * height collapse and height never overshoots past zero.
 */

val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

val RowSizeEnterSpec = tween<IntSize>(500, easing = Emphasized)
val RowSizeExitSpec = tween<IntSize>(400, easing = Emphasized)
val RowFoldEnterSpec = tween<Float>(500, easing = EmphasizedDecelerate)

/** Finishes before [RowSizeExitSpec] so rotation completes before clipping. */
val RowFoldExitSpec = tween<Float>(340, easing = Emphasized)
val RowAlphaEnterSpec = tween<Float>(375)
val RowAlphaExitSpec = tween<Float>(240)
val RowCornerSpec = tween<Dp>(420, easing = Emphasized)

const val RowFoldDegrees = -38f
const val RowFoldScale = 0.93f
val RowFoldRise = 8.dp

/** CSS `perspective(700px)` converted to Compose's `1280 * density` px unit (`700 / 160`). */
const val RowFoldCameraDistance = 4.375f
