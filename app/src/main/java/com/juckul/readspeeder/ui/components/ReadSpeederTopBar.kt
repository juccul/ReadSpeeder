package com.juckul.readspeeder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.lerp as lerpTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import com.juckul.readspeeder.R

internal val CollapsedTopBarHeight = 64.dp
internal val ExpandedTopBarHeight = 224.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadSpeederTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    subtitle: String?,
    showActions: Boolean,
    showBackNavigation: Boolean,
    forceCollapsed: Boolean,
    searchQuery: String?,
    onNavigationClick: () -> Unit,
    onSearchQueryChange: (String?) -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapsedFraction = if (forceCollapsed) 1f else scrollBehavior.state.collapsedFraction
    val searching = showActions && searchQuery != null
    val query = searchQuery.orEmpty()
    val searchFocusRequester = remember { FocusRequester() }

    SideEffect {
        scrollBehavior.state.heightOffsetLimit = with(density) {
            -(ExpandedTopBarHeight - CollapsedTopBarHeight).toPx()
        }
    }
    LaunchedEffect(searching) {
        if (searching) searchFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                statusBarHeight +
                    lerpDp(ExpandedTopBarHeight, CollapsedTopBarHeight, collapsedFraction),
            ),
    ) {
        val titleStyle = lerpTextStyle(
            MaterialTheme.typography.headlineLarge,
            MaterialTheme.typography.titleLarge,
            collapsedFraction,
        )
        val titleY = statusBarHeight +
            if (subtitle == null) lerpDp(116.dp, 18.dp, collapsedFraction) else 4.dp
        if (searching) {
            BasicTextField(
                value = query,
                onValueChange = { onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = titleY)
                    .padding(
                        start = lerpDp(16.dp, 64.dp, collapsedFraction),
                        end = 16.dp,
                    )
                    .focusRequester(searchFocusRequester),
                textStyle = titleStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { field ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.search_library),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = titleStyle,
                            )
                        }
                        field()
                    }
                },
            )
        } else {
            Text(
                text = title,
                modifier = Modifier.offset(
                    x = lerpDp(16.dp, 64.dp, collapsedFraction),
                    y = titleY,
                ),
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.offset(x = 64.dp, y = statusBarHeight + 34.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(CollapsedTopBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (searching) onSearchQueryChange(null) else onNavigationClick()
                },
            ) {
                Icon(
                    if (showBackNavigation || searching) Icons.AutoMirrored.Filled.ArrowBack
                    else Icons.Default.Menu,
                    stringResource(
                        when {
                            searching -> R.string.close_search
                            showBackNavigation -> R.string.back_to_library
                            else -> R.string.open_navigation
                        },
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            if (showActions && !searching) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        Icons.Default.Search,
                        stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onSortClick) {
                    Icon(
                        Icons.Default.FilterList,
                        stringResource(R.string.sort),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
