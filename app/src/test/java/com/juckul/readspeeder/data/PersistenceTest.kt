package com.juckul.readspeeder.data

import com.juckul.readspeeder.ui.screens.filterAndSortDocuments
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

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
        val summary = DocumentCodec.decodeSummary(DocumentCodec.encode(document))
        val cover = DocumentCodec.decodeCover(DocumentCodec.encode(document))

        assertEquals(document.copy(cover = null), restored.copy(cover = null))
        assertArrayEquals(document.cover, restored.cover)
        assertEquals(document.toLibraryDocument(), summary)
        assertArrayEquals(document.cover, cover)
    }

    @Test
    fun frozenVersionOneCacheRemainsCompatible() {
        val expected = ReadDocument(
            id = "fixture",
            title = "Legacy",
            author = "Author",
            text = "One two.",
            formattedHtml = listOf("<p>One two.</p>"),
            cover = byteArrayOf(1, 2, 3),
            chapters = listOf(DocumentChapter("Chapter", 0)),
            progress = 37,
        )
        val bytes = Base64.getDecoder().decode(
            "UlNQRAAAAAEAAAAHZml4dHVyZQAAAAZMZWdhY3kBAAAABkF1dGhvcgAAAAhPbmUgdHdv" +
                "LgAAAAEAAAAPPHA+T25lIHR3by48L3A+AAAAAQAAAAdDaGFwdGVyAAAAAAEAAAADAQID" +
                "AAAAJQ==",
        )

        assertEquals(expected.copy(cover = null), DocumentCodec.decode(bytes).copy(cover = null))
        assertArrayEquals(expected.cover, DocumentCodec.decodeCover(bytes))
        assertEquals(expected.toLibraryDocument(), DocumentCodec.decodeSummary(bytes))
        assertArrayEquals(bytes, DocumentCodec.encode(expected))
    }

    @Test
    fun codecLeavesCallerOwnedStreamsOpen() {
        val output = CloseTrackingOutputStream()
        DocumentCodec.encode(ReadDocument("id", "Title", null, "Text"), output)
        assertFalse(output.closed)

        val input = CloseTrackingInputStream(output.toByteArray())
        DocumentCodec.decode(input)
        assertFalse(input.closed)

        val summaryInput = CloseTrackingInputStream(output.toByteArray())
        DocumentCodec.decodeSummary(summaryInput)
        assertFalse(summaryInput.closed)

        val coverInput = CloseTrackingInputStream(output.toByteArray())
        DocumentCodec.decodeCover(coverInput)
        assertFalse(coverInput.closed)
    }

    @Test
    fun codecRejectsCachesItsDecoderCannotRead() {
        assertThrows(IllegalArgumentException::class.java) {
            DocumentCodec.encode(
                ReadDocument(
                    id = "blocks",
                    title = "Title",
                    author = null,
                    text = "Text",
                    formattedHtml = List(10_001) { "" },
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            DocumentCodec.encode(
                ReadDocument(
                    id = "chapters",
                    title = "Title",
                    author = null,
                    text = "Text",
                    chapters = List(10_001) { DocumentChapter("Chapter", it) },
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            DocumentCodec.encode(
                ReadDocument(
                    id = "cover",
                    title = "Title",
                    author = null,
                    text = "Text",
                    cover = ByteArray(20 * 1024 * 1024 + 1),
                ),
            )
        }
    }

    @Test
    fun preciseProgressRestoresTheSameWordInLargeDocuments() {
        val lastIndex = 149_999
        val wordIndex = 83_417
        val position = progressPosition(wordIndex, lastIndex)

        assertEquals(wordIndex, wordIndexAtProgress(position, lastIndex))
        assertEquals(56, progressPercent(position))
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
            LibraryDocument("1", "Zulu", null),
            LibraryDocument("2", "alpha", "George Orwell"),
            LibraryDocument("3", "Beta", null),
        )

        assertEquals(
            listOf("2", "3", "1"),
            filterAndSortDocuments(documents, "", LibrarySort.TitleAscending)
                .map(LibraryDocument::id),
        )
        assertEquals(
            listOf("1", "3", "2"),
            filterAndSortDocuments(documents, "", LibrarySort.TitleDescending)
                .map(LibraryDocument::id),
        )
        assertEquals(
            listOf("2"),
            filterAndSortDocuments(documents, "orwell", LibrarySort.RecentlyRead)
                .map(LibraryDocument::id),
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

    @Test
    fun longParagraphsAreSplitIntoLazyReaderBlocks() {
        val text = "word ".repeat(2_000)
        val blocks = formatPlainTextBlocks(text)

        assertTrue(blocks.size > 1)
        assertTrue(blocks.all { it.length <= 2_052 })
        assertEquals(text, blocks.joinToString("") { it.text })
    }

    private class CloseTrackingOutputStream : ByteArrayOutputStream() {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    private class CloseTrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }
}
