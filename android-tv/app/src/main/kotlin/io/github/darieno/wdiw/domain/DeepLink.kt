package io.github.darieno.wdiw.domain

import io.github.darieno.wdiw.data.TitleType
import java.net.URI
import java.net.URLDecoder

/** Where a `wdiw://` link points. Assistants, automations and the TV's own search use these. */
sealed interface Destination {
    data class Search(val query: String) : Destination
    data class Title(val type: TitleType, val id: Int) : Destination
}

/** Understands `wdiw://search?q=<text>` and `wdiw://title/<movie|tv>/<tmdb id>`. */
fun parseDeepLink(link: String): Destination? {
    val uri = runCatching { URI(link) }.getOrNull() ?: return null
    if (uri.scheme != "wdiw") return null
    return when (uri.host) {
        "search" -> uri.rawQuery.orEmpty().split('&')
            .firstOrNull { it.startsWith("q=") }
            ?.let { URLDecoder.decode(it.removePrefix("q="), "UTF-8").trim() }
            ?.takeIf { it.isNotEmpty() }
            ?.let(Destination::Search)

        "title" -> {
            val segments = uri.path.orEmpty().trim('/').split('/')
            val type = TitleType.fromPath(segments.getOrNull(0))
            val id = segments.getOrNull(1)?.toIntOrNull()
            if (type != null && id != null && id > 0 && segments.size == 2) Destination.Title(type, id) else null
        }

        else -> null
    }
}

fun titleDeepLink(type: TitleType, id: Int): String = "wdiw://title/${type.path}/$id"
