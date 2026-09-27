package com.abi.expensetracker.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLockTest {

    @Test
    fun `a quick trip away does not lock, a longer one does`() {
        AppLock.markUnlocked()
        AppLock.onBackground()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(30))
        AppLock.onForeground()
        assertFalse(AppLock.locked)

        AppLock.onBackground()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(90))
        AppLock.onForeground()
        assertTrue(AppLock.locked)
    }
}
