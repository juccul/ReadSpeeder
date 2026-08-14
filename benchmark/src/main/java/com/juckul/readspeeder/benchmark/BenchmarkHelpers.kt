package com.juckul.readspeeder.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal const val PackageName = "com.juckul.readspeeder"

internal fun MacrobenchmarkScope.seed(
    empty: Boolean = false,
    active: Boolean = false,
    reader: String = "speed",
) {
    device.executeShellCommand(
        "am broadcast -W -n $PackageName/.BenchmarkSeederReceiver " +
            "-a $PackageName.BENCHMARK_SEED --ez empty $empty --ez active $active " +
            "--es reader $reader",
    )
}

internal fun UiDevice.waitForText(text: String) {
    wait(Until.hasObject(By.text(text)), 10_000)
}

internal fun UiDevice.swipeUp() {
    val x = displayWidth / 2
    swipe(x, displayHeight * 4 / 5, x, displayHeight / 4, 12)
    waitForIdle()
}

internal fun UiDevice.swipeDown() {
    val x = displayWidth / 2
    swipe(x, displayHeight / 4, x, displayHeight * 4 / 5, 12)
    waitForIdle()
}
