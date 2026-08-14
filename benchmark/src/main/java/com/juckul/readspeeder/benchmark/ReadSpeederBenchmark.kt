package com.juckul.readspeeder.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ReadSpeederBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun emptyColdStartup() = startup(empty = true)

    @Test
    fun populatedColdStartup() = startup(empty = false)

    private fun startup(empty: Boolean) {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(
                StartupTimingMetric(),
                MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
                TraceSectionMetric("summary restoration"),
            ),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = {
                seed(empty = empty)
                pressHome()
            },
        ) {
            startActivityAndWait()
            device.waitForText("Library")
        }
    }

    @Test
    fun libraryScrollAndTopBar() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),
            compilationMode = CompilationMode.Partial(),
            iterations = 10,
            setupBlock = {
                seed()
                restartApp()
                device.waitForText("Library")
            },
        ) {
            repeat(5) { device.swipeUp() }
        }
    }

    @Test
    fun openLargeDocument() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(
                FrameTimingMetric(),
                MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
                TraceSectionMetric("full document loading"),
                TraceSectionMetric("RSVP preparation"),
            ),
            compilationMode = CompilationMode.Partial(),
            iterations = 10,
            setupBlock = {
                seed()
                restartApp()
                device.waitForText("Large performance document")
            },
        ) {
            clickDocument("Large performance document")
            check(device.wait(Until.hasObject(By.desc("Play")), 60_000))
        }
    }

    @Test
    fun rsvpPlaybackAtOneThousandWpm() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),
            compilationMode = CompilationMode.Partial(),
            iterations = 10,
            setupBlock = {
                seed(active = true)
                restartApp()
                device.wait(Until.hasObject(By.desc("Play")), 10_000)
            },
        ) {
            device.findObject(By.desc("Play")).click()
            Thread.sleep(4_000)
            device.findObject(By.desc("Pause")).click()
        }
    }

    @Test
    fun standardFormattingAndScroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(
                FrameTimingMetric(),
                MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
                TraceSectionMetric("standard formatting"),
            ),
            compilationMode = CompilationMode.Partial(),
            iterations = 10,
            setupBlock = {
                seed(active = true, reader = "speed")
                restartApp()
                device.waitForText("Standard")
            },
        ) {
            device.findObject(By.text("Standard")).click()
            device.waitForIdle()
            repeat(5) { device.swipeUp() }
        }
    }

    @Test
    fun hazePageTransitions() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            iterations = 10,
            setupBlock = {
                seed()
                restartApp()
                device.waitForText("Library")
            },
        ) {
            clickDescription("Open navigation")
            clickText("Settings")
            check(device.wait(Until.hasObject(By.text("Appearance")), 10_000))
            clickDescription("Back to library")
            check(device.wait(Until.hasObject(By.desc("Open navigation")), 10_000))
            clickDocument("Large performance document")
            check(device.wait(Until.hasObject(By.desc("Play")), 60_000))
            clickDescription("Back to library")
            check(device.wait(Until.hasObject(By.desc("Open navigation")), 10_000))
        }
    }

    private fun MacrobenchmarkScope.clickText(text: String) {
        click(By.text(text))
    }

    private fun MacrobenchmarkScope.restartApp() {
        device.executeShellCommand("am force-stop $PackageName")
        startActivityAndWait()
    }

    private fun MacrobenchmarkScope.clickDescription(description: String) {
        click(By.desc(description))
    }

    private fun MacrobenchmarkScope.clickDocument(title: String) {
        check(device.wait(Until.hasObject(By.text(title)), 10_000))
        repeat(20) {
            try {
                val bounds = device.findObject(By.text(title))?.visibleBounds ?: return@repeat
                device.click(bounds.centerX(), bounds.top - 100)
                return
            } catch (_: StaleObjectException) {
                Thread.sleep(100)
            }
        }
        error("Could not click document $title")
    }

    private fun MacrobenchmarkScope.click(selector: BySelector) {
        check(device.wait(Until.hasObject(selector), 10_000))
        repeat(20) {
            try {
                device.findObject(selector)?.click() ?: return@repeat
                return
            } catch (_: StaleObjectException) {
                Thread.sleep(100)
            }
        }
        error("Could not click $selector")
    }
}
