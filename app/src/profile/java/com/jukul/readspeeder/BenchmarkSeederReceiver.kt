package com.jukul.readspeeder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.jukul.readspeeder.data.AppSettings
import com.jukul.readspeeder.data.DocumentChapter
import com.jukul.readspeeder.data.DocumentStore
import com.jukul.readspeeder.data.ReadDocument
import com.jukul.readspeeder.data.ReaderMode
import com.jukul.readspeeder.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

internal class BenchmarkSeederReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SeedAction) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seed(context.applicationContext, intent)
                pendingResult.resultCode = 0
            } catch (_: Exception) {
                pendingResult.resultCode = 1
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun seed(context: Context, intent: Intent) {
        val store = DocumentStore(context)
        store.clear()
        if (intent.getBooleanExtra(EmptyExtra, false)) return

        val cover = createCover()
        repeat(40) { index ->
            val number = index + 1
            store.save(
                ReadDocument(
                    id = "benchmark-$number",
                    title = "Performance test ${number.toString().padStart(2, '0')}",
                    author = "Benchmark Author ${number % 7}",
                    text = buildString {
                        repeat(600) { word ->
                            append("document").append(number).append('-').append(word).append(' ')
                        }
                    },
                    cover = cover,
                    progress = number * 2 % 100,
                ),
                makeRecent = false,
            )
        }

        val chapterSize = 7_500
        val largeText = buildString(1_800_000) {
            repeat(150_000) { index ->
                append(
                    when {
                        index % 47 == 0 -> "performance,"
                        index % 89 == 0 -> "responsiveness;"
                        index % 127 == 0 -> "characteristically-complex."
                        else -> "reading"
                    },
                )
                append(' ')
            }
        }
        store.save(
            ReadDocument(
                id = LargeDocumentId,
                title = LargeDocumentTitle,
                author = "Benchmark Author",
                text = largeText,
                chapters = List(20) {
                    DocumentChapter("Chapter ${it + 1}", it * chapterSize)
                },
                cover = cover,
            ),
        )
        val readerMode = if (intent.getStringExtra(ReaderExtra) == "standard") {
            ReaderMode.Standard
        } else {
            ReaderMode.Speed
        }
        SettingsStore(context).save(
            AppSettings(
                defaultReader = readerMode,
                defaultWpm = 1_000,
                sentencePreview = true,
            ),
        )
        store.setActiveDocument(
            LargeDocumentId.takeIf { intent.getBooleanExtra(ActiveExtra, false) },
        )
    }

    private fun createCover(): ByteArray {
        val bitmap = Bitmap.createBitmap(96, 144, Bitmap.Config.RGB_565)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(45, 65, 105))
            drawCircle(48f, 56f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(220, 225, 240)
            })
        }
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
            bitmap.recycle()
            it.toByteArray()
        }
    }

    private companion object {
        const val SeedAction = "com.jukul.readspeeder.BENCHMARK_SEED"
        const val EmptyExtra = "empty"
        const val ActiveExtra = "active"
        const val ReaderExtra = "reader"
        const val LargeDocumentId = "benchmark-large"
        const val LargeDocumentTitle = "Large performance document"
    }
}
