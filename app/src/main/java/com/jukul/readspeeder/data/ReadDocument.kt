package com.jukul.readspeeder.data

internal const val ProgressPositionScale = 1_000_000

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
    val progressPosition: Int = progress * (ProgressPositionScale / 100),
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

internal fun progressPosition(wordIndex: Int, lastIndex: Int): Int {
    if (lastIndex <= 0) return 0
    return (
        (wordIndex.coerceIn(0, lastIndex).toLong() * ProgressPositionScale + lastIndex / 2) /
            lastIndex
    ).toInt()
}

internal fun wordIndexAtProgress(progressPosition: Int, lastIndex: Int): Int {
    if (lastIndex <= 0) return 0
    return (
        (lastIndex.toLong() * progressPosition.coerceIn(0, ProgressPositionScale) +
            ProgressPositionScale / 2) / ProgressPositionScale
    ).toInt()
}

internal fun progressPercent(progressPosition: Int): Int =
    (
        (progressPosition.coerceIn(0, ProgressPositionScale).toLong() * 100 +
            ProgressPositionScale / 2) / ProgressPositionScale
    ).toInt()
