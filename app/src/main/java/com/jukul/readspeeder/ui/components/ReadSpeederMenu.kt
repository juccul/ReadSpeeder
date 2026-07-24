package com.jukul.readspeeder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.R

private val MenuItemCornerRadius = 16.dp

@Composable
fun ReadSpeederMenu(
    currentDestination: String,
    onDestinationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .padding(12.dp),
    ) {
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.home)) },
            selected = currentDestination == "home",
            onClick = { onDestinationClick("home") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                )
            },
            badge = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MenuItemCornerRadius),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                selectedContainerColor =
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            ),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.settings)) },
            selected = currentDestination == "settings",
            onClick = { onDestinationClick("settings") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            },
            badge = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MenuItemCornerRadius),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                selectedContainerColor =
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            ),
        )
    }
}
