package com.jukul.readspeeder.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.jukul.readspeeder.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect

@Composable
internal fun AddContentButton(
    expanded: Boolean,
    hazeState: HazeState,
    backgroundColor: Color,
    onExpandedChange: (Boolean) -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    onPasteText: () -> Unit,
    onAddDocument: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuShape = RoundedCornerShape(28.dp)
    val transition = updateTransition(targetState = expanded, label = "add menu")
    val menuExpansion by transition.animateFloat(
        transitionSpec = { tween(280, easing = FastOutSlowInEasing) },
        label = "add menu expansion",
    ) { if (it) 1f else 0f }
    val contentAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(120, delayMillis = 140) else tween(80)
        },
        label = "add menu content",
    ) { if (it) 1f else 0f }
    val plusAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(80) else tween(120, delayMillis = 140)
        },
        label = "add icon",
    ) { if (it) 0f else 1f }

    Box(
        modifier = modifier
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
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val collapsedSize = 56.dp.roundToPx()
                val width = lerp(collapsedSize, placeable.width, menuExpansion)
                val height = lerp(collapsedSize, placeable.height, menuExpansion)
                layout(width, height) {
                    placeable.placeRelative(
                        x = width - placeable.width,
                        y = height - placeable.height,
                    )
                }
            },
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .graphicsLayer { alpha = contentAlpha }
                .padding(8.dp),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.paste_text)) },
                onClick = {
                    onExpandedChange(false)
                    onPasteText()
                },
                leadingIcon = {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = transition.currentState && transition.targetState,
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_document)) },
                onClick = {
                    onExpandedChange(false)
                    onAddDocument()
                },
                leadingIcon = {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = transition.currentState && transition.targetState,
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer { alpha = plusAlpha }
                .clickable(enabled = !expanded) {
                    onExpandedChange(true)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription =
                    if (expanded) null else stringResource(R.string.add_content),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
