package com.jukul.readspeeder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.data.LibrarySort
import com.jukul.readspeeder.data.ReadDocument
import com.jukul.readspeeder.ui.components.DocumentCard

@Composable
internal fun LibraryScreen(
    state: LazyGridState,
    contentPadding: PaddingValues,
    documents: List<ReadDocument>,
    searchQuery: String,
    sort: LibrarySort,
    onDocumentClick: (ReadDocument) -> Unit,
    onDocumentLongClick: (ReadDocument, Offset) -> Unit,
    modifier: Modifier = Modifier,
) = LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = filterAndSortDocuments(documents, searchQuery, sort),
        ) { document ->
            DocumentCard(
                title = document.title,
                author = document.author,
                cover = document.cover,
                progress = document.progress,
                onClick = { onDocumentClick(document) },
                onLongClick = { onDocumentLongClick(document, it) },
            )
        }
    }

internal fun filterAndSortDocuments(
    documents: List<ReadDocument>,
    query: String,
    sort: LibrarySort,
): List<ReadDocument> {
    val matches = if (query.isBlank()) {
        documents
    } else {
        documents.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.author?.contains(query, ignoreCase = true) == true
        }
    }
    val titleOrder = compareBy<ReadDocument, String>(
        String.CASE_INSENSITIVE_ORDER,
        ReadDocument::title,
    )
    return when (sort) {
        LibrarySort.RecentlyRead -> matches
        LibrarySort.TitleAscending -> matches.sortedWith(titleOrder)
        LibrarySort.TitleDescending -> matches.sortedWith(titleOrder.reversed())
    }
}
