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

internal val SampleDocuments = List(12) { index ->
    ReadDocument(
        id = "sample-$index",
        title = "Sample Document ${index + 1}",
        author = "Author ${index + 1}",
        text = """
            This is sample content for Sample Document ${index + 1}.

            Import a TXT, PDF, or EPUB document to read its complete text in speed or standard mode.
        """.trimIndent(),
        progress = index * 9,
    )
}
