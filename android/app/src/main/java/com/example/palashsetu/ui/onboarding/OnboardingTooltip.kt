package com.example.palashsetu.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palashsetu.R
import com.example.palashsetu.theme.Primary
import com.example.palashsetu.theme.Secondary
import com.example.palashsetu.theme.SurfaceContainerLow
import com.example.palashsetu.theme.SurfaceContainerLowest

/**
 * Bilingual tooltip card shown alongside the spotlight.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────┐
 * │  [Hindi title — bold Primary]                       │
 * │  [English title — medium gray]                      │
 * │  ─────────────────────────────────────────────────  │
 * │  [Hindi body — regular]                             │
 * │  [English body — lighter gray]                      │
 * │                                                     │
 * │  [1/16]  [← पिछला]  [अगला →]  [छोड़ें ✕]           │
 * └─────────────────────────────────────────────────────┘
 *
 * Design follows AGENTS.md: 8dp card corners, 14–16dp inner padding,
 * 42dp button height, 4dp button corners, maxLines=1/softWrap=false on buttons.
 */
@Composable
fun OnboardingTooltip(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    visible: Boolean,
    arrowDirection: ArrowDirection = ArrowDirection.BOTTOM,
    modifier: Modifier = Modifier
) {
    val step = state.currentStep
    val isHindi = state.language == "hi"

    val cardShape = RoundedCornerShape(8.dp)        // Stitch rounded-xl container
    val buttonShape = RoundedCornerShape(4.dp)      // Stitch rounded-lg interactive control

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) +
                slideInVertically(animationSpec = androidx.compose.animation.core.tween(250)) { it / 4 },
        exit  = fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) +
                slideOutVertically(animationSpec = androidx.compose.animation.core.tween(180)) { it / 4 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = cardShape)
                .clip(cardShape)
                .background(SurfaceContainerLowest)
                .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = cardShape)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // ── Step counter + Skip button ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step counter pill
                    Text(
                        text = state.stepCounter(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier
                            .clip(buttonShape)
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )

                    // Skip / dismiss ✕
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(buttonShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { onSkip() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = if (isHindi) "छोड़ें" else "Skip",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // ── Title block ────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Hindi title
                    Text(
                        text = step.hiTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    // English subtitle (shown always for dual-language clarity)
                    Text(
                        text = step.enTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // ── Body block ─────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (isHindi) step.hiBody else step.enBody,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1E293B),
                        lineHeight = 18.sp
                    )
                    if (isHindi) {
                        // Show English sub-caption for non-Hindi-fluent reference
                        Text(
                            text = step.enBody,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }

                // ── Navigation row ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button (hidden on step 0)
                    if (!state.isFirstStep) {
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(buttonShape)
                                .background(SurfaceContainerLow)
                                .border(1.dp, Color(0xFFCBD5E1), buttonShape)
                                .clickable { onPrevious() }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_left),
                                    contentDescription = "Previous",
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isHindi) "पिछला" else "Back",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Next / Finish button
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(buttonShape)
                            .background(if (state.isLastStep) Secondary else Primary)
                            .clickable { onNext() }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when {
                                    state.isLastStep && isHindi -> "शुरू करें ✓"
                                    state.isLastStep            -> "Get Started ✓"
                                    isHindi                     -> "अगला"
                                    else                        -> "Next"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (!state.isLastStep) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_right),
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
