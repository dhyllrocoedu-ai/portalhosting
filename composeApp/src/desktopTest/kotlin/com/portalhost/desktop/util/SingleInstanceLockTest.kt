package com.portalhost.desktop.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SingleInstanceLockTest {

    @Test
    fun leftoverLockFromDeadProcessDoesNotBlockStartup() {
        val dir = Files.createTempDirectory("phlock").toFile()
        val lockFile = File(dir, "portalhost.instance.lock")
        // Leftover from a process that no longer exists (e.g. machine restart
        // while the app was minimized to the tray).
        lockFile.writeText("999999999\n0\n")

        val lock = SingleInstanceLock.acquireAt(lockFile)
        assertNotNull(lock)
        val acquired = lock!!

        acquired.release()
    }

    @Test
    fun lockOwnedByLiveProcessIsRejected() {
        val dir = Files.createTempDirectory("phlock2").toFile()
        val lockFile = File(dir, "portalhost.instance.lock")

        val holder = SingleInstanceLock.acquireAt(lockFile)
        assertNotNull(holder)
        val acquired = holder!!

        // Second launch while the first instance is alive (e.g. minimized to tray).
        val second = SingleInstanceLock.acquireAt(lockFile)
        assertNull(second)

        acquired.release()

        // After release the lock can be re-acquired.
        val again = SingleInstanceLock.acquireAt(lockFile)
        assertNotNull(again)
        again!!.release()
    }
}
