package com.jukul.readspeeder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.data.ReadDocument
import com.jukul.readspeeder.ui.components.DocumentCard

@Composable
internal fun LibraryScreen(
    state: LazyGridState,
    contentPadding: PaddingValues,
    documents: List<ReadDocument>,
    onDocumentClick: (ReadDocument) -> Unit,
    modifier: Modifier = Modifier,
) = LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(documents, key = ReadDocument::id) { document ->
            DocumentCard(
                title = document.title,
                author = document.author,
                cover = document.cover,
                progress = document.progress,
                onClick = { onDocumentClick(document) },
            )
        }
    }
