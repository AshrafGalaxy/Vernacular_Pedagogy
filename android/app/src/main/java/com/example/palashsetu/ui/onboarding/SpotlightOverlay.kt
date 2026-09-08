package com.example.palashsetu.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

// Transparent frosted glass wash — neutral luminous frosted mist
// Completely eliminates dark blue, navy blue, and black tints
private val FROSTED_GLASS_MIST = Color(0x35FFFFFF)          // 21% soft white frosted mist
private val FROSTED_GLASS_REFRACTION = Color(0x14000000)    // 8% subtle neutral refraction (zero hue tint)

private const val SPOTLIGHT_PADDING_PX = 14f  // extra space around the target
private const val CORNER_RADIUS_RECT = 20f    // rounded rect cutout radius (Stitch rounded-xl)

/**
 * Full-screen Canvas overlay that applies a soft, translucent frosted-glass
 * veil over the screen and punches a crisp, illuminated spotlight cutout
 * directly over [targetBounds].
 *
 * Smoothly animates position between steps and draws a glowing dual-tone border
 * (Palash Navy + crisp white outline) around the active target.
 *
 * @param targetBounds  Bounding box (in root coordinates) of the highlighted composable.
 * @param shape         [SpotlightShape.RECT] or [SpotlightShape.CIRCLE].
 * @param visible       Controls the scrim alpha (animated).
 */
@Composable
fun SpotlightOverlay(
    targetBounds: Rect?,
    shape: SpotlightShape,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "scrim_alpha"
    )

    // Smoothly animate the spotlight coordinates when changing between steps
    val animLeft by animateFloatAsState(
        targetValue = targetBounds?.left ?: 0f,
        animationSpec = tween(durationMillis = 260),
        label = "spot_left"
    )
    val animTop by animateFloatAsState(
        targetValue = targetBounds?.top ?: 0f,
        animationSpec = tween(durationMillis = 260),
        label = "spot_top"
    )
    val animRight by animateFloatAsState(
        targetValue = targetBounds?.right ?: 0f,
        animationSpec = tween(durationMillis = 260),
        label = "spot_right"
    )
    val animBottom by animateFloatAsState(
        targetValue = targetBounds?.bottom ?: 0f,
        animationSpec = tween(durationMillis = 260),
        label = "spot_bottom"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        if (alpha == 0f) return@Canvas

        if (targetBounds == null) {
            // Momentary fallback before target is measured: draw subtle frosted glass wash only
            drawRect(color = FROSTED_GLASS_MIST.copy(alpha = alpha * 0.5f))
            return@Canvas
        }

        val padded = Rect(
            left   = (animLeft   - SPOTLIGHT_PADDING_PX).coerceAtLeast(0f),
            top    = (animTop    - SPOTLIGHT_PADDING_PX).coerceAtLeast(0f),
            right  = (animRight  + SPOTLIGHT_PADDING_PX).coerceAtMost(size.width),
            bottom = (animBottom + SPOTLIGHT_PADDING_PX).coerceAtMost(size.height)
        )

        val cornerR = when (shape) {
            SpotlightShape.CIRCLE -> padded.minDimension / 2f
            SpotlightShape.RECT   -> CORNER_RADIUS_RECT
        }

        // 1. Build the cutout path
        val cutout = Path().apply {
            addRoundRect(
                RoundRect(
                    rect         = padded,
                    cornerRadius = CornerRadius(cornerR, cornerR)
                )
            )
        }

        // 2. Draw neutral transparent frosted glass veil with cutout erased via ClipOp.Difference
        clipPath(cutout, clipOp = ClipOp.Difference) {
            // Subtle neutral dark refraction for contrast
            drawRect(color = FROSTED_GLASS_REFRACTION.copy(alpha = alpha))
            // Soft white frosted glass mist
            drawRect(color = FROSTED_GLASS_MIST.copy(alpha = alpha))
        }

        // 3. Draw illuminated glowing aperture border around the target
        // Outer Palash Navy glow ring (2.5dp)
        drawRoundRect(
            color = Color(0xFF00236F).copy(alpha = alpha * 0.85f),
            topLeft = Offset(padded.left, padded.top),
            size = Size(padded.width, padded.height),
            cornerRadius = CornerRadius(cornerR, cornerR),
            style = Stroke(width = 2.5f.dp.toPx())
        )

        // Inner crisp white highlight ring (1dp)
        drawRoundRect(
            color = Color.White.copy(alpha = alpha * 0.95f),
            topLeft = Offset(padded.left + 1.2f.dp.toPx(), padded.top + 1.2f.dp.toPx()),
            size = Size(
                (padded.width - 2.4f.dp.toPx()).coerceAtLeast(0f),
                (padded.height - 2.4f.dp.toPx()).coerceAtLeast(0f)
            ),
            cornerRadius = CornerRadius((cornerR - 1.2f.dp.toPx()).coerceAtLeast(0f), (cornerR - 1.2f.dp.toPx()).coerceAtLeast(0f)),
            style = Stroke(width = 1f.dp.toPx())
        )
    }
}
