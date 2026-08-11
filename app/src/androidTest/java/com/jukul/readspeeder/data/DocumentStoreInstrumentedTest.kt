package com.jukul.readspeeder.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class DocumentStoreInstrumentedTest {
    private lateinit var context: Context
    private lateinit var store: DocumentStore

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = DocumentStore(context)
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun concurrentDifferentDocumentSavesRetainEveryDocument() {
        val documents = List(20) { document("document-$it", "Title $it") }
        concurrently(documents.map { { DocumentStore(context).save(it) } })

        assertEquals(
            documents.map(ReadDocument::id).toSet(),
            DocumentStore(context).loadSummaries().documents.map(LibraryDocument::id).toSet(),
        )
    }

    @Test
    fun concurrentSameDocumentSavesProduceOneCompleteVersion() {
        val documents = List(20) {
            document("shared", "Title $it").copy(text = "Text $it")
        }
        concurrently(documents.map { { DocumentStore(context).save(it) } })

        val restored = store.load("shared")
        assertEquals(
            restored.title.substringAfter("Title "),
            restored.text.substringAfter("Text "),
        )
    }

    @Test
    fun corruptIndexRecoversEveryValidDocument() {
        store.save(document("first", "First"))
        store.save(document("second", "Second"))
        assertTrue(documentFile("first").setLastModified(1_000))
        assertTrue(documentFile("second").setLastModified(2_000))
        val missingId = "missing"
        preferences().edit()
            .putString("document_index", "not-json")
            .putInt("progress_${missingId.sha256()}", 50)
            .putString("active_document", missingId)
            .commit()

        val restored = DocumentStore(context).loadSummaries()

        assertEquals(listOf("second", "first"), restored.documents.map { it.id })
        assertEquals(0, restored.failedCount)
        assertFalse(preferences().contains("progress_${missingId.sha256()}"))
        assertFalse(preferences().contains("active_document"))
    }

    @Test
    fun backupWinsAndValidatedLegacyTemporaryRecoversWhenBaseIsMissing() {
        val original = document("document", "Original")
        val replacement = document("document", "Replacement")
        store.save(original)
        val target = documentFile(original.id)
        File("${target.path}.tmp").outputStream().use {
            DocumentCodec.encode(replacement, it)
        }
        assertTrue(target.renameTo(File("${target.path}.bak")))

        val restoredStore = DocumentStore(context)
        restoredStore.loadSummaries()
        assertEquals("Original", restoredStore.load(original.id).title)
        assertFalse(File("${target.path}.tmp").exists())

        File("${target.path}.bak").writeText("invalid")
        val invalidBackup = DocumentStore(context).loadSummaries()
        assertEquals("Original", DocumentStore(context).load(original.id).title)
        assertEquals(1, invalidBackup.failedCount)

        assertTrue(target.delete())
        File("${target.path}.tmp").outputStream().use {
            DocumentCodec.encode(replacement, it)
        }
        File("${target.path}.new").writeText("uncommitted")

        val restored = DocumentStore(context).loadSummaries()
        assertEquals("Replacement", restored.documents.single().title)
        assertTrue(target.exists())
        assertFalse(File("${target.path}.new").exists())
    }

    @Test
    fun corruptCacheIsQuarantinedOnceWithoutHidingValidSiblings() {
        store.save(document("valid", "Valid"))
        store.save(document("corrupt", "Corrupt"))
        val corrupt = documentFile("corrupt")
        RandomAccessFile(corrupt, "rw").use { it.setLength(corrupt.length() / 2) }

        val first = DocumentStore(context).loadSummaries()
        val second = DocumentStore(context).loadSummaries()

        assertEquals(listOf("valid"), first.documents.map { it.id })
        assertEquals(1, first.failedCount)
        assertEquals(0, second.failedCount)
        assertTrue(File(context.filesDir, "documents/quarantine").listFiles().orEmpty().isNotEmpty())
    }

    @Test
    fun failedReplacementLeavesPreviousDocumentReadable() {
        store.save(document("document", "Original"))
        val invalid = document("document", "Invalid").copy(
            chapters = List(10_001) { DocumentChapter("Chapter", it) },
        )

        assertThrows(IllegalArgumentException::class.java) { store.save(invalid) }
        assertEquals("Original", store.load("document").title)
    }

    @Test
    fun deletedDocumentsRejectLateProgressWritesAndClearRemovesQuarantine() {
        val value = document("document", "Document")
        store.save(value)
        concurrently(
            listOf(
                { store.updateProgress(value.id, 50, ProgressPositionScale / 2) },
                { store.delete(value.id) },
            ),
        )

        assertFalse(preferences().contains("progress_${value.id.sha256()}"))
        assertTrue(store.loadSummaries().documents.isEmpty())

        val quarantine = File(context.filesDir, "documents/quarantine").apply { mkdirs() }
        File(quarantine, "cache.corrupt").writeText("bytes")
        store.clear()
        store.updateProgress(value.id, 75, ProgressPositionScale * 3 / 4)

        assertFalse(preferences().contains("progress_${value.id.sha256()}"))
        assertFalse(File(context.filesDir, "documents").exists())
    }

    @Test
    fun summariesKeepOnlyCoverPresenceAndLoadCoverOnDemand() {
        val cover = byteArrayOf(1, 2, 3, 4)
        store.save(document("cover", "Cover").copy(cover = cover))

        val summary = store.loadSummaries().documents.single()

        assertTrue(summary.hasCover)
        assertArrayEquals(cover, store.loadCover(summary.id))
    }

    private fun document(id: String, title: String) =
        ReadDocument(id, title, "Author", "One two three.")

    private fun documentFile(id: String) =
        File(context.filesDir, "documents/${id.sha256()}.bin")

    private fun preferences() =
        context.getSharedPreferences("readspeeder_documents", Context.MODE_PRIVATE)

    private fun concurrently(actions: List<() -> Unit>) {
        val executor = Executors.newFixedThreadPool(actions.size.coerceAtMost(8))
        val start = CountDownLatch(1)
        try {
            val futures = actions.map { action ->
                executor.submit {
                    start.await()
                    action()
                }
            }
            start.countDown()
            futures.forEach { it.get() }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
