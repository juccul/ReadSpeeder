package com.jukul.readspeeder.data

import com.jukul.readspeeder.ui.screens.filterAndSortDocuments
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PersistenceTest {
    @Test
    fun settingsAreClampedAndAligned() {
        val settings = AppSettings(
            defaultWpm = 1_003,
            textSize = 40,
            lineSpacing = Float.NaN,
            horizontalMargin = 19,
        ).validated()

        assertEquals(1_000, settings.defaultWpm)
        assertEquals(32, settings.textSize)
        assertEquals(1.5f, settings.lineSpacing)
        assertEquals(16, settings.horizontalMargin)
        assertEquals(true, AppSettings().splitHyphenatedWords)
    }

    @Test
    fun documentCacheRoundTrips() {
        val document = ReadDocument(
            id = "document",
            title = "Title",
            author = "Author",
            text = "One two three.",
            formattedHtml = listOf("<p>One <em>two</em> three.</p>"),
            cover = byteArrayOf(1, 2, 3),
            chapters = listOf(DocumentChapter("Chapter 1", 0)),
            progress = 37,
        )

        val restored = DocumentCodec.decode(DocumentCodec.encode(document))

        assertEquals(document.copy(cover = null), restored.copy(cover = null))
        assertArrayEquals(document.cover, restored.cover)
    }

    @Test
    fun truncatedDocumentCacheIsRejected() {
        val bytes = DocumentCodec.encode(
            ReadDocument("id", "Title", null, "Text"),
        )

        assertThrows(Exception::class.java) {
            DocumentCodec.decode(bytes.copyOf(bytes.size / 2))
        }
    }

    @Test
    fun librarySearchAndSortingUseTitlesAndAuthors() {
        val documents = listOf(
            ReadDocument("1", "Zulu", null, ""),
            ReadDocument("2", "alpha", "George Orwell", ""),
            ReadDocument("3", "Beta", null, ""),
        )

        assertEquals(
            listOf("2", "3", "1"),
            filterAndSortDocuments(documents, "", LibrarySort.TitleAscending)
                .map(ReadDocument::id),
        )
        assertEquals(
            listOf("1", "3", "2"),
            filterAndSortDocuments(documents, "", LibrarySort.TitleDescending)
                .map(ReadDocument::id),
        )
        assertEquals(
            listOf("2"),
            filterAndSortDocuments(documents, "orwell", LibrarySort.RecentlyRead)
                .map(ReadDocument::id),
        )
    }

    @Test
    fun epubChapterLabelsDiscardLeadingIllustrationCaptions() {
        assertEquals(
            "Chapter II.",
            cleanEpubChapterTitle("I hope Mr. Bingley will like it. CHAPTER II."),
        )
        assertEquals("Chapter XXVII.", cleanEpubChapterTitle("CHAPTERXXVII."))
    }
}
