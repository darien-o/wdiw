package io.github.darieno.wdiw.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test
    fun `later releases are newer`() {
        assertTrue(isNewerVersion("v0.2.0", "0.1.0"))
        assertTrue(isNewerVersion("0.10.0", "0.9.9"))
        assertTrue(isNewerVersion("1.0", "0.9.9"))
        assertTrue(isNewerVersion("1.0.1", "1.0"))
    }

    @Test
    fun `the same or an earlier release is not`() {
        assertFalse(isNewerVersion("v0.1.0", "0.1.0"))
        assertFalse(isNewerVersion("1.0", "1.0.0"))
        assertFalse(isNewerVersion("0.9.0", "0.10.0"))
    }

    @Test
    fun `unreadable versions are never newer`() {
        assertFalse(isNewerVersion("nightly", "0.1.0"))
        assertFalse(isNewerVersion("", "0.1.0"))
        assertFalse(isNewerVersion("1.x", "0.1.0"))
    }
}
