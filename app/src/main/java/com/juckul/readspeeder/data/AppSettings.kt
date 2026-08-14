package com.juckul.readspeeder.data

internal const val MinWpm = 50
internal const val MaxWpm = 1_000
internal const val WpmStep = 5

internal enum class ThemeMode { System, Light, Dark }
internal enum class ReaderMode { Speed, Standard }
internal enum class ReadingFont { SansSerif, Serif }
internal enum class ReadingAlignment { Start, Justified }
internal enum class LibrarySort { RecentlyRead, TitleAscending, TitleDescending }

internal data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val defaultReader: ReaderMode = ReaderMode.Speed,
    val keepScreenAwake: Boolean = false,
    val defaultWpm: Int = 300,
    val smartPauses: Boolean = false,
    val complexWordPauses: Boolean = false,
    val splitHyphenatedWords: Boolean = true,
    val playbackCountdown: Boolean = false,
    val sentencePreview: Boolean = true,
    val focusGuides: Boolean = true,
    val autoHideControls: Boolean = false,
    val readingFont: ReadingFont = ReadingFont.SansSerif,
    val textSize: Int = 16,
    val lineSpacing: Float = 1.5f,
    val horizontalMargin: Int = 24,
    val alignment: ReadingAlignment = ReadingAlignment.Start,
    val librarySort: LibrarySort = LibrarySort.RecentlyRead,
) {
    fun validated() = copy(
        defaultWpm = ((defaultWpm / WpmStep) * WpmStep).coerceIn(MinWpm, MaxWpm),
        textSize = textSize.coerceIn(12, 32),
        lineSpacing = lineSpacing.takeIf(Float::isFinite)?.coerceIn(1f, 2f) ?: 1.5f,
        horizontalMargin = ((horizontalMargin / 4) * 4).coerceIn(8, 48),
    )
}
