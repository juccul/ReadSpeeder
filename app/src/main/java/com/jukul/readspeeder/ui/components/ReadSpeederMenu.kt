package com.jukul.readspeeder.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.R

@Composable
fun ReadSpeederMenu(onHomeClick: () -> Unit) {
    ModalDrawerSheet {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.home)) },
            selected = true,
            onClick = onHomeClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                )
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
