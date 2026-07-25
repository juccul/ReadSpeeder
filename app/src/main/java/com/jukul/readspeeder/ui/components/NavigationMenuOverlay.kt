package com.jukul.readspeeder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.jukul.readspeeder.ui.AppDestination
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect

@Composable
internal fun NavigationMenuOverlay(
    expanded: Boolean,
    currentDestination: AppDestination,
    topOffset: Dp,
    hazeState: HazeState,
    backgroundColor: Color,
    onBoundsChanged: (Rect) -> Unit,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f)),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = topOffset)
                .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) },
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            val menuExpansion by transition.animateFloat(
                transitionSpec = { tween(280, easing = FastOutSlowInEasing) },
                label = "menu expansion",
            ) { if (it == EnterExitState.Visible) 1f else 0f }
            val cornerRadius by transition.animateDp(
                transitionSpec = { tween(280, easing = FastOutSlowInEasing) },
                label = "menu corners",
            ) { if (it == EnterExitState.Visible) 28.dp else 24.dp }
            val surfaceAlpha by transition.animateFloat(
                transitionSpec = { tween(120) },
                label = "menu surface",
            ) { if (it == EnterExitState.Visible) 1f else 0f }
            val contentAlpha by transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) {
                        tween(120, delayMillis = 140)
                    } else {
                        tween(80)
                    }
                },
                label = "menu content",
            ) { if (it == EnterExitState.Visible) 1f else 0f }
            val menuShape = RoundedCornerShape(cornerRadius)

            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = surfaceAlpha }
                    .clip(menuShape)
                    .hazeEffect(state = hazeState) {
                        blurEffect {
                            this.backgroundColor = backgroundColor
                            blurRadius = 28.dp
                        }
                    }
                    .background(backgroundColor.copy(alpha = 0.68f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = menuShape,
                    )
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val collapsedSize = 48.dp.roundToPx()
                        layout(
                            width = lerp(collapsedSize, placeable.width, menuExpansion),
                            height = lerp(collapsedSize, placeable.height, menuExpansion),
                        ) {
                            placeable.placeRelative(0, 0)
                        }
                    },
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { alpha = contentAlpha }
                        .width(IntrinsicSize.Max)
                        .padding(12.dp),
                ) {
                    AppDestination.entries.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(destination.titleRes)) },
                            selected = currentDestination == destination,
                            onClick = { onDestinationSelected(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            badge = {
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.secondaryContainer.copy(
                                        alpha = 0.72f,
                                    ),
                            ),
                        )
                    }
                }
            }
        }
    }
}
