package com.jukul.readspeeder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.data.LibrarySort
import com.jukul.readspeeder.data.LibraryDocument
import com.jukul.readspeeder.ui.components.DocumentCard

@Composable
internal fun LibraryScreen(
    state: LazyGridState,
    contentPadding: PaddingValues,
    documents: List<LibraryDocument>,
    searchQuery: String,
    sort: LibrarySort,
    loadCover: (String) -> ByteArray?,
    onDocumentClick: (LibraryDocument) -> Unit,
    onDocumentLongClick: (LibraryDocument, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleDocuments by remember(documents, searchQuery, sort) {
        derivedStateOf { filterAndSortDocuments(documents, searchQuery, sort) }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(visibleDocuments, key = LibraryDocument::id) { document ->
            DocumentCard(
                documentId = document.id,
                title = document.title,
                author = document.author,
                hasCover = document.hasCover,
                loadCover = loadCover,
                progress = document.progress,
                onClick = { onDocumentClick(document) },
                onLongClick = { onDocumentLongClick(document, it) },
            )
        }
    }
}

internal fun filterAndSortDocuments(
    documents: List<LibraryDocument>,
    query: String,
    sort: LibrarySort,
): List<LibraryDocument> {
    val matches = if (query.isBlank()) {
        documents
    } else {
        documents.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.author?.contains(query, ignoreCase = true) == true
        }
    }
    val titleOrder = compareBy<LibraryDocument, String>(
        String.CASE_INSENSITIVE_ORDER,
        LibraryDocument::title,
    )
    return when (sort) {
        LibrarySort.RecentlyRead -> matches
        LibrarySort.TitleAscending -> matches.sortedWith(titleOrder)
        LibrarySort.TitleDescending -> matches.sortedWith(titleOrder.reversed())
    }
}
