package com.juckul.readspeeder.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = PackageName,
            includeInStartupProfile = true,
        ) {
            seed()
            pressHome()
            startActivityAndWait()
            device.waitForText("Library")
            repeat(3) { device.swipeUp() }
            repeat(3) { device.swipeDown() }
            device.findObject(By.desc("Open navigation")).click()
            device.findObject(By.text("Settings")).click()
            device.findObject(By.desc("Back to library")).click()
            device.findObject(By.text("Large performance document")).click()
            device.wait(Until.hasObject(By.desc("Play")), 10_000)
            device.findObject(By.desc("Play")).click()
            Thread.sleep(1_000)
            device.findObject(By.desc("Pause")).click()
            device.findObject(By.text("Standard")).click()
            device.waitForIdle()
            device.swipeUp()
        }
    }
}
