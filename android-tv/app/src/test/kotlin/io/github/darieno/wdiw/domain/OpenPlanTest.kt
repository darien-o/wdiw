package io.github.darieno.wdiw.domain

import io.github.darieno.wdiw.data.AppJson
import io.github.darieno.wdiw.data.Link
import io.github.darieno.wdiw.data.Offer
import io.github.darieno.wdiw.data.OfferKind
import io.github.darieno.wdiw.data.Provider
import io.github.darieno.wdiw.data.Registry
import io.github.darieno.wdiw.data.TitleDetails
import io.github.darieno.wdiw.data.TitleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpenPlanTest {
    private val netflix = Provider(
        id = "netflix",
        name = "Netflix",
        tmdbIds = listOf(8, 1796),
        packages = listOf("com.netflix.ninja", "com.netflix.mediaclient"),
        titleLinks = listOf(
            Link("https://www.netflix.com/title/{id}", idSource = "netflix", extras = mapOf("source" to "30")),
        ),
    )
    private val prime = Provider(
        id = "prime-video",
        name = "Prime Video",
        tmdbIds = listOf(9),
        packages = listOf("com.amazon.amazonvideo.livingroom"),
        searchLink = Link("https://app.primevideo.com/search?phrase={query}"),
    )
    private val appleTv = Provider(
        id = "apple-tv",
        name = "Apple TV",
        tmdbIds = listOf(2),
        packages = listOf("com.apple.atve.androidtv.appletv"),
        titleLinks = listOf(
            Link("https://tv.apple.com/movie/{id}", idSource = "appletv", types = listOf(TitleType.MOVIE)),
            Link("https://tv.apple.com/show/{id}", idSource = "appletv", types = listOf(TitleType.TV)),
        ),
    )
    private val registry = Registry(schemaVersion = 1, revision = 1, providers = listOf(netflix, prime, appleTv))

    private fun offer(providerId: Int, vararg kinds: OfferKind) = Offer(providerId, "p$providerId", kinds = kinds.toList())

    private fun title(
        type: TitleType = TitleType.MOVIE,
        offers: List<Offer> = emptyList(),
        ids: Map<String, String> = emptyMap(),
    ) = TitleDetails(id = 1, type = type, title = "Amélie & Co", country = "CO", offers = offers, ids = ids)

    @Test
    fun `a known ID gives the exact title first, then the app launch`() {
        val plan = planOffer(
            offer(8, OfferKind.SUBSCRIPTION),
            title(ids = mapOf("netflix" to "70131314")),
            registry,
            installed = setOf("com.netflix.ninja"),
        )
        assertEquals(
            listOf(
                OpenStep.TitleLink("com.netflix.ninja", "https://www.netflix.com/title/70131314", mapOf("source" to "30")),
                OpenStep.Launch("com.netflix.ninja"),
            ),
            plan.steps,
        )
    }

    @Test
    fun `without an ID the title link is skipped`() {
        val plan = planOffer(offer(8), title(), registry, setOf("com.netflix.ninja"))
        assertEquals(listOf(OpenStep.Launch("com.netflix.ninja")), plan.steps)
    }

    @Test
    fun `a search link carries the URL-encoded title`() {
        val plan = planOffer(offer(9), title(), registry, setOf("com.amazon.amazonvideo.livingroom"))
        assertEquals(
            OpenStep.SearchLink(
                "com.amazon.amazonvideo.livingroom",
                "https://app.primevideo.com/search?phrase=Am%C3%A9lie+%26+Co",
                emptyMap(),
            ),
            plan.steps.first(),
        )
    }

    @Test
    fun `title links only apply to their title type`() {
        val ids = mapOf("appletv" to "umc.1")
        val installed = setOf("com.apple.atve.androidtv.appletv")
        val movie = planOffer(offer(2), title(TitleType.MOVIE, ids = ids), registry, installed)
        val show = planOffer(offer(2), title(TitleType.TV, ids = ids), registry, installed)
        assertEquals("https://tv.apple.com/movie/umc.1", (movie.steps.first() as OpenStep.TitleLink).uri)
        assertEquals("https://tv.apple.com/show/umc.1", (show.steps.first() as OpenStep.TitleLink).uri)
        assertEquals(2, movie.steps.size)
    }

    @Test
    fun `the first installed package is used`() {
        val plan = planOffer(offer(8), title(), registry, setOf("com.netflix.mediaclient"))
        assertEquals("com.netflix.mediaclient", plan.steps.single().packageName)
    }

    @Test
    fun `an app that is not installed has no steps but keeps its provider`() {
        val plan = planOffer(offer(8), title(), registry, installed = emptySet())
        assertFalse(plan.isInstalled)
        assertEquals(netflix, plan.provider)
    }

    @Test
    fun `a provider missing from the registry is reported as unknown`() {
        val plan = planOffer(offer(999), title(), registry, setOf("com.netflix.ninja"))
        assertFalse(plan.isInstalled)
        assertNull(plan.provider)
    }

    @Test
    fun `installed apps come first, and streaming before paying`() {
        val offers = listOf(
            offer(999, OfferKind.SUBSCRIPTION), // not installed, streaming
            offer(2, OfferKind.RENT, OfferKind.BUY), // installed, paid
            offer(9, OfferKind.ADS), // installed, streaming
            offer(8, OfferKind.SUBSCRIPTION), // not installed, streaming
        )
        val plans = planOffers(
            title(offers = offers),
            registry,
            installed = setOf("com.apple.atve.androidtv.appletv", "com.amazon.amazonvideo.livingroom"),
        )
        assertEquals(listOf(9, 2, 999, 8), plans.map { it.offer.providerId })
    }

    @Test
    fun `the bundled registry parses and every title link names an ID source`() {
        val parsed = AppJson.decodeFromString<Registry>(File("../../registry/providers.json").readText())
        assertEquals(Registry.SUPPORTED_SCHEMA, parsed.schemaVersion)
        assertTrue(parsed.providers.isNotEmpty())
        assertEquals("netflix", parsed.providerFor(8)?.id)
        assertTrue(parsed.providers.flatMap { it.titleLinks }.all { it.idSource != null })
    }

    @Test
    fun `only plain https links to a host are allowed`() {
        assertTrue(isAllowedLink("https://www.netflix.com/title/1"))
        assertTrue(isAllowedLink("HTTPS://app.primevideo.com/search?phrase=a"))
        assertFalse(isAllowedLink("http://www.netflix.com/title/1"))
        assertFalse(isAllowedLink("file:///sdcard/x.html"))
        assertFalse(isAllowedLink("content://settings/system"))
        assertFalse(isAllowedLink("javascript:alert(1)"))
        assertFalse(isAllowedLink("intent://x#Intent;component=a.b/.C;end"))
        assertFalse(isAllowedLink("https:///path"))
        assertFalse(isAllowedLink("https://user@evil.example/"))
    }

    @Test
    fun `links that are not https are skipped and the app is still launched`() {
        val unsafe = prime.copy(searchLink = Link("javascript:alert('{query}')"))
        val plan = planOffer(
            offer(9, OfferKind.SUBSCRIPTION),
            title(),
            Registry(schemaVersion = 1, revision = 1, providers = listOf(unsafe)),
            installed = setOf("com.amazon.amazonvideo.livingroom"),
        )
        assertEquals(listOf(OpenStep.Launch("com.amazon.amazonvideo.livingroom")), plan.steps)
    }

    @Test
    fun `a registry from a newer app version with unknown fields still parses`() {
        val json = """
            {"schemaVersion":1,"revision":7,"somethingNew":true,"providers":[
              {"id":"x","name":"X","tmdbIds":[1],"packages":["a.b"],"futureField":{"k":1},
               "searchLink":{"uri":"x://s?q={query}","status":"verified","alsoNew":1}}]}
        """.trimIndent()
        val parsed = AppJson.decodeFromString<Registry>(json)
        assertEquals(7, parsed.revision)
        assertEquals("x://s?q={query}", parsed.providers.single().searchLink?.uri)
    }
}
