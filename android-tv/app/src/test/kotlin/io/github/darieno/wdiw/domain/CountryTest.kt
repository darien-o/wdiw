package io.github.darieno.wdiw.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryTest {
    @Test
    fun `the viewer's choice wins over everything`() {
        assertEquals("ES", resolveCountry(chosen = "es", detected = "CO", lastDetected = "MX", deviceRegion = "US"))
    }

    @Test
    fun `the network's country beats the device region`() {
        assertEquals("CO", resolveCountry(chosen = null, detected = "CO", lastDetected = "MX", deviceRegion = "US"))
    }

    @Test
    fun `offline, the last detected country is used`() {
        assertEquals("MX", resolveCountry(chosen = null, detected = null, lastDetected = "MX", deviceRegion = "US"))
    }

    @Test
    fun `invalid values are skipped and the last resort is US`() {
        assertEquals("AR", resolveCountry(chosen = "", detected = "T1", lastDetected = "419", deviceRegion = "AR"))
        assertEquals("US", resolveCountry(null, null, null, ""))
    }

    @Test
    fun `language tags are reduced to what the proxy accepts`() {
        assertEquals("es-CO", languageTag("es", "CO"))
        assertEquals("es", languageTag("es", "419"))
        assertEquals("en-US", languageTag("", "CO"))
    }
}
