package com.jukul.readspeeder.data

import android.content.Context

internal class SettingsStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("readspeeder_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        themeMode = preferences.enum("theme_mode", ThemeMode.System),
        dynamicColor = preferences.getBoolean("dynamic_color", true),
        defaultReader = preferences.enum("default_reader", ReaderMode.Speed),
        keepScreenAwake = preferences.getBoolean("keep_screen_awake", false),
        defaultWpm = preferences.getInt("default_wpm", 300),
        smartPauses = preferences.getBoolean("smart_pauses", false),
        complexWordPauses = preferences.getBoolean("complex_word_pauses", false),
        splitHyphenatedWords = preferences.getBoolean("split_hyphenated_words", true),
        playbackCountdown = preferences.getBoolean("playback_countdown", false),
        sentencePreview = preferences.getBoolean("sentence_preview", true),
        focusGuides = preferences.getBoolean("focus_guides", true),
        autoHideControls = preferences.getBoolean("auto_hide_controls", false),
        readingFont = preferences.enum("reading_font", ReadingFont.SansSerif),
        textSize = preferences.getInt("text_size", 16),
        lineSpacing = preferences.getFloat("line_spacing", 1.5f),
        horizontalMargin = preferences.getInt("horizontal_margin", 24),
        alignment = preferences.enum("alignment", ReadingAlignment.Start),
        librarySort = preferences.enum("library_sort", LibrarySort.RecentlyRead),
    ).validated()

    fun save(settings: AppSettings) {
        val value = settings.validated()
        preferences.edit()
            .putString("theme_mode", value.themeMode.name)
            .putBoolean("dynamic_color", value.dynamicColor)
            .putString("default_reader", value.defaultReader.name)
            .putBoolean("keep_screen_awake", value.keepScreenAwake)
            .putInt("default_wpm", value.defaultWpm)
            .putBoolean("smart_pauses", value.smartPauses)
            .putBoolean("complex_word_pauses", value.complexWordPauses)
            .putBoolean("split_hyphenated_words", value.splitHyphenatedWords)
            .putBoolean("playback_countdown", value.playbackCountdown)
            .putBoolean("sentence_preview", value.sentencePreview)
            .putBoolean("focus_guides", value.focusGuides)
            .putBoolean("auto_hide_controls", value.autoHideControls)
            .putString("reading_font", value.readingFont.name)
            .putInt("text_size", value.textSize)
            .putFloat("line_spacing", value.lineSpacing)
            .putInt("horizontal_margin", value.horizontalMargin)
            .putString("alignment", value.alignment.name)
            .putString("library_sort", value.librarySort.name)
            .apply()
    }
}

private inline fun <reified T : Enum<T>> android.content.SharedPreferences.enum(
    key: String,
    default: T,
): T = getString(key, null)?.let { value ->
    enumValues<T>().firstOrNull { it.name == value }
} ?: default
