package com.jukul.readspeeder.data

import android.content.ContentValues
import android.content.ContentResolver
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class DocumentImporterInstrumentedTest {
    private lateinit var resolver: ContentResolver

    @Before
    fun setUp() {
        resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
    }

    @After
    fun tearDown() {
        resolver.delete(QualificationDocumentProvider.RootUri, null, null)
    }

    @Test
    fun importsUtf8Text() {
        val uri = add("sample.txt", "text/plain", "One two three.".toByteArray())

        val document = DocumentImporter.import(resolver, uri)

        assertEquals("sample", document.title)
        assertEquals("One two three.", document.text)
    }

    @Test
    fun importsMinimalEpubMetadataAndChapter() {
        val uri = add("book.epub", "application/epub+zip", minimalEpub())

        val document = DocumentImporter.import(resolver, uri)

        assertEquals("Qualification Book", document.title)
        assertEquals("Release Tester", document.author)
        assertTrue(document.text.contains("A tested chapter."))
        assertEquals("Chapter One", document.chapters.single().title)
    }

    @Test
    fun rejectsMalformedAndUnsupportedDocuments() {
        val malformed = add(
            "broken.epub",
            "application/epub+zip",
            zipOf("chapter.xhtml" to "<p>No package metadata</p>"),
        )
        val unsupported = add("image.png", "image/png", byteArrayOf(1, 2, 3))

        assertThrows(IllegalStateException::class.java) {
            DocumentImporter.import(resolver, malformed)
        }
        assertThrows(IllegalStateException::class.java) {
            DocumentImporter.import(resolver, unsupported)
        }
    }

    private fun minimalEpub(): ByteArray = zipOf(
        "META-INF/container.xml" to """
            <?xml version="1.0"?>
            <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles>
            </container>
        """.trimIndent(),
        "OEBPS/content.opf" to """
            <?xml version="1.0"?>
            <package xmlns="http://www.idpf.org/2007/opf">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Qualification Book</dc:title>
                <dc:creator>Release Tester</dc:creator>
              </metadata>
              <manifest>
                <item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine><itemref idref="chapter"/></spine>
            </package>
        """.trimIndent(),
        "OEBPS/chapter.xhtml" to """
            <html><body><h1>Chapter One</h1><p>A tested chapter.</p></body></html>
        """.trimIndent(),
    )

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun add(name: String, mimeType: String, contents: ByteArray): Uri =
        requireNotNull(
            resolver.insert(
                QualificationDocumentProvider.RootUri,
                ContentValues().apply {
                    put(QualificationDocumentProvider.Name, name)
                    put(QualificationDocumentProvider.MimeType, mimeType)
                    put(QualificationDocumentProvider.Contents, contents)
                },
            ),
        )
}
