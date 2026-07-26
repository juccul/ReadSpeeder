package com.jukul.readspeeder.data

import androidx.compose.ui.text.AnnotatedString

internal data class DocumentChapter(
    val title: String,
    val startWord: Int,
)

internal data class ReadDocument(
    val id: String,
    val title: String,
    val author: String?,
    val text: String,
    val formattedText: List<AnnotatedString> = emptyList(),
    val formattedHtml: List<String> = emptyList(),
    val cover: ByteArray? = null,
    val chapters: List<DocumentChapter> = emptyList(),
    val progress: Int = 0,
)
