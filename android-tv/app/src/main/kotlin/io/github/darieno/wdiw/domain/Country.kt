package io.github.darieno.wdiw.domain

private val ISO_COUNTRY = Regex("[A-Z]{2}")

/**
 * The country whose catalogue to show. A choice made in Settings wins; otherwise where the network
 * says the TV is (a TV's language setting often does not match where it stands), then the last
 * place it was seen, then the device's region.
 */
fun resolveCountry(chosen: String?, detected: String?, lastDetected: String?, deviceRegion: String?): String =
    sequenceOf(chosen, detected, lastDetected, deviceRegion)
        .mapNotNull { it?.uppercase() }
        .firstOrNull { ISO_COUNTRY.matches(it) }
        ?: "US"

/** The device language as the "ll-CC" (or "ll") tag the proxy accepts. */
fun languageTag(language: String, region: String): String = when {
    !Regex("[a-z]{2}").matches(language) -> "en-US"
    ISO_COUNTRY.matches(region) -> "$language-$region"
    else -> language
}
