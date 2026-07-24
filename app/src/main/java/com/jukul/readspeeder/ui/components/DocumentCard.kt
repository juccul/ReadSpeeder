package com.jukul.readspeeder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun DocumentCard(
    title: String,
    author: String,
    progress: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Box(Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = author,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
