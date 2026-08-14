package com.juckul.readspeeder.data

import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Html
import android.util.Xml
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

internal const val MaxDocumentBytes = 50 * 1024 * 1024
private const val MaxExtractedCharacters = 5_000_000
private const val MaxCoverDimension = 1_024
private const val CoverQuality = 85
private const val FormattedBlockLines = 50
private const val FormattedBlockCharacters = 2_048
private val ChapterHeadingPattern = Regex(
    """<h[1-3]\b[^>]*>.*?</h[1-3]>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val BodyPattern = Regex(
    """<body\b[^>]*>(.*)</body>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val ChapterTitlePrefix = Regex(
    """(?i)(chapter|part|book)(?:\s+|(?=[ivxlcdm]+\b))""",
)

private data class ImportedChapter(
    val title: String,
    val text: String,
    val formattedHtml: String? = null,
)

private data class ImportedContent(
    val text: String,
    val formattedHtml: List<String> = emptyList(),
    val title: String? = null,
    val author: String? = null,
    val cover: ByteArray? = null,
    val chapters: List<ImportedChapter> = emptyList(),
    val chapterMarkers: List<DocumentChapter> = emptyList(),
)

private data class EpubNavigation(
    val title: String,
    val path: String,
    val fragment: String?,
)

internal object DocumentImporter {
    fun import(resolver: ContentResolver, uri: Uri): ReadDocument {
        val name = resolver.displayName(uri)
        resolver.validateSize(uri)
        val extension = name.substringAfterLast('.', "").lowercase()
        val mimeType = resolver.getType(uri)
        val format = when {
            extension == "txt" || mimeType == "text/plain" -> "TXT"
            extension == "pdf" || mimeType == "application/pdf" -> "PDF"
            extension == "epub" || mimeType == "application/epub+zip" -> "EPUB"
            else -> error("Only TXT, PDF, and EPUB documents are supported")
        }
        val content = resolver.openInputStream(uri)?.use { stream ->
            when (format) {
                "TXT" -> ImportedContent(stream.readLimited().decodeText())
                "PDF" -> extractPdf(stream)
                else -> extractEpub(stream)
            }
        } ?: error("Unable to open document")
        val normalizedChapters = content.chapters.mapNotNull { chapter ->
            val text = chapter.text.normalizeDocumentText()
            chapter.copy(text = text).takeIf { text.isNotEmpty() }
        }
        val normalizedText = if (normalizedChapters.isEmpty()) {
            content.text.normalizeDocumentText()
        } else {
            normalizedChapters.joinToString("\n\n", transform = ImportedChapter::text)
        }
        require(normalizedText.isNotEmpty()) { "No readable text was found in this document" }
        require(normalizedText.length <= MaxExtractedCharacters) {
            "The document contains too much text to import"
        }
        var chapterStart = 0
        val fallbackChapters = normalizedChapters.map { chapter ->
            DocumentChapter(chapter.title, chapterStart).also {
                chapterStart += chapter.text.wordCount()
            }
        }
        return ReadDocument(
            id = uri.toString(),
            title = content.title ?: name.substringBeforeLast('.').ifBlank { name },
            author = content.author,
            text = normalizedText,
            formattedHtml = content.formattedHtml,
            cover = content.cover,
            chapters = content.chapterMarkers.ifEmpty { fallbackChapters },
        )
    }
}

private fun extractPdf(stream: InputStream): ImportedContent =
    PDDocument.load(stream).use { document ->
        require(!document.isEncrypted) { "Password-protected PDFs are not supported" }
        ImportedContent(
            text = PDFTextStripper().getText(document),
            title = document.documentInformation.title.cleanMetadata(),
            author = document.documentInformation.author.cleanMetadata(),
            cover = if (document.numberOfPages > 0) {
                try {
                    val pageSize = document.getPage(0).mediaBox.let {
                        maxOf(it.width, it.height)
                    }
                    val scale = (MaxCoverDimension / pageSize).coerceAtMost(1f)
                    PDFRenderer(document).renderImage(0, scale).toJpeg()
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            },
        )
    }

private fun extractEpub(stream: InputStream): ImportedContent {
    val entries = readEpubEntries(stream)
    val container = entries["META-INF/container.xml"]
        ?: error("Invalid EPUB: container.xml is missing")
    val packagePath = container.findAttribute("rootfile", "full-path")
        ?: error("Invalid EPUB: package document is missing")
    val packageDocument = entries[packagePath]
        ?: error("Invalid EPUB: package document cannot be opened")
    val manifest = mutableMapOf<String, String>()
    val images = mutableMapOf<String, String>()
    val spine = mutableListOf<String>()
    var title: String? = null
    var author: String? = null
    var coverId: String? = null
    var coverPath: String? = null
    var navigationPath: String? = null
    packageDocument.parseXml { parser ->
        when (parser.name) {
            "title" -> if (title == null) {
                title = parser.nextText().trim().takeIf(String::isNotEmpty)
            }

            "creator" -> if (author == null) {
                author = parser.nextText().trim().takeIf(String::isNotEmpty)
            }

            "meta" -> if (parser.getAttributeValue(null, "name") == "cover") {
                coverId = parser.getAttributeValue(null, "content")
            }

            "item" -> {
                val id = parser.getAttributeValue(null, "id")
                val href = parser.getAttributeValue(null, "href")
                val mediaType = parser.getAttributeValue(null, "media-type")
                if (id != null && href != null) {
                    val path = resolveEpubPath(packagePath, href)
                    if (mediaType in setOf("application/xhtml+xml", "text/html")) {
                        manifest[id] = path
                    } else if (mediaType?.startsWith("image/") == true) {
                        images[id] = path
                        if (
                            parser.getAttributeValue(null, "properties")
                                ?.split(' ')
                                ?.contains("cover-image") == true
                        ) {
                            coverPath = path
                        }
                    } else if (mediaType == "application/x-dtbncx+xml") {
                        navigationPath = path
                    }
                }
            }

            "itemref" -> if (parser.getAttributeValue(null, "linear") != "no") {
                parser.getAttributeValue(null, "idref")?.let(spine::add)
            }
        }
    }
    val contentItems = spine.mapNotNull { id -> manifest[id]?.let { id to it } }
        .ifEmpty { manifest.toList() }
    require(contentItems.isNotEmpty()) { "No readable chapters were found in this EPUB" }
    val navigation = navigationPath
        ?.let { path -> entries[path]?.extractNcxNavigation(path) }
        .orEmpty()
        .let { entries ->
            entries.filter { ChapterTitlePrefix.containsMatchIn(it.title) }
                .ifEmpty { entries }
        }
    val navigationByPath = navigation.groupBy(EpubNavigation::path)
    val chapterMarkers = mutableListOf<DocumentChapter>()
    var precedingWords = 0
    var currentPart: String? = null
    val chapters = contentItems.mapIndexedNotNull { index, (id, path) ->
        val bytes = entries[path] ?: return@mapIndexedNotNull null
        val html = bytes.decodeText()
        val body = BodyPattern.find(html)?.groupValues?.get(1) ?: html
        val text = Html.fromHtml(body, Html.FROM_HTML_MODE_LEGACY).toString()
        if (text.isBlank()) return@mapIndexedNotNull null
        navigationByPath[path].orEmpty().forEach { entry ->
            body.wordOffsetAt(entry.fragment)?.let { offset ->
                chapterMarkers += DocumentChapter(
                    cleanEpubChapterTitle(entry.title),
                    precedingWords + offset,
                )
            }
        }
        precedingWords += text.normalizeDocumentText().wordCount()
        val headings = body.extractHeadings()
        headings.firstOrNull { it.startsWith("Part ", ignoreCase = true) }
            ?.let { currentPart = it }
        val heading = headings.lastOrNull()
        val chapterTitle = when {
            heading == null -> id
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Chapter ${index + 1}" }
            currentPart != null && heading.startsWith("Chapter ", ignoreCase = true) ->
                "$currentPart · $heading"
            else -> headings.joinToString(" · ")
        }
        ImportedChapter(chapterTitle, text, body)
    }
    val cover = (coverPath ?: coverId?.let(images::get))
        ?.let(entries::get)
        ?.toThumbnail()
    return ImportedContent(
        text = chapters.joinToString("\n\n", transform = ImportedChapter::text),
        formattedHtml = chapters.mapNotNull(ImportedChapter::formattedHtml),
        title = title,
        author = author,
        cover = cover,
        chapters = chapters,
        chapterMarkers = chapterMarkers.distinctBy(DocumentChapter::startWord),
    )
}

private fun ByteArray.extractNcxNavigation(path: String): List<EpubNavigation> {
    val parser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        setInput(inputStream(), null)
    }
    val entries = mutableListOf<EpubNavigation>()
    var navPointDepth = 0
    var label: String? = null
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "navPoint" -> {
                    navPointDepth++
                    label = null
                }

                "text" -> if (navPointDepth > 0 && label == null) {
                    label = parser.nextText().normalizeDocumentText().replace('\n', ' ')
                }

                "content" -> if (navPointDepth > 0) {
                    val source = parser.getAttributeValue(null, "src")?.let(Uri::decode)
                    val title = label
                    if (source != null && title != null) {
                        entries += EpubNavigation(
                            title = title,
                            path = resolveEpubPath(path, source.substringBefore('#')),
                            fragment = source.substringAfter('#', "").takeIf(String::isNotEmpty),
                        )
                    }
                }
            }

            XmlPullParser.END_TAG -> if (parser.name == "navPoint") navPointDepth--
        }
    }
    return entries
}

internal fun cleanEpubChapterTitle(label: String): String {
    val normalized = label.normalizeDocumentText().replace('\n', ' ')
    val prefix = ChapterTitlePrefix.findAll(normalized).lastOrNull() ?: return normalized
    val kind = prefix.groupValues[1].lowercase().replaceFirstChar(Char::uppercase)
    return "$kind ${normalized.substring(prefix.range.last + 1).trimStart()}"
}

internal fun formatHtmlBlocks(html: List<String>): List<AnnotatedString> =
    html.flatMap { body ->
        buildAnnotatedString {
            append(AnnotatedString.fromHtml(body))
            append("\n")
        }.chunkedBlocks()
    }

internal fun formatPlainTextBlocks(text: String): List<AnnotatedString> =
    AnnotatedString(text).chunkedBlocks()

private fun AnnotatedString.chunkedBlocks(): List<AnnotatedString> = buildList {
    var start = 0
    var lines = 0
    text.forEachIndexed { index, character ->
        if (character == '\n') lines++
        if (
            character.isWhitespace() &&
            (lines >= FormattedBlockLines || index - start + 1 >= FormattedBlockCharacters)
        ) {
            add(subSequence(start, index + 1))
            start = index + 1
            lines = 0
        }
    }
    if (start < length) add(subSequence(start, length))
}

private fun String.extractHeadings(): List<String> =
    ChapterHeadingPattern.findAll(this).mapNotNull { match ->
        Html.fromHtml(match.value, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .normalizeDocumentText()
            .replace('\n', ' ')
            .takeIf(String::isNotEmpty)
    }.toList()

private fun String.wordOffsetAt(fragment: String?): Int? {
    if (fragment == null) return 0
    val anchor = Regex(
        """<[^>]+\b(?:id|name)\s*=\s*["']${Regex.escape(fragment)}["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    ).find(this) ?: return null
    return Html.fromHtml(
        substring(0, anchor.range.first),
        Html.FROM_HTML_MODE_LEGACY,
    ).toString().normalizeDocumentText().wordCount()
}

private fun String.wordCount(): Int =
    if (isBlank()) 0 else split(Regex("\\s+")).size

private fun String?.cleanMetadata(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)

private fun ByteArray.toThumbnail(): ByteArray? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
    val sampleSize = maxOf(
        1,
        ((maxDimension.toLong() + MaxCoverDimension - 1) / MaxCoverDimension).toInt(),
    )
    val bitmap = BitmapFactory.decodeByteArray(
        this,
        0,
        size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null
    return bitmap.toJpeg()
}

private fun Bitmap.toJpeg(): ByteArray? = try {
    ByteArrayOutputStream().use { output ->
        if (compress(Bitmap.CompressFormat.JPEG, CoverQuality, output)) {
            output.toByteArray()
        } else {
            null
        }
    }
} finally {
    recycle()
}

private fun ByteArray.findAttribute(tag: String, attribute: String): String? {
    var result: String? = null
    parseXml { parser ->
        if (parser.name == tag) result = parser.getAttributeValue(null, attribute)
    }
    return result
}

private inline fun ByteArray.parseXml(onStartTag: (XmlPullParser) -> Unit) {
    val parser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        setInput(inputStream(), null)
    }
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.START_TAG) onStartTag(parser)
    }
}

private fun resolveEpubPath(packagePath: String, href: String): String {
    val segments = mutableListOf<String>()
    "${packagePath.substringBeforeLast('/', "")}/${Uri.decode(href).substringBefore('#')}"
        .split('/')
        .forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
    return segments.joinToString("/")
}

private fun InputStream.readLimited(limit: Int = MaxDocumentBytes): ByteArray {
    require(limit > 0) { "Document is larger than 50 MB" }
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= limit) { "Document is larger than 50 MB" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun ByteArray.decodeText(): String = when {
    size >= 3 && this[0] == 0xEF.toByte() &&
        this[1] == 0xBB.toByte() && this[2] == 0xBF.toByte() ->
        copyOfRange(3, size).toString(StandardCharsets.UTF_8)
    size >= 2 && this[0] == 0xFF.toByte() && this[1] == 0xFE.toByte() ->
        copyOfRange(2, size).toString(StandardCharsets.UTF_16LE)
    size >= 2 && this[0] == 0xFE.toByte() && this[1] == 0xFF.toByte() ->
        copyOfRange(2, size).toString(StandardCharsets.UTF_16BE)
    else -> toString(StandardCharsets.UTF_8)
}

private fun ContentResolver.displayName(uri: Uri): String =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        cursor.string(OpenableColumns.DISPLAY_NAME)
    } ?: uri.lastPathSegment.orEmpty().ifBlank { "Document" }

private fun ContentResolver.validateSize(uri: Uri) {
    val size = query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        cursor.long(OpenableColumns.SIZE)
    }
    require(size == null || size < 0 || size <= MaxDocumentBytes) {
        "Document is larger than 50 MB"
    }
}

private fun Cursor.string(column: String): String? =
    if (moveToFirst()) getString(getColumnIndexOrThrow(column)) else null

private fun Cursor.long(column: String): Long? =
    if (moveToFirst() && !isNull(getColumnIndexOrThrow(column))) {
        getLong(getColumnIndexOrThrow(column))
    } else {
        null
    }

private fun String.normalizeDocumentText(): String =
    replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace(Regex("[\\t ]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
