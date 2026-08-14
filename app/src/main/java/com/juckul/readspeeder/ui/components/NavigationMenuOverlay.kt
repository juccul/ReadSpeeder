package com.juckul.readspeeder.ui.components

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.juckul.readspeeder.R
import com.juckul.readspeeder.data.LibrarySort
import com.juckul.readspeeder.ui.AppDestination
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
) = AnimatedMenuOverlay(
    expanded = expanded,
    alignment = Alignment.TopStart,
    horizontalOffset = 8.dp,
    topOffset = topOffset,
    hazeState = hazeState,
    backgroundColor = backgroundColor,
    onBoundsChanged = onBoundsChanged,
    modifier = modifier,
) {
    AppDestination.entries.forEach { destination ->
        NavigationDrawerItem(
            label = { Text(stringResource(destination.titleRes)) },
            selected = currentDestination == destination,
            onClick = { onDestinationSelected(destination) },
            icon = { Icon(destination.icon, contentDescription = null) },
            badge = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = 0.72f,
                ),
            ),
        )
    }
}

@Composable
internal fun SortMenuOverlay(
    expanded: Boolean,
    currentSort: LibrarySort,
    topOffset: Dp,
    hazeState: HazeState,
    backgroundColor: Color,
    onBoundsChanged: (Rect) -> Unit,
    onSortSelected: (LibrarySort) -> Unit,
    modifier: Modifier = Modifier,
) = AnimatedMenuOverlay(
    expanded = expanded,
    alignment = Alignment.TopEnd,
    horizontalOffset = (-8).dp,
    topOffset = topOffset,
    hazeState = hazeState,
    backgroundColor = backgroundColor,
    onBoundsChanged = onBoundsChanged,
    modifier = modifier,
) {
    LibrarySort.entries.forEach { sort ->
        val selected = currentSort == sort
        NavigationDrawerItem(
            label = {
                Text(
                    stringResource(
                        when (sort) {
                            LibrarySort.RecentlyRead -> R.string.most_recently_read
                            LibrarySort.TitleAscending -> R.string.title_a_z
                            LibrarySort.TitleDescending -> R.string.title_z_a
                        },
                    ),
                )
            },
            selected = selected,
            onClick = { onSortSelected(sort) },
            badge = {
                if (selected) Icon(Icons.Default.Check, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = 0.72f,
                ),
            ),
        )
    }
}

@Composable
internal fun DocumentMenuOverlay(
    expanded: Boolean,
    anchor: Offset,
    hazeState: HazeState,
    backgroundColor: Color,
    onBoundsChanged: (Rect) -> Unit,
    onChangeTitle: () -> Unit,
    onChangeAuthor: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val anchorX = with(density) { anchor.x.toDp() }.coerceIn(8.dp, maxWidth - 8.dp)
        val anchorY = with(density) { anchor.y.toDp() }.coerceIn(8.dp, maxHeight - 8.dp)
        val opensLeft = anchorX + 176.dp > maxWidth - 8.dp
        val opensUp = anchorY + 160.dp > maxHeight - 8.dp
        val alignment = when {
            opensLeft && opensUp -> Alignment.BottomEnd
            opensLeft -> Alignment.TopEnd
            opensUp -> Alignment.BottomStart
            else -> Alignment.TopStart
        }

        AnimatedMenuOverlay(
            expanded = expanded,
            alignment = alignment,
            horizontalOffset = if (opensLeft) anchorX - maxWidth else anchorX,
            topOffset = if (opensUp) anchorY - maxHeight else anchorY,
            hazeState = hazeState,
            backgroundColor = backgroundColor,
            onBoundsChanged = onBoundsChanged,
            modifier = Modifier,
            contentPadding = 8.dp,
            collapsedSize = 56.dp,
            expandFromEnd = opensLeft,
            expandFromBottom = opensUp,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.change_title)) },
                onClick = onChangeTitle,
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.change_author)) },
                onClick = onChangeAuthor,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = onDelete,
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnimatedMenuOverlay(
    expanded: Boolean,
    alignment: Alignment,
    horizontalOffset: Dp,
    topOffset: Dp,
    hazeState: HazeState,
    backgroundColor: Color,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
    contentPadding: Dp = 12.dp,
    collapsedSize: Dp = 48.dp,
    expandFromEnd: Boolean = false,
    expandFromBottom: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
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
                .align(alignment)
                .offset(x = horizontalOffset, y = topOffset)
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
                        val collapsedPixels = collapsedSize.roundToPx()
                        val width = lerp(collapsedPixels, placeable.width, menuExpansion)
                        val height = lerp(collapsedPixels, placeable.height, menuExpansion)
                        layout(width, height) {
                            placeable.placeRelative(
                                x = if (expandFromEnd) width - placeable.width else 0,
                                y = if (expandFromBottom) height - placeable.height else 0,
                            )
                        }
                    },
            ) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { alpha = contentAlpha }
                        .width(IntrinsicSize.Max)
                        .padding(contentPadding),
                    content = content,
                )
            }
        }
    }
}
