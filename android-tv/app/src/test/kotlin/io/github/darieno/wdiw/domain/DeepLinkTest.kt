package io.github.darieno.wdiw.domain

import io.github.darieno.wdiw.data.TitleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {
    @Test
    fun `search links carry the decoded query`() {
        assertEquals(Destination.Search("the matrix"), parseDeepLink("wdiw://search?q=the%20matrix"))
        assertEquals(Destination.Search("amélie"), parseDeepLink("wdiw://search?lang=es&q=am%C3%A9lie"))
    }

    @Test
    fun `title links carry the type and TMDB ID`() {
        assertEquals(Destination.Title(TitleType.MOVIE, 27205), parseDeepLink("wdiw://title/movie/27205"))
        assertEquals(Destination.Title(TitleType.TV, 1396), parseDeepLink(titleDeepLink(TitleType.TV, 1396)))
    }

    @Test
    fun `anything else is rejected`() {
        assertNull(parseDeepLink("wdiw://search"))
        assertNull(parseDeepLink("wdiw://search?q="))
        assertNull(parseDeepLink("wdiw://title/person/5"))
        assertNull(parseDeepLink("wdiw://title/movie/abc"))
        assertNull(parseDeepLink("wdiw://title/movie/5/extra"))
        assertNull(parseDeepLink("https://search?q=x"))
        assertNull(parseDeepLink("not a uri"))
    }
}
