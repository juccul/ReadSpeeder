package com.jukul.readspeeder.data

internal data class DocumentChapter(
    val title: String,
    val startWord: Int,
)

internal data class ReadDocument(
    val id: String,
    val title: String,
    val author: String?,
    val text: String,
    val formattedHtml: List<String> = emptyList(),
    val cover: ByteArray? = null,
    val chapters: List<DocumentChapter> = emptyList(),
    val progress: Int = 0,
)

internal data class LibraryDocument(
    val id: String,
    val title: String,
    val author: String?,
    val cover: ByteArray? = null,
    val progress: Int = 0,
)

internal fun ReadDocument.toLibraryDocument() =
    LibraryDocument(id, title, author, cover, progress)
