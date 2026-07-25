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
    val cover: ByteArray? = null,
    val chapters: List<DocumentChapter> = emptyList(),
    val progress: Int = 0,
)
